package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequestDto;
import com.example.taskmanager.dto.TaskResponseDto;
import com.example.taskmanager.dto.TaskStatusUpdateRequest;
import com.example.taskmanager.entity.RoleType;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.DataBaseOperationException;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.exception.UserNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public Page<TaskResponseDto> findByStatus(TaskStatus status, String username, Pageable pageable) {
        logger.debug("Поиск задачи по статусу: {}", status);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Page<Task> tasks;

        if (user.getRole().equals(RoleType.ROLE_ADMIN)) {
            tasks = taskRepository.findByStatus(status, pageable);
        } else {
            tasks = taskRepository.findByStatusAndUser(status, user, pageable);
        }

        return tasks.map(taskMapper::mapToDto);
    }

    public Page<TaskResponseDto> findTasksForCurrentUser(Pageable pageable) {
        logger.debug("Поиск задачи для текущего пользователя");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Page<Task> tasks;

        if (user.getRole() == RoleType.ROLE_ADMIN) {
            tasks = taskRepository.findAll(pageable);
        } else {
            tasks = taskRepository.findAllByUserUsername(username, pageable);
        }

        return tasks.map(taskMapper::mapToDto);
    }

    public TaskResponseDto findByIdAndUsername(Long id, String username) {
        logger.debug("Поиск задачи по id={} и username пользователя={}", id, username);

        return Optional.of(findTaskByUsername(id, username))
                .map(taskMapper::mapToDto)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));
    }

    @Transactional
    public TaskResponseDto create(TaskRequestDto taskRequestDto) {
        logger.info("создание новой задачи: {} для пользователя с id={}", taskRequestDto.getTitle(), taskRequestDto.getUserId());

        User user = userRepository.findById(taskRequestDto.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var task = taskMapper.mapToEntity(taskRequestDto);
        task.setUser(user);

        try {
            taskRepository.save(task);
            return taskMapper.mapToDto(task);
        } catch (DataAccessException e) {
            throw new DataBaseOperationException("Failed to create task", e);
        }
    }

    @Transactional
    public TaskResponseDto updateOwnTaskStatus(Long taskId, TaskStatusUpdateRequest request, String username) {
        logger.info("Обновление статуса задачи с id={} пользователем={}", taskId, username);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + taskId + " not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getRole().equals(RoleType.ROLE_ADMIN) && !task.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to update this task");
        }

        task.setStatus(request.status());
        Task updatedTask = taskRepository.save(task);
        return taskMapper.mapToDto(updatedTask);
    }

    @Transactional
    public TaskResponseDto updateTaskByAdmin(Long id, TaskRequestDto taskRequestDto, String username) {
        logger.info("Обновление задачи с id={} админом={}", id, username);

        var task = findTaskByUsername(id, username);

        if (taskRequestDto.getTitle() != null) {
            task.setTitle(taskRequestDto.getTitle());
        }
        if (taskRequestDto.getDescription() != null) {
            task.setDescription(taskRequestDto.getDescription());
        }
        if (taskRequestDto.getStatus() != null) {
            task.setStatus(taskRequestDto.getStatus());
        }

        var savedTask = taskRepository.save(task);

        return taskMapper.mapToDto(savedTask);
    }

    @Transactional
    public void delete(Long id, String username) {
        logger.info("Удаление задачи с id={} админом={}", id, username);

        Task task = findTaskByUsername(id, username);

        taskRepository.delete(task);
    }

    private Task findTaskByUsername(Long id, String username) {
        logger.debug("Поиск задачи с id={} по имени текущего пользователя={}", id, username);

        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task with id " + id + " not found"));

        if (!task.getUser().getUsername().equals(username) && !isAdmin(username)) {
            throw new AccessDeniedException("You are not allowed to this task");
        }
        return task;
    }

    private boolean isAdmin(String username) {
        logger.debug("Проверка на наличие роли admin у пользователя={}", username);

        return userRepository.findByUsername(username)
                .map(user -> user.getRole() == RoleType.ROLE_ADMIN)
                .orElse(false);
    }
}
