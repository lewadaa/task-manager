package com.example.taskmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Универсальный ответ с пагинацией")
public record PageResponse<T>(

        @Schema(description = "Список элементов на текущей странице")
        List<T> content,

        @Schema(description = "Номер текущей страницы(начиная с 0)")
        int page,

        @Schema(description = "Общий размер страницы")
        int size,

        @Schema(description = "Общее количество элементов")
        long totalElements,

        @Schema(description = "Общее количество страниц")
        int totalPages,

        @Schema(description = "Последняя ли это страница")
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
