package com.example.taskmanager.controller;

import com.example.taskmanager.dto.PageResponse;
import com.example.taskmanager.dto.TaskRequestDto;
import com.example.taskmanager.dto.TaskResponseDto;
import com.example.taskmanager.dto.TaskStatusUpdateRequest;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@Tag(name = "Задачи", description = "Управление задачами")
public class TaskController {
    private final TaskService taskService;

    @Operation(
            summary = "Получить задачи по статусу",
            description = "Позволяет получить страницу с отфильтрованными задачами по их статусу выполнения"
    )
    @GetMapping("/filter")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<TaskResponseDto>> getTasksByStatus(
            @RequestParam TaskStatus status,
            @PageableDefault(size = 10) Pageable pageable,
            Principal principal) {

        Sort forcedSort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable forcedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), forcedSort);

        return ResponseEntity.ok(PageResponse.of(taskService.findByStatus(status, principal.getName(), forcedPageable)));
    }

    @Operation(
            summary = "Получить все задачи",
            description = "Позволяет получить: пользователю страницу со всеми своими задачами " +
                    "/ админу страницу со всеми задачами"
    )
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<TaskResponseDto>> getAllTasks(
            @PageableDefault(size = 10) Pageable pageable) {

        Sort forcedSort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable forcedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), forcedSort);

        return ResponseEntity.ok(PageResponse.of(taskService.findTasksForCurrentUser(forcedPageable)));
    }

    @Operation(
            summary = "Получить задачу по id",
            description = "Позволяет получить: пользователю свою задачу по id / админу любую задачу по id"
    )
    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TaskResponseDto> getTaskById(@PathVariable @Min(1) Long id, Principal principal) {
        return ResponseEntity.ok(taskService.findByIdAndUsername(id, principal.getName()));
    }

    @Operation(
            summary = "Создать задачу",
            description = "Позволяет админу создать задачу и назначить ее пользователю"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TaskResponseDto> createTask(@Valid @RequestBody TaskRequestDto taskRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(taskRequestDto));
    }

    @Operation(
            summary = "Обновить задачу",
            description = "Позволяет админу обновить задачу пользователя по id"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TaskResponseDto> updateTaskByAdmin(@PathVariable @Min(1) Long id,
                                                      @Valid @RequestBody TaskRequestDto taskRequestDto,
                                                      Principal principal) {
        return ResponseEntity.ok(taskService.updateTaskByAdmin(id, taskRequestDto, principal.getName()));
    }

    @Operation(
            summary = "Обновить статус задачи",
            description = "Позволяет пользователю обновить статус своей задачи по id"
    )
    @PatchMapping("{id}/status")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<TaskResponseDto> updateTaskStatus(@PathVariable @Min(1) Long id,
                                                            @Valid @RequestBody TaskStatusUpdateRequest request,
                                                            Principal principal) {
        return ResponseEntity.ok(taskService.updateOwnTaskStatus(id, request, principal.getName()));
    }

    @Operation(
            summary = "Удалить задачу",
            description = "Позволяет админу удалить задачу по id"
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteTask(@PathVariable @Min(1) Long id, Principal principal) {
        taskService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
