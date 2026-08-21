package com.vela.velaCMS.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePostRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotBlank String slug,
        @NotBlank String category,
        @NotBlank String postType,
        String actionLabel,
        String actionLink,
        String icon,
        boolean published,
        @NotBlank String content
) {
}
