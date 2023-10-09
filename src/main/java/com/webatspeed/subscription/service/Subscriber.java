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

  private final Mailer mailer;

  public void initiateToken(Subscription subscription) {
    mailer.emailPleaseConfirm(subscription.getEmail(), subscription.getUserConfirmationToken());
  }

  public Subscription applyUpdateToken(Subscription subscription, String token) {
    var email = subscription.getEmail();

    if (subscription.getConfirmedByUser()
        && token.equals(subscription.getOwnerConfirmationToken())) {
      subscription.setConfirmedByOwner(true);
      mailer.emailFirstCv(email, subscription.getUserUnsubscribeToken());
    } else if (token.equals(subscription.getUserConfirmationToken())) {
      subscription.setConfirmedByUser(true);
      mailer.emailPleaseWait(email);
      mailer.emailPleaseApprove(email, subscription.getOwnerConfirmationToken());
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
