package com.webatspeed.subscription.service;

import com.webatspeed.subscription.SubscriptionRepository;
import com.webatspeed.subscription.model.Subscription;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.util.ResourceUtils;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
public class SubscriberTests {

  private static final Faker FAKER = new Faker();

  @Autowired private Subscriber subscriber;

  @Autowired private SubscriptionRepository subscriptionRepository;

  @MockBean
  private SesV2Client emailClient;

  @MockBean
  private S3Client storageClient;

  private ListObjectsV2Response objectsResponse;

  @AfterEach
  void cleanUp() {
    subscriptionRepository.deleteAll();
    objectsResponse = null;
    await().until(() -> !subscriber.isDistributing());
  }

  @Test
  void distributeShouldLock() throws IOException {
    givenSavedSubscriptions(1, true);
    givenDistributing();
    givenGetEmailTemplateResult();
    givenListObjectsResult();
    givenGetObjectsResponse();
    givenRenderedEmailTemplateResult();

    subscriber.distribute();

    await().until(subscriber::isDistributing);
  }

  @Test
  void distributeShouldNotEmailIfNoSubscriptionExist() {
    subscriber.distribute();

    verify(emailClient, never()).sendEmail(any(SendEmailRequest.class));
  }

  @Test
  void distributeShouldNotEmailIfNoSubscriptionConfirmedByOwner() {
    givenSavedSubscriptions(10, false);

    subscriber.distribute();

    verify(emailClient, never()).sendEmail(any(SendEmailRequest.class));
  }

  @Test
  void distributeShouldEmailRateLimitedIfSubscriptionConfirmedByOwner() throws IOException {
    var numberOfSubscriptions = FAKER.number().numberBetween(10, 19);
    givenSavedSubscriptions(numberOfSubscriptions, true);
    givenGetEmailTemplateResult();
    givenListObjectsResult();
    givenGetObjectsResponse();
    givenRenderedEmailTemplateResult();

    var tic = Instant.now();
    subscriber.distribute();

    verify(emailClient, timeout(10000).times(numberOfSubscriptions))
        .sendEmail(any(SendEmailRequest.class));
    var toc = Instant.now();
    var fullPages = (int) Math.floor((double) numberOfSubscriptions / 3);
    var minDuration = Duration.ofSeconds(fullPages);
    var duration = Duration.between(tic, toc);
    assertTrue(duration.minus(minDuration).toMillis() >= 0);
  }

  private void givenSavedSubscriptions(int number, boolean confirmedByOwner) {
    for (int i = 0; i < number; i++) {
      var subscription =
          Instancio.of(Subscription.class)
              .set(Select.field("id"), null)
              .set(Select.field("email"), FAKER.internet().emailAddress())
              .set(Select.field("numTokenErrors"), 0)
              .set(Select.field("confirmedByOwner"), confirmedByOwner)
              .set(Select.field("createdAt"), null)
              .set(Select.field("modifiedAt"), null)
              .set(Select.field("version"), null)
              .create();
      subscriptionRepository.save(subscription);
    }
  }

  private void givenDistributing() {
    when(emailClient.sendEmail(any(SendEmailRequest.class)))
        .thenAnswer(new AnswersWithDelay(500, new Returns(null)));
  }

  private void givenGetEmailTemplateResult() {
    var templateContent = EmailTemplateContent.builder().subject(FAKER.internet().emailSubject()).build();
    var getEmailTemplateResult = GetEmailTemplateResponse.builder().templateContent(templateContent).build();

    when(emailClient.getEmailTemplate(any(GetEmailTemplateRequest.class)))
        .thenReturn(getEmailTemplateResult);
  }

  private void givenListObjectsResult() {
    objectsResponse = Instancio.create(ListObjectsV2Response.class);

    when(storageClient.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(objectsResponse);
  }

  private void givenGetObjectsResponse() throws IOException {
    var file = ResourceUtils.getFile("classpath:static/file.pdf");
    var bytes = Files.readAllBytes(file.toPath());
    var response = GetObjectResponse.builder()
            .contentType(MediaType.APPLICATION_PDF_VALUE)
            .build();
    var objectBytes = ResponseBytes.fromByteArray(response, bytes);

    when(storageClient.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(objectBytes);
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
}
