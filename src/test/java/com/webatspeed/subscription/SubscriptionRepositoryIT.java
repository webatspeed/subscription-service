package com.webatspeed.subscription;

import static org.junit.jupiter.api.Assertions.*;

import com.webatspeed.subscription.config.MongoConfiguration;
import com.webatspeed.subscription.model.Subscription;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;

@DataMongoTest
@Import(MongoConfiguration.class)
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

  @Test
  void saveShouldAudit() {
    givenAValidEmail();
    givenAValidSubscriptionWith(email);

    subscriptionRepository.save(subscription);

    var createdAt = subscription.getCreatedAt();
    assertNotNull(createdAt);
    assertEquals(createdAt, subscription.getModifiedAt());
    assertEquals(0, subscription.getVersion());

    givenAValidEmail();
    subscription.setEmail(email);
    subscriptionRepository.save(subscription);

    assertEquals(createdAt, subscription.getCreatedAt());
    assertTrue(createdAt.isBefore(subscription.getModifiedAt()));
    assertEquals(1, subscription.getVersion());
  }

  private void givenAValidEmail() {
    email = FAKER.internet().emailAddress();
  }

  private void givenAValidSubscriptionWith(String email) {
    subscription =
        Instancio.of(Subscription.class)
            .set(Select.field("id"), null)
            .set(Select.field("email"), email)
            .set(Select.field("createdAt"), null)
            .set(Select.field("modifiedAt"), null)
            .set(Select.field("version"), null)
            .create();
  }
}
