package com.webatspeed.subscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.model.Subscription;
import java.util.UUID;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
public class SubscriptionControllerIT {
  private static final Faker FAKER = new Faker();

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private SubscriptionRepository subscriptionRepository;

  private SubscriptionDetails subscriptionDetails;

  private Subscription subscription;

  @AfterEach
  void cleanUp() {
    subscription = null;
    subscriptionDetails = null;
    subscriptionRepository.deleteAll();
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestIfEmailNull() throws Exception {
    givenCreateSubscriptionDetailsWithNullEmail();

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestIfEmailEmpty() throws Exception {
    givenCreateSubscriptionDetailsWithEmptyEmail();

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestOnNonEmailString() throws Exception {
    givenCreateSubscriptionDetailsWithNonEmailString();

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithConflictOnExistingEmail() throws Exception {
    givenValidCreateSubscriptionDetails();
    givenAnExistingSubscription(subscriptionDetails.email());

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isConflict());
  }

  @Test
  void createSubscriptionShouldRespondWithNoContentOnValidDetails() throws Exception {
    givenValidCreateSubscriptionDetails();

    assertEquals(0, subscriptionRepository.count());
    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    var subscriptions = subscriptionRepository.findAll();
    assertEquals(1, subscriptions.size());
    assertEquals(subscriptionDetails.email(), subscriptions.get(0).getEmail());
  }

  @Test
  void updateSubscriptionShouldRespondWithBadRequestOnWithNullEmail() throws Exception {
    givenValidCreateSubscriptionDetails();

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateSubscriptionShouldRespondWithBadRequestOnWithEmptyToken() throws Exception {
    givenUpdateSubscriptionDetailsWithEmptyToken();

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateSubscriptionShouldRespondWithNotFoundOnUnknownEmail() throws Exception {
    givenValidUpdateSubscriptionDetailsByUser();

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateSubscriptionShouldRespondWithNoContentOnValidUserToken() throws Exception {
    givenValidUpdateSubscriptionDetailsByUser();
    givenAnExistingSubscription(subscriptionDetails.email(), subscriptionDetails.token(), null);

    assertEquals(1, subscriptionRepository.count());
    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    var subscriptions = subscriptionRepository.findAll();
    assertEquals(1, subscriptions.size());
    var savedSubscription = subscriptions.get(0);
    assertEquals(subscriptionDetails.email(), savedSubscription.getEmail());
    assertTrue(savedSubscription.getConfirmedByUser());
    assertFalse(savedSubscription.getConfirmedByOwner());
  }

  @Test
  void updateSubscriptionShouldRespondWithNoContentOnValidOwnerToken() throws Exception {
    givenValidUpdateSubscriptionDetailsByUser();
    givenAnExistingSubscription(subscriptionDetails.email(), null, subscriptionDetails.token());

    assertEquals(1, subscriptionRepository.count());
    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    var subscriptions = subscriptionRepository.findAll();
    assertEquals(1, subscriptions.size());
    var savedSubscription = subscriptions.get(0);
    assertEquals(subscriptionDetails.email(), savedSubscription.getEmail());
    assertTrue(savedSubscription.getConfirmedByUser());
    assertTrue(savedSubscription.getConfirmedByOwner());
  }

  private void givenCreateSubscriptionDetailsWithNullEmail() {
    subscriptionDetails = new SubscriptionDetails(null, null);
  }

  private void givenCreateSubscriptionDetailsWithEmptyEmail() {
    subscriptionDetails = new SubscriptionDetails("", null);
  }

  private void givenCreateSubscriptionDetailsWithNonEmailString() {
    var noEmail = FAKER.internet().emailAddress().replace('@', 'a');
    subscriptionDetails = new SubscriptionDetails(noEmail, null);
  }

  private void givenValidCreateSubscriptionDetails() {
    subscriptionDetails = new SubscriptionDetails(FAKER.internet().emailAddress(), null);
  }

  private void givenUpdateSubscriptionDetailsWithEmptyToken() {
    subscriptionDetails = new SubscriptionDetails(FAKER.internet().emailAddress(), "");
  }

  private void givenValidUpdateSubscriptionDetailsByUser() {
    subscriptionDetails =
        Instancio.of(SubscriptionDetails.class)
            .set(Select.field("email"), FAKER.internet().emailAddress())
            .set(Select.field("token"), UUID.randomUUID().toString())
            .create();
  }

  private void givenAnExistingSubscription(String email) {
    givenAnExistingSubscription(email, null, null);
  }

  private void givenAnExistingSubscription(
      String email, String userConfirmationToken, String ownerConfirmationToken) {
    subscription =
        Instancio.of(Subscription.class)
            .set(Select.field("id"), null)
            .set(Select.field("email"), email)
            .set(Select.field("userConfirmationToken"), userConfirmationToken)
            .set(Select.field("ownerConfirmationToken"), ownerConfirmationToken)
            .set(Select.field("userUnsubscribeToken"), null)
            .set(Select.field("numTokenErrors"), null)
            .set(Select.field("confirmedByUser"), ownerConfirmationToken != null)
            .set(Select.field("confirmedByOwner"), null)
            .set(Select.field("createdAt"), null)
            .set(Select.field("modifiedAt"), null)
            .set(Select.field("version"), null)
            .create();
    subscriptionRepository.save(subscription);
  }
}
