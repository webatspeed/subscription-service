package com.webatspeed.subscription;

import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.model.Subscription;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionMapper {

  public Subscription subscriptionOf(SubscriptionDetails subscriptionDetails) {
    var subscription = new Subscription();
    subscription.setEmail(subscriptionDetails.email());

    return subscription;
  }
}
