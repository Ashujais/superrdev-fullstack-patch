# Handwritten Explanations Checklist

### Page 1: SQL Operator Precedence Bug (`01-sql-bug.jpg`)
1. **File / Location**:
   - `backend/src/main/java/com/internal/tasktracker/TaskRepository.java` (lines 14–17)
   - `db/queries/search_tasks.sql` (lines 10–13)
   - `db/oracle/task_search_package.sql` (lines 52–55, 66–69)
2. **How I Discovered It**:
   - Inspected repository query and compared against sample dataset (`data.sql`). Querying `q=api` returned archived tasks (IDs 20 & 21), and `status=OPEN` returned tasks with status `DONE` and `IN_PROGRESS`.
3. **Root Cause**:
   - In SQL, `AND` takes precedence over `OR`. The clause `WHERE archived = FALSE AND LOWER(title) LIKE :term OR LOWER(description) LIKE :term AND (:status IS NULL OR status = :status)` evaluated as `(archived = FALSE AND title LIKE :term) OR (desc LIKE :term AND status = :status)`. If description matched, `archived = FALSE` was bypassed. If title matched, status filter was bypassed.
4. **Fix**:
   - Added explicit parentheses around the search terms:
     `WHERE archived = FALSE AND (LOWER(title) LIKE :term OR LOWER(description) LIKE :term) AND (:status IS NULL OR status = :status)`.
5. **Why I Chose This Fix**:
   - Directly enforces the business requirement: task must be unarchived, must match title OR description, and must match status if specified. Mirrored fix in reference SQL files for consistency.

---

### Page 2: In-Memory Pagination & Performance (`02-database-pagination.jpg`)
1. **File / Location**:
   - `backend/src/main/java/com/internal/tasktracker/TaskRepository.java` & `TaskController.java`
2. **How I Discovered It**:
   - Code inspection showed `taskRepository.searchTasks()` returning `List<Task>` of all matching records, followed by Java-level `allResults.subList(start, end)`.
3. **Root Cause**:
   - Slicing results in memory forces the database to load the entire matching dataset and instantiate JPA entities on the heap, creating $O(N)$ memory and network overhead regardless of requested page size.
4. **Fix**:
   - Switched repository query to return `Page<Task>` with `Pageable pageable` and added an explicit `countQuery`. In `TaskController`, passed `PageRequest.of(page - 1, pageSize)` to execute `OFFSET`/`LIMIT` directly in H2/database.
5. **Why I Chose This Fix**:
   - Offloads pagination and counting to the database engine where it belongs, scales efficiently to large datasets, and preserves the exact existing JSON API response format.

---

### Page 3: Artificial Request Delay & Input Validation (`03-backend-delays-and-validation.jpg`)
1. **File / Location**:
   - `backend/src/main/java/com/internal/tasktracker/TaskController.java`
2. **How I Discovered It**:
   - Profiling API requests revealed blank queries took ~1000ms. Code review revealed `complexityScore = Math.max(0, 10 - query.length())` and `Thread.sleep(queryWeight)`. Additionally, testing `status=INVALID` or `page=0` triggered 500 server errors.
3. **Root Cause**:
   - Artificial `Thread.sleep` intentionally blocked servlet container worker threads. Lack of input validation allowed invalid bounds (`page < 1`, `pageSize <= 0`) and `TaskStatus.valueOf()` to throw unhandled runtime exceptions.
4. **Fix**:
   - Removed `complexityScore`, `queryWeight`, and `Thread.sleep` completely.
   - Added validation returning 400 Bad Request for `page < 1` and `pageSize < 1 || pageSize > 100`.
   - Wrapped `TaskStatus.valueOf()` in try/catch to return 400 Bad Request with allowed enum values. Replaced `System.out.println` with SLF4J logger.
5. **Why I Chose This Fix**:
   - Eliminates thread starvation and artificial latency. Input errors must produce clean 4xx client errors instead of uncontrolled 500 server failures.

---

### Page 4: Frontend Async State, Race Conditions & Error Handling (`04-frontend-async.jpg`)
1. **File / Location**:
   - `frontend/src/hooks/useTasks.js` & `frontend/src/api.js`
2. **How I Discovered It**:
   - Code inspection showed `fetchTasks().catch(...)` set error but never reset `loading = false`, leaving the UI permanently in "Loading..." on network/API failure. Also, rapid sequential typing caused out-of-order responses overwriting newer state.
3. **Root Cause**:
   - Missing `setLoading(false)` and `setError(null)` resets in Promise lifecycle; lack of request cancellation.
4. **Fix**:
   - Integrated `AbortController` in `useEffect` cleanup in `useTasks.js` and wired `signal` into `fetch()` in `api.js`. Handled `AbortError` quietly.
   - Ensured `setLoading(false)` always executes on failure, and cleared errors on new request triggers.
5. **Why I Chose This Fix**:
   - Native `AbortController` is standard, cancelable, and requires zero external libraries. Prevents stale request races and keeps UI state strictly consistent.

---

### Page 5: Search Debouncing & Pagination Reset (`05-ux-debounce-pagination.jpg`)
1. **File / Location**:
   - `frontend/src/App.jsx`
2. **How I Discovered It**:
   - Navigating to page 4 and subsequently filtering by search or status left `page` at 4, resulting in an empty table with missing pagination controls. Furthermore, typing sent an API call per keystroke.
3. **Root Cause**:
   - `page` state was independent of `query` and `status` filter changes. Keystrokes updated search query without throttling.
4. **Fix**:
   - Added event handlers `handleQueryChange` and `handleStatusChange` that reset `page` to 1 immediately when filters change.
   - Added a 300ms debounce using `setTimeout`/`clearTimeout` on the search query before triggering API requests.
5. **Why I Chose This Fix**:
   - Immediate page reset prevents the confusing empty-page dead-end. A lightweight 300ms debounce prevents network flooding while keeping input typing responsive with zero dependencies.
