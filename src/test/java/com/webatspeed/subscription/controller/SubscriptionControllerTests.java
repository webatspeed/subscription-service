package com.webatspeed.subscription.controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.SubscriptionRepository;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.model.Subscription;
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
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
public class SubscriptionControllerTests {
  private static final Faker FAKER = new Faker();

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private SubscriptionRepository subscriptionRepository;

  @MockBean
  private SesV2Client emailClient;

  @MockBean private AmazonS3 storageClient;

  @Captor ArgumentCaptor<SendEmailRequest> captor;

  private SubscriptionDetails subscriptionDetails;

  private Subscription subscription;

  private ListObjectsV2Result objectsResult;

  @AfterEach
  void cleanUp() {
    subscription = null;
    subscriptionDetails = null;
    subscriptionRepository.deleteAll();
    objectsResult = null;
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    assertEquals(subscriptionDetails.email(), request.destination().toAddresses().get(0));
    var template = request.content().template();
    assertEquals("please-confirm", template.templateName());
    assertTrue(template.templateData().contains(subscriptionSaved.getUserConfirmationToken()));
    assertTrue(template.templateData().contains(subscriptionDetails.email()));
    assertEquals("test@email.local", request.fromEmailAddress());
    assertEquals(1, request.replyToAddresses().size());
    assertEquals("test@email.local", request.replyToAddresses().get(0));
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
  }

  @Test
  void updateSubscriptionShouldRespondWithInternalErrorOnAmazonConnectionError() throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        subscriptionDetails.token(),
        UUID.randomUUID().toString());
    givenRenderedEmailTemplateError();

    assertEquals(1, subscriptionRepository.count());
    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isInternalServerError());

    verifyNoInteractions(storageClient);
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

    verifyNoInteractions(emailClient);
  }

  @Test
  void updateSubscriptionShouldRespondWithNoContentAndSendEmailOnValidUserToken() throws Exception {
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

    verify(emailClient, times(2)).sendEmail(captor.capture());
    var requests = captor.getAllValues();

    var firstRequest = requests.get(0);
    assertEquals(
            subscriptionDetails.email(), firstRequest.destination().toAddresses().get(0));
    var firstTemplate = firstRequest.content().template();
    assertEquals("please-wait", firstTemplate.templateName());
    assertTrue(firstTemplate.templateData().contains(subscriptionDetails.email()));
    assertEquals("test@email.local", firstRequest.fromEmailAddress());
    assertEquals(1, firstRequest.replyToAddresses().size());
    assertEquals("test@email.local", firstRequest.replyToAddresses().get(0));

    var secondRequest = requests.get(1);
    assertEquals("test@email.local", secondRequest.destination().toAddresses().get(0));
    var secondTemplate = secondRequest.content().template();
    assertEquals("please-approve", secondTemplate.templateName());
    assertTrue(
            secondTemplate.templateData().contains(savedSubscription.getOwnerConfirmationToken()));
    assertTrue(secondTemplate.templateData().contains(subscriptionDetails.email()));
    assertEquals("test@email.local", secondRequest.fromEmailAddress());
    assertEquals(1, secondRequest.replyToAddresses().size());
    assertEquals("test@email.local", secondRequest.replyToAddresses().get(0));
  }

  @Test
  void updateSubscriptionShouldRespondWithNoContentAndSendEmailOnValidOwnerToken()
      throws Exception {
    givenFullSubscriptionDetails();
    givenAnExistingSubscription(
        subscriptionDetails.email(),
        UUID.randomUUID().toString(),
        subscriptionDetails.token(),
        UUID.randomUUID().toString());
    givenRenderedEmailTemplateResult();
    givenGetEmailTemplateResult();
    givenListObjectsResult();
    givenGetObjectsResult();

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

    verify(emailClient).sendEmail(captor.capture());
    var request = captor.getValue();
    assertEquals(subscriptionDetails.email(), request.destination().toAddresses().get(0));
    var rawMessage = request.content().raw();
    var content = StandardCharsets.UTF_8.decode(rawMessage.data().asByteBuffer()).toString();
    assertTrue(content.contains("Content1"));
    assertTrue(content.contains("Content2"));
    assertEquals(
        objectsResult.getObjectSummaries().size(),
        StringUtils.countOccurrencesOf(content, "application/pdf"));
    assertEquals("test@email.local", request.fromEmailAddress());
    assertEquals(1, request.replyToAddresses().size());
    assertEquals("test@email.local", request.replyToAddresses().get(0));
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
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
    verifyNoInteractions(emailClient);
  }

  @Test
  void methodsShouldResultInCrudFlow() throws Exception {
    givenSubscriptionDetailsWithoutToken();
    givenRenderedEmailTemplateResult();
    givenGetEmailTemplateResult();
    givenListObjectsResult();
    givenGetObjectsResult();

    assertEquals(0, subscriptionRepository.count());
    mockMvc
        .perform(
            post("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isCreated());

    var subscriptionSaved =
        subscriptionRepository
            .findByEmailAndNumTokenErrorsLessThan(subscriptionDetails.email(), 1)
            .orElseThrow();
    subscriptionDetails =
        new SubscriptionDetails(
            subscriptionSaved.getEmail(), subscriptionSaved.getUserConfirmationToken());
    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    subscriptionDetails =
        new SubscriptionDetails(
            subscriptionSaved.getEmail(), subscriptionSaved.getOwnerConfirmationToken());
    mockMvc
        .perform(
            put("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());

    assertEquals(1, subscriptionRepository.count());
    subscriptionDetails =
        new SubscriptionDetails(
            subscriptionSaved.getEmail(), subscriptionSaved.getUserUnsubscribeToken());
    mockMvc
        .perform(
            delete("/v1/subscription")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(subscriptionDetails)))
        .andExpect(status().isNoContent());
    assertEquals(0, subscriptionRepository.count());
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

  private void givenRenderedEmailTemplateResult() {
    var testRenderEmailTemplateResult =
            TestRenderEmailTemplateResponse.builder()
                    .renderedTemplate(
                """
                            Subject: A Subject
                            MIME-Version: 1.0
                            Content-Type: multipart/alternative;\s
                            \tboundary="----=_Part_11286_1224453801.1698335693925"

                            ------=_Part_11286_1224453801.1698335693925
                            Content-Type: text/plain; charset=UTF-8
                            Content-Transfer-Encoding: quoted-printable

                            Content1

                            ------=_Part_11286_1224453801.1698335693925
                            Content-Type: text/html; charset=UTF-8
                            Content-Transfer-Encoding: quoted-printable

                            Content2

                            ------=_Part_11286_1224453801.1698335693925--
                        """)
                    .build();

    when(emailClient.testRenderEmailTemplate(any(TestRenderEmailTemplateRequest.class)))
        .thenReturn(testRenderEmailTemplateResult);
  }

  private void givenRenderedEmailTemplateError() {
    when(emailClient.testRenderEmailTemplate(any(TestRenderEmailTemplateRequest.class)))
        .thenThrow(new AmazonServiceException("error"));
  }

  private void givenGetEmailTemplateResult() {
    var templateContent = EmailTemplateContent.builder().subject(FAKER.internet().emailSubject()).build();
    var getEmailTemplateResult = GetEmailTemplateResponse.builder().templateContent(templateContent).build();

    when(emailClient.getEmailTemplate(any(GetEmailTemplateRequest.class)))
        .thenReturn(getEmailTemplateResult);
  }

  private void givenListObjectsResult() {
    objectsResult = Instancio.create(ListObjectsV2Result.class);

    when(storageClient.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(objectsResult);
  }

  private void givenGetObjectsResult() throws FileNotFoundException {
    var object = new S3Object();
    var file = ResourceUtils.getFile("classpath:static/file.pdf");
    object.setObjectContent(new FileInputStream(file));
    var metadata = new ObjectMetadata();
    metadata.setContentType("application/pdf");
    object.setObjectMetadata(metadata);

    when(storageClient.getObject(anyString(), anyString())).thenReturn(object);
  }
}
