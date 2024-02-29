package com.webatspeed.subscription.service;

import com.webatspeed.subscription.SubscriptionRepository;
import com.webatspeed.subscription.exception.FalseTokenException;
import com.webatspeed.subscription.model.Subscription;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Subscriber {

  private final SubscriptionRepository repository;

  private final Mailer mailer;

  private final RateLimiterProperties rateLimiterProperties;

  private final Semaphore distributionLock = new Semaphore(1);

  public void initiateToken(Subscription subscription) {
    mailer.emailPleaseConfirm(subscription.getEmail(), subscription.getUserConfirmationToken());
  }

  public Subscription applyUpdateToken(Subscription subscription, String token) {
    var email = subscription.getEmail();

    if (subscription.getConfirmedByUser()
        && token.equals(subscription.getOwnerConfirmationToken())) {
      subscription.setConfirmedByOwner(true);
      mailer.emailCv(email, subscription.getUserUnsubscribeToken(), true);
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

  @Async
  public void distribute() {
    var pageIndex = 0;
    var pageSize = rateLimiterProperties.getInstances().get("ses").getLimitForPeriod();
    Page<Subscription> subscriptionPage;

    try {
      distributionLock.acquire();
      do {
        @SuppressWarnings("ConstantConditions")
        var pageRequest = PageRequest.of(pageIndex, pageSize);
        subscriptionPage = repository.findAllByConfirmedByOwnerIsTrue(pageRequest);
        subscriptionPage
            .getContent()
            .forEach(s -> mailer.emailCv(s.getEmail(), s.getUserUnsubscribeToken(), false));
        pageIndex++;
      } while (subscriptionPage.hasNext());
    } catch (InterruptedException e) {
      log.error("Distribution interrupted", e);
    } finally {
      distributionLock.release();
    }
  }

  public boolean isDistributing() {
    return distributionLock.availablePermits() == 0;
  }
}
