# Engineering Notes

### 1. Summary of Changes
- **SQL Operator Precedence**: Wrapped search conditions in parentheses in `TaskRepository.java`, `search_tasks.sql`, and `task_search_package.sql`, ensuring unarchived filtering and status matching are strictly enforced.
- **Database Pagination**: Replaced in-memory `subList` with Spring Data `Pageable` (`Page<Task>`) and count query, pushing offset/limit to the database while preserving the API contract.
- **Removed Fake Delay & Added Validation**: Removed `Thread.sleep` and fake complexity delay in `TaskController`. Added 400 validation for invalid `page`, `pageSize`, and `status`. Switched to SLF4J logging.
- **Frontend Async State & AbortController**: Updated `useTasks` to abort stale requests via `AbortController`, reset errors on success, and ensure `loading = false` on failure.
- **Search UX & Pagination Reset**: Added 300ms search debouncing and reset page to 1 when search or status filters change.

### 2. What I Chose Not to Change and Why
- **Architecture**: Kept the simple controller/repository layout without adding DTO layers or service interfaces under the 90-minute timebox.
- **Libraries**: Avoided adding Redux, Axios, or lodash; standard React hooks, `AbortController`, and `setTimeout` solved every requirement cleanly.
- **Schema & Seed Data**: Kept H2 schema and seed data unchanged to maintain compatibility.

### 3. Biggest Remaining Risk
- **Unindexed Wildcard Search**: The query uses `LIKE %term%` on `title` and `description`. As data grows, leading-wildcard full table scans will cause database latency. A dedicated full-text search index will be required.

### 4. Tools/AI Used and How
- Used an AI assistant to survey the codebase, inspect the Oracle artifact, and suggest failure modes.
- Verified the SQL precedence bug and artificial delays locally via API requests before patching.
- Used AI to draft debounce logic and automated tests, then verified all edge cases against the running application.
