package com.example.taskmanager.service;

import com.example.taskmanager.dto.UserRequestDto;
import com.example.taskmanager.dto.UserResponseDto;
import com.example.taskmanager.entity.RoleType;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.exception.UserNotFoundException;
import com.example.taskmanager.mapper.UserMapper;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TaskRepository taskRepository;

    public Page<UserResponseDto> findAll(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(userMapper::mapToDto);
    }

    public UserResponseDto findById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::mapToDto)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    @Transactional
    public UserResponseDto create(UserRequestDto userRequestDto) {
        if (userRepository.existsByUsername(userRequestDto.getUsername())) {
            throw new RuntimeException("Username " + userRequestDto.getUsername() + " already exists");
        }
        return Optional.of(userRequestDto)
                .map(userMapper::mapToEntity)
                .map(entity -> {
                    entity.setRole(RoleType.ROLE_USER);
                    entity.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));
                    return userRepository.save(entity);
                })
                .map(userMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("User cannot be created"));
    }

    @Transactional
    public UserResponseDto update(Long id, UserRequestDto userRequestDto) {
        var existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        BeanUtils.copyProperties(userRequestDto, existingUser, getNullPropertyNames(userRequestDto));

        var savedUser = userRepository.saveAndFlush(existingUser);

        return userMapper.mapToDto(savedUser);
    }

    private String[] getNullPropertyNames(Object source) {
        try {
            return Arrays.stream(Introspector.getBeanInfo(source.getClass(), Object.class)
                    .getPropertyDescriptors())
                    .map(PropertyDescriptor::getName)
                    .filter(name -> {
                        try {
                            return Objects.isNull(new PropertyDescriptor(name, source.getClass()).getReadMethod().invoke(source));
                        } catch (Exception e) {
                            return false;
                        }
                    }).toArray(String[]::new);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.deleteByUserId(id);
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        userRepository.delete(user);
    }
}
