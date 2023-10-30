package com.webatspeed.subscription.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
public class SubscriptionControllerSecurityTests {
  private static final Faker FAKER = new Faker();

  @Autowired private MockMvc mockMvc;

  private String originUrl;

  @AfterEach
  void cleanUp() {
    originUrl = null;
  }

  @Test
  void createSubscriptionShouldRespondWithForbiddenOnFalseOrigins() throws Exception {
    givenAFalseOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription")
                .header("Access-Control-Request-Method", "POST")
                .header("Origin", originUrl))
        .andExpect(status().isForbidden());
  }

  @Test
  void createSubscriptionShouldRespondWithSuccessOnCorrectOrigins() throws Exception {
    givenTheCorrectOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription")
                .header("Access-Control-Request-Method", "POST")
                .header("Origin", originUrl))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void updateSubscriptionShouldRespondWithForbiddenOnFalseOrigins() throws Exception {
    givenAFalseOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription")
                .header("Access-Control-Request-Method", "PUT")
                .header("Origin", originUrl))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateSubscriptionShouldRespondWithSuccessOnCorrectOrigins() throws Exception {
    givenTheCorrectOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription")
                .header("Access-Control-Request-Method", "PUT")
                .header("Origin", originUrl))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void deleteSubscriptionShouldRespondWithForbiddenOnFalseOrigins() throws Exception {
    givenAFalseOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Origin", originUrl))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteSubscriptionShouldRespondWithSuccessOnCorrectOrigins() throws Exception {
    givenTheCorrectOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Origin", originUrl))
        .andExpect(status().is2xxSuccessful());
  }

  @Test
  void applySubscriptionsShouldRespondWithForbiddenOnFalseOrigins() throws Exception {
    givenAFalseOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription/distribute")
                .header("Access-Control-Request-Method", "POST")
                .header("Origin", originUrl))
        .andExpect(status().isForbidden());
  }

  @Test
  void applySubscriptionsShouldRespondWithSuccessOnCorrectOrigins() throws Exception {
    givenTheCorrectOriginUrl();

    mockMvc
        .perform(
            options("/v1/subscription/distribute")
                .header("Access-Control-Request-Method", "POST")
                .header("Origin", originUrl))
        .andExpect(status().is2xxSuccessful());
  }

  private void givenTheCorrectOriginUrl() {
    originUrl = "http://localhost:3000";
  }

  private void givenAFalseOriginUrl() {
    originUrl = FAKER.internet().url();
  }
}
