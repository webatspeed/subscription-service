package com.webatspeed.subscription;

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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.sesv2.model.GetEmailTemplateRequest;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.Template;
import software.amazon.awssdk.services.sesv2.model.TestRenderEmailTemplateRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

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

    return Template.builder().templateName(templateName.toString()).templateData(templateData).build();
  }

  public Template templateOf(String username, TemplateName templateName) {
    var args = Map.of("username", username);

    return Template.builder().templateName(templateName.toString()).templateData(jsonOf(args)).build();
  }

  public TestRenderEmailTemplateRequest renderRequestOf(
      String username, String token, TemplateName templateName) {
    var args =
        Map.of(
            "token", token,
            "username", username);

    return TestRenderEmailTemplateRequest.builder()
            .templateName(templateName.toString())
            .templateData(jsonOf(args))
            .build();
  }

  public GetEmailTemplateRequest templateRequestOf(TemplateName templateName) {
    return GetEmailTemplateRequest.builder().templateName(templateName.toString()).build();
  }

  public MimeBodyPart bodyPartOf(String renderedTemplate, Session session)
      throws IOException, MessagingException {
    var textAndHtmlPart = new MimeBodyPart();
    var templateStream = new ByteArrayInputStream(renderedTemplate.getBytes());
    var textAndHtml = (MimeMultipart) new MimeMessage(session, templateStream).getContent();
    textAndHtmlPart.setContent(textAndHtml);

    return textAndHtmlPart;
  }

  public MimeBodyPart bodyPartOf(ResponseBytes<GetObjectResponse> objContent, String key) throws MessagingException {
    var dataSource = new ByteArrayDataSource(objContent.asByteArray(), objContent.response().contentType());

    var attachmentPart = new MimeBodyPart();
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(key);

    return attachmentPart;
  }

  public GetObjectRequest getRequest(String bucket, String key) {
    return GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
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

    var rawData = SdkBytes.fromByteArray(outputStream.toByteArray());

    return RawMessage.builder().data(rawData).build();
  }

  public ListObjectsV2Request listRequestOf(String bucketName) {
    return ListObjectsV2Request.builder().bucket(bucketName).build();
  }
}
