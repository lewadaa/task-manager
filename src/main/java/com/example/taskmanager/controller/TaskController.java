package com.example.taskmanager.controller;

import com.example.taskmanager.dto.PageResponse;
import com.example.taskmanager.dto.TaskRequestDto;
import com.example.taskmanager.dto.TaskResponseDto;
import com.example.taskmanager.dto.TaskStatusUpdateRequest;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {
    private final TaskService taskService;

    @GetMapping("/filter")
    public ResponseEntity<PageResponse<TaskResponseDto>> getTasksByStatus(
            @RequestParam TaskStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(PageResponse.of(taskService.findByStatus(status, principal.getName(), pageable)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TaskResponseDto>> getAllTasks(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(taskService.findTasksForCurrentUser(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable @Min(1) Long id, Principal principal) {
        return ResponseEntity.ok(taskService.findByIdAndUsername(id, principal.getName()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskRequestDto taskRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(taskRequestDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponseDto> updateTaskByAdmin(@PathVariable @Min(1) Long id,
                                                      @Valid @RequestBody TaskRequestDto taskRequestDto,
                                                      Principal principal) {
        return ResponseEntity.ok(taskService.updateTaskByAdmin(id, taskRequestDto, principal.getName()));
    }

    @PatchMapping("{id}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TaskResponseDto> updateTaskStatus(@PathVariable @Min(1) Long id,
                                                            @Valid @RequestBody TaskStatusUpdateRequest request,
                                                            Principal principal) {
        return ResponseEntity.ok(taskService.updateOwnTaskStatus(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable @Min(1) Long id, Principal principal) {
        taskService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
