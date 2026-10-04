package com.internal.tasktracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskRepository taskRepository;

    public TaskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/api/tasks")
    public ResponseEntity<?> searchTasks(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize) {

        // Validate pagination input
        if (page < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Page must be greater than or equal to 1"));
        }
        if (pageSize < 1 || pageSize > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "Page size must be between 1 and 100"));
        }

        // Normalize query input
        String query = q == null ? "" : q.trim();
        String searchTerm = "%" + query.toLowerCase() + "%";

        // Parse and validate status filter
        String normalizedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                normalizedStatus = TaskStatus.valueOf(status.trim().toUpperCase()).name();
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Invalid status: '" + status + "'. Allowed values: OPEN, IN_PROGRESS, DONE"));
            }
        }

        log.info("searchTasks: q='{}', status={}, page={}, pageSize={}", query, normalizedStatus, page, pageSize);

        // Database-level pagination
        Page<Task> taskPage = taskRepository.searchTasks(
                searchTerm,
                normalizedStatus,
                PageRequest.of(page - 1, pageSize));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", taskPage.getContent());
        response.put("total", taskPage.getTotalElements());
        response.put("page", page);
        response.put("pageSize", pageSize);

        return ResponseEntity.ok(response);
    }
}
