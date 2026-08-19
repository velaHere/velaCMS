package com.vela.gramstore.dto.request;

public record PostFrontMatterRequest(
        String title,
        String description,
        String slug,
        String category,
        String postType,
        String icon,
        String actionLabel,
        String actionLink,
        boolean published
) { }
