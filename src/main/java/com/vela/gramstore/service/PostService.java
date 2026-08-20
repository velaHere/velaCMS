package com.vela.gramstore.service;


import com.vela.gramstore.core.result.FailureType;
import com.vela.gramstore.core.result.Result;
import com.vela.gramstore.dto.request.CreatePostRequest;
import com.vela.gramstore.dto.request.PostFrontMatterRequest;
import com.vela.gramstore.dto.request.UpdatePostContentRequest;
import com.vela.gramstore.dto.response.PageResponse;
import com.vela.gramstore.dto.response.PostCardResponse;
import com.vela.gramstore.dto.response.PostResponse;
import com.vela.gramstore.entity.Post;
import com.vela.gramstore.entity.User;
import com.vela.gramstore.repository.PostRepository;
import com.vela.gramstore.repository.UserRepository;
import com.vela.gramstore.security.AuthenticatedUser;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PostService {

    private final Path postsPath;
    private final PostRepository repository;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;

    @Autowired
    public PostService(
            @Value("${app.storage.root}") String root,
            PostRepository repository,
            UserRepository userRepository,
            UserDetailsService userDetailsService
    ) {
        this.postsPath = Paths.get(root, "posts");
        this.repository = repository;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    public PostResponse getPost(@NotNull String username, @NotNull String slug) {
        User user = userRepository.findByUsername(username);
        if(user==null) return null;
        Optional<Post> opPost = repository.findBySlug(user.getId(), slug);
        if(opPost.isEmpty()) return null;
        Post post = opPost.get();

        String fileName = post.getId() + ".md";
        Path file = postsPath.resolve(user.getId().toString()).resolve(fileName);

        if(!Files.exists(file)) return null;

        try {
            String matter = getFrontMatter(post);
            String content = Files.readString(file);
            return new PostResponse(matter + content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the contents of the file: " + fileName, e);
        }
    }

    public PostResponse getDescription(@NotNull String username, @NotNull String slug) {

        User user = userRepository.findByUsername(username);
        if(user==null) return null;
        Optional<Post> opPost = repository.findBySlug(user.getId(), slug);
        if(opPost.isEmpty()) return null;
        Post post = opPost.get();

        String fileName = post.getId() + ".md";
        Path file = postsPath.resolve(user.getId().toString()).resolve(fileName);

        if(!Files.exists(file)) return null;

        try {
            return new PostResponse(Files.readString(file));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read the contents of the file: " + fileName, e);
        }
    }

    public Result<PageResponse<PostCardResponse>> getAllPosts(@NotNull String username, @Nullable String category, int page, int limit) {
        if(page < 0 || limit <= 0) return Result.failure(FailureType.INVALID_ARGUMENTS);
        AuthenticatedUser user = (AuthenticatedUser) userDetailsService.loadUserByUsername(username);
        if(user==null) return Result.failure(FailureType.CONTENT_NOT_FOUND);

        if (limit > 9) limit = 9;
        boolean hasNext;
        List<Post> posts;

        if(category == null) {
            posts = repository.getAllPosts(user.getId(), page, limit);
            hasNext = repository.hasNext(user.getId(), page, limit);
        }
        else {
            posts = repository.getAllPosts(user.getId(), category, page, limit);
            hasNext = repository.hasNext(user.getId(), category, page, limit);
        }

        return Result.success(
               new PageResponse<>(
                       posts.stream()
                               .map(PostCardResponse::buildFromPost)
                               .toList(),
                       page,
                       limit,
                       hasNext
               )
        );
    }

    @Transactional
    public void deletePost(@NotNull String username, @NotNull String slug) {

        User user = userRepository.findByUsername(username);
        if(user==null)
            throw new RuntimeException("User not found: " + username);

        Optional<Post> post = repository.findBySlugAndRemove(user.getId(), slug);
        if(post.isEmpty())
            throw new RuntimeException("Post not found with slug: " + slug);

        String fileName = post.get().getId() + ".md";
        Path file = postsPath.resolve(user.getId().toString()).resolve(fileName);

        try {
            Files.deleteIfExists(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete the post: " + slug, e);
        }
    }

    @Transactional
    public ResponseEntity<?> createPost(@NotNull CreatePostRequest request, @NotNull String username) {

        if(!request.slug().matches("[a-z0-9-]+"))
            throw new RuntimeException("Invalid slug");

        switch (request.category().toLowerCase()) {
            case "releases", "updates", "blogs" -> {}
            default -> throw new RequestRejectedException("Invalid Post Category");
        }

        ObjectId postID = new ObjectId();

        String fileName = postID + ".md";

        User user = userRepository.findByUsername(username);

        if(user==null)
            throw new RequestRejectedException("User not found: " + username);

        String userID = user.getId().toString();

        Path file = postsPath
                .resolve(userID)
                .resolve(fileName);

        try {
            Files.createDirectories(file.getParent());
        } catch (Exception e) {
            throw new RuntimeException("failed to create the Directory", e);
        }

        if(Files.exists(file))
            throw new RuntimeException("Post: " + fileName + " already exists");

        repository.insert(
                Post.builder()
                        .id(postID)
                        .ownerID(new ObjectId(userID))
                        .title(request.title())
                        .description(request.description())
                        .createdAt(Instant.now())
                        .slug(request.slug())
                        .category(request.category().toLowerCase())
                        .postType(request.postType())
                        .published(request.published())
                        .icon(request.icon())
                        .actionLabel(request.actionLabel())
                        .actionLink(request.actionLink())
                        .build()
        );

        try {
            String markdown = buildMarkdown(request.content());
            Files.writeString(file, markdown, StandardOpenOption.CREATE_NEW);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(file);
            } catch (Exception deleteError) {
                log.error("Failed rollback", deleteError);
            }
            throw new RuntimeException("Failed to create the post: " + fileName, e);
        }

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> updatePostContent(@NotNull UpdatePostContentRequest request, @NotNull String username, @NotNull String slug) {
        User user = userRepository.findByUsername(username);
        if(user==null)
            return ResponseEntity.notFound().build();

        Optional<Post> result = repository.findBySlug(user.getId(), slug);
        if(result.isEmpty())
            return ResponseEntity.notFound().build();

        Path file = postsPath
                .resolve(user.getId().toString())
                .resolve(result.get().getId() + ".md");

        if(!Files.exists(file))
            return ResponseEntity.notFound().build();

        try {
            Files.writeString(
                    file,
                    buildMarkdown(request.content()),
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to update the post content.", e);
        }

        return ResponseEntity.ok().build();
    }

    @Transactional
    public ResponseEntity<?> updatePostFrontMatter(@NotNull PostFrontMatterRequest request, @NotNull String username, @NotNull String currentSlug) {

        if(!request.slug().matches("[a-z0-9-]+"))
            throw new RuntimeException("Invalid slug");

        switch (request.category().toLowerCase()) {
            case "releases", "updates", "blogs" -> {}
            default -> throw new RequestRejectedException("Invalid Post Category");
        }

        User user = userRepository.findByUsername(username);
        if(user==null)
            throw new RequestRejectedException("User not found: " + username);

        boolean result;
        try {
            result = repository.updateFrontMatter(user.getId(), currentSlug, request);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("Error while updating the post of " + username +  ": ", e);
        }

        return result ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private String buildMarkdown(String content) {
        return """
               \s
                %s
               \s
               """
                .formatted(content);
    }

    public String getFrontMatter(@NotNull Post post) {
        return """
                ---
                 layout: post
                 title: "%s"
                 description: "%s"
                 date: %s
                 category: %s
                 project_type: %s
                 icon: %s
                 action_label: %s
                 action_link: %s
                ---
                """
                .formatted(
                        escape(post.getTitle()),
                        escape(post.getDescription()),
                        escape(post.getCreatedAt().toString()),
                        escape(post.getCategory()),
                        escape(post.getPostType()),
                        escape(post.getIcon()),
                        escape(post.getActionLabel()),
                        escape(post.getActionLink())
                );
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
