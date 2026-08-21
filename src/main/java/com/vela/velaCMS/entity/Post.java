package com.vela.velaCMS.entity;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "posts")
@CompoundIndex(
        name = "owner_slug_unique",
        def = "{'ownerID': 1, 'slug': 1}",
        unique = true
)
public class Post {
    @Id
    private ObjectId id;
    @Indexed
    private ObjectId ownerID;
    private String title;
    private String description;
    @Indexed
    private String slug;
    @Indexed
    private String category;
    @Indexed
    private String postType;
    private String icon;
    private String actionLabel;
    private String actionLink;
    private boolean published;
    private Instant createdAt;
}