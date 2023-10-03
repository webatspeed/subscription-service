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

  public Subscription applyToken(Subscription subscription, String token) {
    if (subscription.getConfirmedByUser()
        && token.equals(subscription.getOwnerConfirmationToken())) {
      subscription.setConfirmedByOwner(true);
      subscription.resetNumTokenErrors();
    } else if (token.equals(subscription.getUserConfirmationToken())) {
      subscription.setConfirmedByUser(true);
      subscription.resetNumTokenErrors();
    } else {
      subscription.incNumTokenErrors();
      throw new FalseTokenException();
    }

    repository.save(subscription);

    return subscription;
  }
}
