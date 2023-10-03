package com.webatspeed.subscription;

import com.webatspeed.subscription.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

  boolean existsByEmail(String email);

  Optional<Subscription> findByEmailAndNumTokenErrorsLessThan(String email, Integer numTokenErrors);
}
