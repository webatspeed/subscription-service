package com.webatspeed.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.model.Subscription;
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

  @SuppressWarnings("FieldCanBeLocal")
  private Subscription subscription;

  @AfterEach
  void cleanUp() {
    subscriptionDetails = null;
    subscriptionRepository.deleteAll();
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestIfEmailNull() throws Exception {
    givenSubscriptionDetailsWithNullEmail();

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestIfEmailEmpty() throws Exception {
    givenSubscriptionDetailsWithNullEmpty();

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestOnNonEmailString() throws Exception {
    givenSubscriptionDetailsWithNonEmailString();

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithConflictOnExistingEmail() throws Exception {
    givenValidSubscriptionDetails();
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
    givenValidSubscriptionDetails();

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

  private void givenSubscriptionDetailsWithNullEmail() {
    subscriptionDetails = new SubscriptionDetails(null);
  }

  private void givenSubscriptionDetailsWithNullEmpty() {
    subscriptionDetails = new SubscriptionDetails("");
  }

  private void givenSubscriptionDetailsWithNonEmailString() {
    var noEmail = FAKER.internet().emailAddress().replace('@', 'a');
    subscriptionDetails = new SubscriptionDetails(noEmail);
  }

  private void givenValidSubscriptionDetails() {
    subscriptionDetails = new SubscriptionDetails(FAKER.internet().emailAddress());
  }

  private void givenAnExistingSubscription(String email) {
    subscription =
        Instancio.of(Subscription.class)
            .set(Select.field("id"), null)
            .set(Select.field("email"), email)
            .set(Select.field("createdAt"), null)
            .set(Select.field("modifiedAt"), null)
            .set(Select.field("version"), null)
            .create();
    subscriptionRepository.save(subscription);
    subscriptionDetails = new SubscriptionDetails(email);
  }
}
