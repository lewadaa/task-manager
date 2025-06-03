package com.example.taskmanager.controller;

import com.example.taskmanager.dto.PageResponse;
import com.example.taskmanager.dto.UserRequestDto;
import com.example.taskmanager.dto.UserResponseDto;
import com.example.taskmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Пользователи", description = "Взаимодействие с пользователями")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "Получить всех пользователей",
            description = "Позволяет админу получить страницу со всеми пользователями"
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<UserResponseDto>> getAllUsers(
            @PageableDefault(size = 10, sort = "username", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.of(userService.findAll(pageable)));
    }

    @Operation(
            summary = "Получить пользователя по id",
            description = "Позволяет админу получить пользователя по id"
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(
            summary = "Создать пользователя",
            description = "Позволяет админу создать пользователя"
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(userRequestDto));
    }

    @Operation(
            summary = "Обновить пользователя",
            description = "Позволяет админу обновить данные пользователя по id"
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable @Min(1) Long id,
                                                      @Valid @RequestBody UserRequestDto userRequestDto) {
        return ResponseEntity.ok(userService.update(id, userRequestDto));
    }

    @Operation(
            summary = "Удалить пользователя",
            description = "Позволяет админу удалить пользователя по id"
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteUser(@PathVariable @Min(1) Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
