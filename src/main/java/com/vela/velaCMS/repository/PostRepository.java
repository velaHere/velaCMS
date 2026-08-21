package com.vela.velaCMS.repository;

import com.mongodb.client.result.UpdateResult;
import com.vela.velaCMS.dto.request.PostFrontMatterRequest;
import com.vela.velaCMS.entity.Post;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PostRepository {

    private static final int SIZE = 9;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public PostRepository(MongoTemplate template) {
        this.mongoTemplate = template;
    }

    public Optional<Post> findBySlug(ObjectId userID, String slug) {
        Query query = new Query();
        query.addCriteria(Criteria.where("slug").is(slug).andOperator(Criteria.where("ownerID").is(userID)));
        Post post = mongoTemplate.findOne(query, Post.class);
        return Optional.ofNullable(post);
    }

    public Optional<Post> findBySlugAndRemove(ObjectId userID, String slug) {
        Query query = new Query();
        query.addCriteria(Criteria.where("slug").is(slug).andOperator(Criteria.where("ownerID").is(userID)));
        Post post = mongoTemplate.findAndRemove(query, Post.class);
        return Optional.ofNullable(post);
    }

    public boolean updateFrontMatter(ObjectId ownerID, String currentSlug, PostFrontMatterRequest request) {

        Query query = new Query();
        query.addCriteria(Criteria.where("ownerID").is(ownerID).and("slug").is(currentSlug));

        Update update = new Update()
                .set("title", request.title())
                .set("description", request.description())
                .set("slug", request.slug())
                .set("category", request.category().toLowerCase())
                .set("postType", request.postType())
                .set("icon", request.icon())
                .set("actionLabel", request.actionLabel())
                .set("actionLink", request.actionLink())
                .set("published", request.published());

        UpdateResult result = mongoTemplate.updateFirst(query, update, Post.class);

        return result.getMatchedCount() > 0;
    }

    public List<Post> getAllPosts(@NotNull String userID, int page, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("ownerID").is(new ObjectId(userID)));
        query.skip((long) page*SIZE);
        query.limit(limit);
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Post.class);
    }

    public List<Post> getAllPosts(@NotNull String userID, @NotNull String category, int page, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("ownerID").is(new ObjectId(userID)).and("category").is(category));
        query.skip((long) page*SIZE);
        query.limit(limit);
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        return mongoTemplate.find(query, Post.class);
    }

    public boolean hasNext(@NotNull String userID, @NotNull String category, int page, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("ownerID").is(new ObjectId(userID)).and("category").is(category));
        long count = mongoTemplate.count(query, Post.class);
        return (long) (page + 1) *limit < count;
    }

    public boolean hasNext(@NotNull String userID, int page, int limit) {
        Query query = new Query();
        query.addCriteria(Criteria.where("ownerID").is(new ObjectId(userID)));
        long count = mongoTemplate.count(query, Post.class);
        return (long) (page + 1) *limit < count;
    }

    public void insert(Post post) {
        mongoTemplate.insert(post);
    }
}
