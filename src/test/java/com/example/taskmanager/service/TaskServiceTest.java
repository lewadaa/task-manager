package com.example.taskmanager.service;

import com.example.taskmanager.dto.TaskRequestDto;
import com.example.taskmanager.dto.TaskResponseDto;
import com.example.taskmanager.dto.TaskStatusUpdateRequest;
import com.example.taskmanager.dto.UserResponseDto;
import com.example.taskmanager.entity.RoleType;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.exception.UserNotFoundException;
import com.example.taskmanager.mapper.TaskMapper;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    private Long taskId;
    private TaskRequestDto taskRequestDto;
    private User user;
    private User admin;
    private UserResponseDto userResponseDto;
    private TaskResponseDto taskResponseDto;
    private TaskResponseDto updatedTaskResponseDto;
    private Task task;
    private Task newTask;
    private final LocalDateTime now = LocalDateTime.now();
    private final String USERNAME = "user";
    private final String ADMIN = "admin";
    private Page<Task> tasks;
    Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        taskId = 1L;
        taskRequestDto = new TaskRequestDto("New Title", "New Description", TaskStatus.PENDING, 1L);
        user = new User(1L, "user", "password", RoleType.ROLE_USER);
        admin = new User(2L, "admin", "password2", RoleType.ROLE_ADMIN);
        userResponseDto = new UserResponseDto("John", RoleType.ROLE_USER.name());
        taskResponseDto = new TaskResponseDto("Test Task", "Description", TaskStatus.IN_PROGRESS, userResponseDto);
        updatedTaskResponseDto = new TaskResponseDto("New Test Task", "New Description", TaskStatus.COMPLETED, userResponseDto);
        task = new Task(taskId, "Test Task",
                "Description", TaskStatus.IN_PROGRESS, now, now, user);
        newTask = new Task(taskId, "New Title",
                "New Description", TaskStatus.PENDING, now, now, user);
        tasks = new PageImpl<>(List.of(task));
    }

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;


    @InjectMocks
    private TaskService taskService;

    @Test
    void findByStatus_ShouldReturnPageTasks_WhenRoleAdmin() {
        //arrange
        Mockito.when(userRepository.findByUsername(ADMIN)).thenReturn(Optional.of(admin));
        Mockito.when(taskRepository.findByStatus(TaskStatus.PENDING, pageable)).thenReturn(tasks);
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(taskResponseDto);

        //act
        Page<TaskResponseDto> result = taskService.findByStatus(TaskStatus.PENDING, ADMIN, pageable);

        //assert
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByStatus_ShouldReturnPageTasks_WhenRoleUser() {
        //arrange
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        Mockito.when(taskRepository.findByStatusAndUser(TaskStatus.PENDING, user, pageable)).thenReturn(tasks);
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(taskResponseDto);

        //act
        Page<TaskResponseDto> result = taskService.findByStatus(TaskStatus.PENDING, USERNAME, pageable);

        //assert
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findTasksForCurrentUser_ShouldReturnPageTasks_WhenRoleAdmin() {
        //arrange
        mockSecurity(ADMIN);

        Mockito.when(userRepository.findByUsername(ADMIN)).thenReturn(Optional.of(admin));
        Mockito.when(taskRepository.findAll(pageable)).thenReturn(tasks);
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(taskResponseDto);

        //act
        Page<TaskResponseDto> result = taskService.findTasksForCurrentUser(pageable);

        //assert
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findTasksForCurrentUser_ShouldReturnPageTasks_WhenRoleUser() {
        //arrange
        mockSecurity(USERNAME);

        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        Mockito.when(taskRepository.findAllByUserUsername(USERNAME, pageable)).thenReturn(tasks);
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(taskResponseDto);

        //act
        Page<TaskResponseDto> result = taskService.findTasksForCurrentUser(pageable);

        //assert
        assertEquals(1, result.getContent().size());
    }

    @Test
    void findByIdAndUsername_ShouldReturnTask_WhenOwner() {
        //arrange
        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(taskResponseDto);

        //act
        TaskResponseDto result = taskService.findByIdAndUsername(taskId, USERNAME);

        //assert
        assertNotNull(result);
        assertEquals(taskResponseDto, result);
        Mockito.verify(taskRepository, Mockito.times(1)).findById(taskId);
        Mockito.verify(taskMapper, Mockito.times(1)).mapToDto(task);
    }

    @Test
    void findByIdAndUsername_ShouldReturnTask_WhenAdmin() {
        //arrange
        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername(ADMIN)).thenReturn(Optional.of(admin));
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(taskResponseDto);
        //act
        TaskResponseDto result = taskService.findByIdAndUsername(taskId, ADMIN);

        //assert
        assertNotNull(result);
        assertEquals(taskResponseDto, result);
        Mockito.verify(taskRepository, Mockito.times(1)).findById(taskId);
        Mockito.verify(userRepository, Mockito.times(1)).findByUsername(ADMIN);
        Mockito.verify(taskMapper, Mockito.times(1)).mapToDto(task);
    }

    @Test
    void findById_ShouldThrowException_WhenNotAdminAndNotOwner() {
        //arrange
        User anotherUser = new User();
        anotherUser.setId(99L);
        anotherUser.setUsername("Hacker");
        anotherUser.setRole(RoleType.ROLE_USER);

        task.setUser(admin);

        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername("Hacker")).thenReturn(Optional.of(anotherUser));

        //act & assert
        assertThrows(AccessDeniedException.class, () -> taskService.findByIdAndUsername(taskId, "Hacker"));
    }

    @Test
    void findById_ShouldThrowException_WhenTaskDoesNotExist() {
        //arrange
        Mockito.when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(TaskNotFoundException.class, () -> taskService.findByIdAndUsername(99L, USERNAME));
    }

    @Test
    void create_ShouldCreateTask_WhenRequestIsValid() {
        //arrange
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Mockito.when(taskMapper.mapToEntity(taskRequestDto)).thenReturn(newTask);
        Mockito.when(taskRepository.save(Mockito.any(Task.class))).thenReturn(newTask);
        Mockito.when(taskMapper.mapToDto(newTask)).thenReturn(taskResponseDto);

        //act
        TaskResponseDto result = taskService.create(taskRequestDto);

        //assert
        assertNotNull(result);
        assertEquals(taskResponseDto.getTitle(), result.getTitle());
        assertEquals(taskResponseDto.getDescription(), result.getDescription());
        assertEquals(taskResponseDto.getStatus(), result.getStatus());


        Mockito.verify(userRepository, Mockito.times(1)).findById(1L);
        Mockito.verify(taskRepository, Mockito.times(1)).save(newTask);
        Mockito.verify(taskMapper, Mockito.times(1)).mapToEntity(taskRequestDto);
        Mockito.verify(taskMapper, Mockito.times(1)).mapToDto(newTask);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        Mockito.verify(taskRepository, Mockito.times(1)).save(captor.capture());

        Task capturedTask = captor.getValue();

        assertEquals("New Title", capturedTask.getTitle());
        assertEquals("New Description", capturedTask.getDescription());
        assertEquals(TaskStatus.PENDING, capturedTask.getStatus());
    }

    @Test
    void create_ShouldThrowException_WhenUserNotFound() {
        //arrange
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UserNotFoundException.class, () -> taskService.create(taskRequestDto));
    }

    @Test
    void create_ShouldThrowException_WhenTaskCreationFails() {
        //arrange
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Mockito.when(taskMapper.mapToEntity(taskRequestDto)).thenReturn(task);

        Mockito.when(taskRepository.save(task)).thenThrow(new RuntimeException("Database error"));

        //act & assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> taskService.create(taskRequestDto));
        assertEquals("Database error", exception.getMessage());

        Mockito.verify(taskRepository, Mockito.times(1)).save(task);
        Mockito.verify(taskMapper, Mockito.times(1)).mapToEntity(taskRequestDto);
    }

    @Test
    void updateOwnTaskStatus_ShouldUpdateTask_WhenRequestIsValid() {
        //arrange
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);

        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        Mockito.when(taskRepository.save(task)).thenReturn(task);
        Mockito.when(taskMapper.mapToDto(task)).thenReturn(updatedTaskResponseDto);

        //act
        task.setStatus(TaskStatus.COMPLETED);
        TaskResponseDto result = taskService.updateOwnTaskStatus(taskId, request, USERNAME);

        //assert
        assertNotNull(result);
        assertEquals(updatedTaskResponseDto, result);
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());

        Mockito.verify(taskRepository, Mockito.times(1)).findById(taskId);
        Mockito.verify(taskRepository, Mockito.times(1)).save(task);
        Mockito.verify(taskMapper, Mockito.times(1)).mapToDto(task);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        Mockito.verify(taskRepository, Mockito.times(1)).save(captor.capture());

        Task capturedTask = captor.getValue();

        assertEquals("Test Task", capturedTask.getTitle());
        assertEquals("Description", capturedTask.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, capturedTask.getStatus());
    }

    @Test
    void updateOwnTaskStatus_ShouldThrowException_WhenNotOwnerAndNotAdmin() {
        //arrange
        Task anotherUserTask = new Task();
        anotherUserTask.setId(2L);
        anotherUserTask.setUser(admin);

        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.CANCELLED);

        Mockito.when(taskRepository.findById(2L)).thenReturn(Optional.of(anotherUserTask));
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        //act & assert
        assertThrows(AccessDeniedException.class, () -> {
            taskService.updateOwnTaskStatus(2L, request, USERNAME);
        });
    }

    @Test
    void updateTaskByAdmin_ShouldUpdateTask_WhenUserIsAdmin() {
        //arrange
        TaskRequestDto dto = new TaskRequestDto("New Title", "New Description", TaskStatus.COMPLETED, 1L);

        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername(ADMIN)).thenReturn(Optional.of(admin));
        Mockito.when(taskRepository.save(Mockito.any(Task.class))).thenReturn(newTask);
        Mockito.when(taskMapper.mapToDto(newTask)).thenReturn(taskResponseDto);

        //act
        TaskResponseDto result = taskService.updateTaskByAdmin(taskId, dto, ADMIN);

        //assert
        assertEquals(taskResponseDto, result);
    }

    @Test
    void updateTaskByAdmin_ShouldThrowException_WhenUserIsNotAdmin() {
        //arrange
        TaskRequestDto dto = new TaskRequestDto("New Title", "New Description", TaskStatus.CANCELLED, 1L);

        task.setUser(admin);

        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        //act & assert
        assertThrows(AccessDeniedException.class, () -> {
            taskService.updateTaskByAdmin(taskId, dto, USERNAME);
        });
    }

    @Test
    void updateTaskByAdmin_ShouldThrowException_WhenTaskNotFound() {
        //arrange
        TaskRequestDto dto = new TaskRequestDto("New Title", "New Description", TaskStatus.CANCELLED, 1L);

        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(TaskNotFoundException.class, () -> {
            taskService.updateTaskByAdmin(taskId, dto, USERNAME);
        });
    }

    @Test
    void delete_ShouldDeleteTask_WhenRequestIsValid() {
        //arrange
        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername(ADMIN)).thenReturn(Optional.of(admin));

        //act
        taskService.delete(taskId, ADMIN);

        //assert
        Mockito.verify(taskRepository, Mockito.times(1)).delete(task);
    }

    @Test
    void delete_ShouldThrowException_WhenNotAdmin() {
        //arrange
        task.setUser(admin);

        Mockito.when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        Mockito.when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));

        //act & assert
        assertThrows(AccessDeniedException.class, () -> {
            taskService.delete(taskId, USERNAME);
        });
        Mockito.verify(taskRepository, Mockito.never()).delete(Mockito.any());
    }

    private void mockSecurity(String username) {
        SecurityContext context =  SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
        SecurityContextHolder.setContext(context);
    }
}
