package com.webatspeed.subscription.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.model.*;
import com.webatspeed.subscription.SubscriptionRepository;
import com.webatspeed.subscription.model.Subscription;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
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

@SpringBootTest
public class SubscriberTests {

  private static final Faker FAKER = new Faker();

  @Autowired private Subscriber subscriber;

  @Autowired private SubscriptionRepository subscriptionRepository;

  @MockBean private AmazonSimpleEmailServiceV2 emailClient;

  @MockBean private AmazonS3 storageClient;

  private ListObjectsV2Result objectsResult;

  @AfterEach
  void cleanUp() {
    subscriptionRepository.deleteAll();
    objectsResult = null;
    await().until(() -> !subscriber.isDistributing());
  }

  @Test
  void distributeShouldLock() throws FileNotFoundException {
    givenSavedSubscriptions(1, true);
    givenDistributing();
    givenGetEmailTemplateResult();
    givenListObjectsResult();
    givenGetObjectsResult();
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
  void distributeShouldEmailRateLimitedIfSubscriptionConfirmedByOwner()
      throws FileNotFoundException {
    var numberOfSubscriptions = FAKER.number().numberBetween(10, 19);
    givenSavedSubscriptions(numberOfSubscriptions, true);
    givenGetEmailTemplateResult();
    givenListObjectsResult();
    givenGetObjectsResult();
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
    var templateContent = new EmailTemplateContent().withSubject(FAKER.internet().emailSubject());
    var getEmailTemplateResult = new GetEmailTemplateResult().withTemplateContent(templateContent);

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
    metadata.setContentType(MediaType.APPLICATION_PDF_VALUE);
    object.setObjectMetadata(metadata);

    when(storageClient.getObject(anyString(), anyString())).thenReturn(object);
  }

  private void givenRenderedEmailTemplateResult() {
    var testRenderEmailTemplateResult =
        new TestRenderEmailTemplateResult()
            .withRenderedTemplate(
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
                                        """);

    when(emailClient.testRenderEmailTemplate(any(TestRenderEmailTemplateRequest.class)))
        .thenReturn(testRenderEmailTemplateResult);
  }
}
