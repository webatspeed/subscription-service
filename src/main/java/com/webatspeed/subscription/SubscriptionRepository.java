package com.webatspeed.subscription;

import com.webatspeed.subscription.model.Subscription;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SubscriptionRepository extends MongoRepository<Subscription, String> {

  boolean existsByEmail(String email);

  Optional<Subscription> findByEmailAndNumTokenErrorsLessThan(String email, Integer numTokenErrors);

  Page<Subscription> findAllByConfirmedByOwnerIsTrue(Pageable pageable);
}
