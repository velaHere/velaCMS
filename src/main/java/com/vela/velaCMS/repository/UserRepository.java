package com.vela.velaCMS.repository;

import com.vela.velaCMS.entity.User;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class UserRepository {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public UserRepository(MongoTemplate mongoTemplate){
        this.mongoTemplate=mongoTemplate;
    }

    public Optional<User> findById(ObjectId userId){
        try{
            User user = mongoTemplate.findById(userId, User.class);
            if(user == null) return Optional.empty();
            return Optional.of(user);
        }catch(Exception e){
            log.error("Error: ", e);
            return Optional.empty();
        }
    }

    public User findByEmail(String email){
        try{
            Query query = new Query();
            query.addCriteria(Criteria.where("email").is(email));
            return mongoTemplate.findOne(query, User.class);
        }catch(Exception e){
            log.error(e.getMessage());
            log.error(Arrays.toString(e.getStackTrace()));
            return null;
        }
    }

    public User findByUsername(String username){
        try{
            Query query = new Query();
            query.addCriteria(Criteria.where("username").is(username));
            return mongoTemplate.findOne(query, User.class);
        }catch(Exception e){
            return null;
        }
    }

    public void update(String userID, String key, Object value) {
        Query query = new Query();
        query.addCriteria(Criteria.where("id").is(userID));

        Update update = new Update().set(key, value);

        mongoTemplate.updateFirst(query, update, User.class);
    }

    public void insert(User user){
        mongoTemplate.insert(user);
    }
}
