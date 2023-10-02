package com.webatspeed.subscription;

import com.webatspeed.subscription.model.Subscription;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

  boolean existsByEmail(String email);
}
