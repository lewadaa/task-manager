package com.example.taskmanager.service;

import com.example.taskmanager.dto.UserRequestDto;
import com.example.taskmanager.dto.UserResponseDto;
import com.example.taskmanager.entity.RoleType;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.UserNotFoundException;
import com.example.taskmanager.mapper.UserMapper;
import com.example.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private Long USER_ID;
    private User user;
    private User updatedUser;
    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;
    private UserResponseDto updatedUserResponseDto;

    @BeforeEach
    void setUp() {
        USER_ID = 1L;
        user = new User(USER_ID, "John", "password", RoleType.ROLE_USER);
        updatedUser = new User(USER_ID, "Max", "password", RoleType.ROLE_ADMIN);
        userRequestDto = new UserRequestDto("John", "password");
        userResponseDto = new UserResponseDto("John", RoleType.ROLE_USER.name());
        updatedUserResponseDto = new UserResponseDto("Max", RoleType.ROLE_ADMIN.name());
    }

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void findById_ShouldReturnUser_WhenUserExists() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(userMapper.mapToDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.findById(USER_ID);

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);
        Mockito.verify(userMapper, Mockito.times(1)).mapToDto(user);

        assertNotNull(result);
        assertEquals("John", result.getUsername());
        assertEquals(RoleType.ROLE_USER.name(), result.getRole());
    }

    @Test
    void findById_ShouldThrowException_WhenUserDoesNotExist() {
        Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.findById(99L));
    }

    @Test
    void create_ShouldCreateUser_whenRequestIsValid() {
        Mockito.when(userMapper.mapToEntity(userRequestDto)).thenReturn(user);
        Mockito.when(passwordEncoder.encode(userRequestDto.getPassword())).thenReturn("password");
        Mockito.when(userRepository.save(user)).thenReturn(user);
        Mockito.when(userMapper.mapToDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.create(userRequestDto);

        Mockito.verify(userMapper, Mockito.times(1)).mapToEntity(userRequestDto);
        Mockito.verify(passwordEncoder, Mockito.times(1)).encode(userRequestDto.getPassword());
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
        Mockito.verify(userMapper, Mockito.times(1)).mapToDto(user);

        assertNotNull(result);
        assertEquals("John", result.getUsername());
        assertEquals(RoleType.ROLE_USER.name(), result.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();

        assertEquals(userRequestDto.getUsername(), capturedUser.getUsername());
        assertEquals(userRequestDto.getPassword(), capturedUser.getPassword());
    }

    @Test
    void create_ShouldThrowException_WhenUserCreationFails() {
        Mockito.when(userMapper.mapToEntity(userRequestDto)).thenReturn(user);
        Mockito.when(userRepository.save(user)).thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.create(userRequestDto));
        assertEquals("Database error", exception.getMessage());

        Mockito.verify(userMapper, Mockito.times(1)).mapToEntity(userRequestDto);
        Mockito.verify(userRepository, Mockito.times(1)).save(user);
    }

    @Test
    void update_ShouldUpdateUser_whenRequestIsValid() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(userRepository.saveAndFlush(user)).thenReturn(updatedUser);
        Mockito.when(userMapper.mapToDto(updatedUser)).thenReturn(updatedUserResponseDto);

        UserResponseDto result = userService.update(USER_ID, userRequestDto);

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);
        Mockito.verify(userRepository, Mockito.times(1)).saveAndFlush(user);
        Mockito.verify(userMapper, Mockito.times(1)).mapToDto(updatedUser);

        assertNotNull(result);
        assertEquals("Max", result.getUsername());
        assertEquals(RoleType.ROLE_ADMIN.name(), result.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository, Mockito.times(1)).saveAndFlush(userCaptor.capture());

        User capturedUser = userCaptor.getValue();

        assertEquals(userRequestDto.getUsername(), capturedUser.getUsername());
        assertEquals(userRequestDto.getPassword(), capturedUser.getPassword());
    }

    @Test
    void update_ShouldThrowException_WhenUserDoesNotExist() {
        Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.update(99L, userRequestDto));
    }

    @Test
    void delete_ShouldDeleteUser_whenRequestIsValid() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.delete(USER_ID);

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);
        Mockito.verify(userRepository, Mockito.times(1)).delete(user);
    }

    @Test
    void delete_ShouldThrowException_WhenUserDoesNotExist() {
        Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.delete(99L));
    }
}
