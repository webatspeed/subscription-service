package com.webatspeed.subscription;

import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.simpleemailv2.model.GetEmailTemplateRequest;
import com.amazonaws.services.simpleemailv2.model.RawMessage;
import com.amazonaws.services.simpleemailv2.model.Template;
import com.amazonaws.services.simpleemailv2.model.TestRenderEmailTemplateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.model.Subscription;
import com.webatspeed.subscription.service.TemplateName;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionMapper {

  private final ObjectMapper objectMapper;

  public Subscription subscriptionOf(SubscriptionDetails subscriptionDetails) {
    var subscription = new Subscription();
    subscription.setEmail(subscriptionDetails.email());

    return subscription;
  }

  @SneakyThrows
  public String jsonOf(Map<String, String> args) {
    return objectMapper.writeValueAsString(args);
  }

  public Template templateOf(String username, String token, TemplateName templateName) {
    var args =
        Map.of(
            "token", token,
            "username", username);
    var templateData = jsonOf(args);

    return new Template().withTemplateName(templateName.toString()).withTemplateData(templateData);
  }

  public Template templateOf(String username, TemplateName templateName) {
    var args = Map.of("username", username);

    return new Template().withTemplateName(templateName.toString()).withTemplateData(jsonOf(args));
  }

  public TestRenderEmailTemplateRequest renderRequestOf(
      String username, String token, TemplateName templateName) {
    var args =
        Map.of(
            "token", token,
            "username", username);

    return new TestRenderEmailTemplateRequest()
        .withTemplateName(templateName.toString())
        .withTemplateData(jsonOf(args));
  }

  public GetEmailTemplateRequest templateRequestOf(TemplateName templateName) {
    return new GetEmailTemplateRequest().withTemplateName(templateName.toString());
  }

  public MimeBodyPart bodyPartOf(String renderedTemplate, Session session)
      throws IOException, MessagingException {
    var textAndHtmlPart = new MimeBodyPart();
    var templateStream = new ByteArrayInputStream(renderedTemplate.getBytes());
    var textAndHtml = (MimeMultipart) new MimeMessage(session, templateStream).getContent();
    textAndHtmlPart.setContent(textAndHtml);

    return textAndHtmlPart;
  }

  public MimeBodyPart bodyPartOf(S3Object object, S3ObjectSummary summary)
      throws IOException, MessagingException {
    var contentType = object.getObjectMetadata().getContentType();
    var objContent = object.getObjectContent();
    var dataSource = new ByteArrayDataSource(objContent, contentType);

    var attachmentPart = new MimeBodyPart();
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(summary.getKey());

    return attachmentPart;
  }

  public RawMessage rawMessageOf(
      String from, String to, String subject, Session session, MimeMultipart content)
      throws MessagingException, IOException {
    var message = new MimeMessage(session);
    message.setSubject(subject);
    message.setFrom(from);
    message.setRecipients(Message.RecipientType.TO, to);
    message.setContent(content);
    var outputStream = new ByteArrayOutputStream();
    message.writeTo(outputStream);

    var rawData = ByteBuffer.wrap(outputStream.toByteArray());

    return new RawMessage().withData(rawData);
  }

  public ListObjectsV2Request listRequestOf(String bucketName) {
    return new ListObjectsV2Request().withBucketName(bucketName);
  }
}
