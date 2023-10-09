package com.webatspeed.subscription;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.model.SendEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.model.Subscription;
import java.util.UUID;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
public class SubscriptionControllerTests {
  private static final Faker FAKER = new Faker();

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private SubscriptionRepository subscriptionRepository;

  @MockBean private AmazonSimpleEmailServiceV2 emailClient;

  @Captor ArgumentCaptor<SendEmailRequest> captor;

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
    givenSubscriptionDetailsWithoutToken();
    givenAnExistingSubscription(subscriptionDetails.email());

    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isConflict());
  }

  @Test
  void createSubscriptionShouldRespondWithCreatedOnValidDetailsAndSendEmail() throws Exception {
    givenSubscriptionDetailsWithoutToken();

    assertEquals(0, subscriptionRepository.count());
    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isCreated());

    var subscriptionsSaved = subscriptionRepository.findAll();
    assertEquals(1, subscriptionsSaved.size());
    var subscriptionSaved = subscriptionsSaved.get(0);
    assertEquals(subscriptionDetails.email(), subscriptionSaved.getEmail());
    verify(emailClient).sendEmail(captor.capture());

    var request = captor.getValue();
    assertEquals(subscriptionDetails.email(), request.getDestination().getToAddresses().get(0));
    var template = request.getContent().getTemplate();
    assertEquals("please-confirm", template.getTemplateName());
    assertTrue(template.getTemplateData().contains(subscriptionSaved.getUserConfirmationToken()));
    assertTrue(template.getTemplateData().contains(subscriptionDetails.email()));
    assertEquals("test@email.local", request.getFromEmailAddress());
  }

  @Test
  void updateSubscriptionShouldRespondWithBadRequestOnWithNullEmail() throws Exception {
    givenSubscriptionDetailsWithoutToken();

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateSubscriptionShouldRespondWithBadRequestOnWithEmptyToken() throws Exception {
    givenSubscriptionDetailsWithEmptyToken();

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateSubscriptionShouldRespondWithNotFoundOnUnknownEmail() throws Exception {
    givenFullSubscriptionDetails();

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateSubscriptionShouldRespondWithBadRequestOnNotMatchingToken() throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString());

    var s =
        subscriptionRepository
            .findByEmailAndNumTokenErrorsLessThan(subscription.getEmail(), 3)
            .orElseThrow();
    assertEquals(0, s.getNumTokenErrors());

    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());

    s =
        subscriptionRepository
            .findByEmailAndNumTokenErrorsLessThan(subscription.getEmail(), 3)
            .orElseThrow();
    assertEquals(1, s.getNumTokenErrors());
  }

  @Test
  void updateSubscriptionShouldRespondWithNotFoundAfterThreeFails() throws Exception {
    givenFullSubscriptionDetails();
    var token = UUID.randomUUID().toString();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        token,
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString());

    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              put("/v1/subscription")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(subscriptionDetails)))
          .andExpect(status().isBadRequest());
    }

    givenFullSubscriptionDetails(subscriptionDetails.email(), token);
    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateSubscriptionShouldRespondWithNoContentOnValidUserToken() throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        subscriptionDetails.token(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString());

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
    assertEquals(0, savedSubscription.getNumTokenErrors());
  }

  @Test
  void updateSubscriptionShouldRespondWithNoContentOnValidOwnerToken() throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        subscriptionDetails.token(),
        UUID.randomUUID().toString());

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
    assertEquals(0, savedSubscription.getNumTokenErrors());
  }

  @Test
  void deleteSubscriptionShouldRespondWithBadRequestOnWithNullEmail() throws Exception {
    givenSubscriptionDetailsWithoutToken();

    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteSubscriptionShouldRespondWithBadRequestOnWithEmptyToken() throws Exception {
    givenSubscriptionDetailsWithEmptyToken();

    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deleteSubscriptionShouldRespondWithBadRequestOnNotMatchingToken() throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString());

    var s =
        subscriptionRepository
            .findByEmailAndNumTokenErrorsLessThan(subscription.getEmail(), 3)
            .orElseThrow();
    assertEquals(0, s.getNumTokenErrors());

    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isBadRequest());

    s =
        subscriptionRepository
            .findByEmailAndNumTokenErrorsLessThan(subscription.getEmail(), 3)
            .orElseThrow();
    assertEquals(1, s.getNumTokenErrors());
  }

  @Test
  void deleteSubscriptionShouldRespondWithNoContentOnValidUnsubscribeTokenAndExistingSubscription()
      throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        subscriptionDetails.token());

    assertEquals(1, subscriptionRepository.count());
    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    assertEquals(0, subscriptionRepository.count());
  }

  @Test
  void deleteSubscriptionShouldRespondWithNoContentOnTokenAndNotExistingSubscription()
      throws Exception {
    givenFullSubscriptionDetails();

    assertEquals(0, subscriptionRepository.count());
    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    assertEquals(0, subscriptionRepository.count());
  }

  @Test
  void deleteSubscriptionShouldRespondWithNotFoundAfterThreeFails() throws Exception {
    givenFullSubscriptionDetails();
    var token = UUID.randomUUID().toString();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        token);

    for (int i = 0; i < 3; i++) {
      mockMvc
          .perform(
              delete("/v1/subscription")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(subscriptionDetails)))
          .andExpect(status().isBadRequest());
    }

    givenFullSubscriptionDetails(subscriptionDetails.email(), token);
    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNotFound());
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

  private void givenSubscriptionDetailsWithoutToken() {
    subscriptionDetails = new SubscriptionDetails(FAKER.internet().emailAddress(), null);
  }

  private void givenSubscriptionDetailsWithEmptyToken() {
    subscriptionDetails = new SubscriptionDetails(FAKER.internet().emailAddress(), "");
  }

  private void givenFullSubscriptionDetails() {
    givenFullSubscriptionDetails(FAKER.internet().emailAddress(), UUID.randomUUID().toString());
  }

  private void givenFullSubscriptionDetails(String email, String token) {
    subscriptionDetails =
        Instancio.of(SubscriptionDetails.class)
            .set(Select.field("email"), email)
            .set(Select.field("token"), token)
            .create();
  }

  private void givenAnExistingSubscription(String email) {
    givenAnExistingSubscription(email, null, null, null);
  }

  private void givenAnExistingSubscription(
      String email,
      String userConfirmationToken,
      String ownerConfirmationToken,
      String userUnsubscribeToken) {
    subscription =
        Instancio.of(Subscription.class)
            .set(Select.field("id"), null)
            .set(Select.field("email"), email)
            .set(Select.field("userConfirmationToken"), userConfirmationToken)
            .set(Select.field("ownerConfirmationToken"), ownerConfirmationToken)
            .set(Select.field("userUnsubscribeToken"), userUnsubscribeToken)
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
