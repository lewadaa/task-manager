package com.example.taskmanager.mapper;

import com.example.taskmanager.dto.UserRequestDto;
import com.example.taskmanager.dto.UserResponseDto;
import com.example.taskmanager.entity.RoleType;
import com.example.taskmanager.entity.User;
import com.example.taskmanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    private UserRepository userRepository;

    @Named("mapUserToDto")
    @Mapping(source = "role", target = "role")
    public abstract UserResponseDto mapToDto(User user);

    @Mapping(source = "password", target = "password")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    public abstract User mapToEntity(UserRequestDto userRequestDto);

    @Named("mapToEntityById")
    public User mapToEntityById(Long userId) {
        System.out.println("Mapping user with ID: " + userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
    }

    public String mapRoleToString(RoleType role) {
        return role.name();
    }
}
