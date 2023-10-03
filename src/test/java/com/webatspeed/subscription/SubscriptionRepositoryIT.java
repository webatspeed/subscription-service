package com.webatspeed.subscription;

import static org.junit.jupiter.api.Assertions.*;

import com.webatspeed.subscription.config.MongoConfiguration;
import com.webatspeed.subscription.model.Subscription;
import java.util.UUID;
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

  @Test
  void saveShouldCreateDefaultValues() {
    givenAValidEmail();
    givenAValidSubscriptionWith(email);

    subscriptionRepository.save(subscription);

    subscription = subscriptionRepository.findById(subscription.getId()).orElseThrow();
    assertFalse(subscription.getId().isBlank());
    assertEquals(36, subscription.getUserConfirmationToken().length());
    assertEquals(36, subscription.getOwnerConfirmationToken().length());
    assertEquals(36, subscription.getUserUnsubscribeToken().length());
    assertEquals(0, subscription.getNumTokenErrors());
    assertFalse(subscription.getConfirmedByUser());
    assertFalse(subscription.getConfirmedByOwner());
  }

  @Test
  void findByEmailAndNumTokenErrorsLessThanShouldFindSubscription() {
    givenAValidEmail();
    givenAValidSubscriptionWith(email, UUID.randomUUID().toString());
    givenTheSubscriptionSaved();

    var foundSubscription =
        subscriptionRepository.findByEmailAndNumTokenErrorsLessThan(email, 3).orElseThrow();

    assertEquals(subscription.getId(), foundSubscription.getId());
    assertEquals(subscription.getEmail(), foundSubscription.getEmail());
  }

  private void givenAValidEmail() {
    email = FAKER.internet().emailAddress();
  }

  private void givenAValidSubscriptionWith(String email) {
    givenAValidSubscriptionWith(email, null);
  }

  private void givenAValidSubscriptionWith(String email, String userConfirmationToken) {
    subscription =
        Instancio.of(Subscription.class)
            .set(Select.field("id"), null)
            .set(Select.field("email"), email)
            .set(Select.field("userConfirmationToken"), userConfirmationToken)
            .set(Select.field("ownerConfirmationToken"), null)
            .set(Select.field("userUnsubscribeToken"), null)
            .set(Select.field("numTokenErrors"), null)
            .set(Select.field("confirmedByUser"), null)
            .set(Select.field("confirmedByOwner"), null)
            .set(Select.field("createdAt"), null)
            .set(Select.field("modifiedAt"), null)
            .set(Select.field("version"), null)
            .create();
  }

  private void givenTheSubscriptionSaved() {
    subscriptionRepository.save(subscription);
  }
}
