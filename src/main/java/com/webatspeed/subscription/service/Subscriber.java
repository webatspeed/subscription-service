package com.webatspeed.subscription.service;

import com.webatspeed.subscription.SubscriptionRepository;
import com.webatspeed.subscription.exception.FalseTokenException;
import com.webatspeed.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Subscriber {

  private final SubscriptionRepository repository;

  public Subscription applyUpdateToken(Subscription subscription, String token) {
    if (subscription.getConfirmedByUser()
        && token.equals(subscription.getOwnerConfirmationToken())) {
      subscription.setConfirmedByOwner(true);
    } else if (token.equals(subscription.getUserConfirmationToken())) {
      subscription.setConfirmedByUser(true);
    } else {
      subscription.incNumTokenErrors();
      repository.save(subscription);
      throw new FalseTokenException();
    }

    subscription.resetNumTokenErrors();
    repository.save(subscription);

    return subscription;
  }

  public Subscription applyDeleteToken(Subscription subscription, String token) {
    if (token.equals(subscription.getUserUnsubscribeToken())) {
      repository.delete(subscription);
    } else {
      subscription.incNumTokenErrors();
      repository.save(subscription);
      throw new FalseTokenException();
    }

    return subscription;
  }
}
