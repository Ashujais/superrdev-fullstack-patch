package com.internal.tasktracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskTrackerApplicationTests {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void repository_excludesArchivedTasksEvenWhenMatchingTerm() {
        // Query for "api" which exists in archived task titles and descriptions (e.g. ID 20, 21)
        Page<Task> result = taskRepository.searchTasks("%api%", null, PageRequest.of(0, 50));
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(task -> !task.isArchived());
    }

    @Test
    void repository_respectsStatusFilterWhenTitleMatches() {
        // ID 2 ("Update API rate limiting") is IN_PROGRESS and matches "api"
        // Searching for "api" with status "OPEN" should NOT return ID 2
        Page<Task> result = taskRepository.searchTasks("%api%", "OPEN", PageRequest.of(0, 50));
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(task -> "OPEN".equals(task.getStatus()) && !task.isArchived());
    }

    @Test
    void controller_returnsBadRequestForInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void controller_returnsBadRequestForInvalidPage() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Page must be greater than or equal to 1"));
    }

    @Test
    void controller_returnsBadRequestForInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/tasks").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Page size must be between 1 and 100"));

        mockMvc.perform(get("/api/tasks").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Page size must be between 1 and 100"));
    }

    @Test
    void controller_databasePaginationReturnsExpectedShapeAndItems() throws Exception {
        mockMvc.perform(get("/api/tasks").param("page", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.total").value(47))
                .andExpect(jsonPath("$.items.length()").value(5));
    }
}
