package com.webatspeed.subscription;

import com.webatspeed.subscription.model.Subscription;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
public class SubscriptionRepositoryIT {

  private static final Faker FAKER = new Faker();

  @Autowired SubscriptionRepository subscriptionRepository;

  private Subscription subscription;

  private String email;

  @AfterEach
  void cleanUp() {
    email = null;
    subscription = null;
    subscriptionRepository.deleteAll();
  }

  @Test
  void emailsShouldBeUnique() {
    givenAValidEmail();
    givenAValidSubscriptionWith(email);

    assertNotNull(subscriptionRepository.save(subscription));

    givenAValidSubscriptionWith(email);

    var exception =
        assertThrows(DuplicateKeyException.class, () -> subscriptionRepository.save(subscription));
    assertTrue(exception.getMessage().contains("duplicate key error"));
  }

  private void givenAValidEmail() {
    email = FAKER.internet().emailAddress();
  }

  private void givenAValidSubscriptionWith(String email) {
    subscription =
        Instancio.of(Subscription.class)
            .set(Select.field("id"), null)
            .set(Select.field("email"), email)
            .create();
  }
}
