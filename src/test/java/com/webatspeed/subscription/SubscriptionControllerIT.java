package com.webatspeed.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@AutoConfigureMockMvc
@SpringBootTest
public class SubscriptionControllerIT {
  private static final Faker FAKER = new Faker();

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  private SubscriptionDetails subscriptionDetails;

  @AfterEach
  void cleanUp() {
    subscriptionDetails = null;
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestIfEmailNull() throws Exception {
    givenSubscriptionDetailsWithNullEmail();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestIfEmailEmpty() throws Exception {
    givenSubscriptionDetailsWithNullEmpty();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithBadRequestOnNonEmailString() throws Exception {
    givenSubscriptionDetailsWithNonEmailString();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  void createSubscriptionShouldRespondWithNotImplementedOnValidDetails() throws Exception {
    givenValidSubscriptionDetails();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(MockMvcResultMatchers.status().isNotImplemented());
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
}
