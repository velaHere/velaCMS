package com.vela.velaCMS.dto.response;

import com.vela.velaCMS.entity.Post;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public record PostCardResponse(
        String title,
        String description,
        String slug,
        String category,
        String icon,
        Instant createdAt
) {

    public static PostCardResponse buildFromPost(@NotNull Post post) {
        return new PostCardResponse(
                post.getTitle(),
                post.getDescription(),
                post.getSlug(),
                post.getCategory(),
                post.getIcon(),
                post.getCreatedAt()
        );
    }
}
