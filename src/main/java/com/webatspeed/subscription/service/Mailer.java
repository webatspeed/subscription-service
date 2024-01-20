package com.webatspeed.subscription.service;

import static com.webatspeed.subscription.service.TemplateName.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.model.*;
import com.webatspeed.subscription.SubscriptionMapper;
import com.webatspeed.subscription.config.MailConfiguration;
import com.webatspeed.subscription.exception.EmailSendException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mailer {

  private final AmazonSimpleEmailServiceV2 emailClient;

  private final AmazonS3 storageClient;

  private final MailConfiguration mailConfiguration;

  private final SubscriptionMapper mapper;

  public void emailPleaseConfirm(String to, String token) {
    var template = mapper.templateOf(to, token, PLEASE_CONFIRM);
    email(to, template);
  }

  public void emailPleaseWait(String to) {
    var template = mapper.templateOf(to, PLEASE_WAIT);
    email(to, template);
  }

  public void emailPleaseApprove(String username, String token) {
    var template = mapper.templateOf(username, token, PLEASE_APPROVE);
    email(mailConfiguration.getDefaultSender(), template);
  }

  @RateLimiter(name = "ses")
  public void emailCv(String to, String token) {
    var renderRequest = mapper.renderRequestOf(to, token, FIRST_CV);
    var renderedTemplate = emailClient.testRenderEmailTemplate(renderRequest).getRenderedTemplate();
    var templateRequest = mapper.templateRequestOf(FIRST_CV);
    var subject = emailClient.getEmailTemplate(templateRequest).getTemplateContent().getSubject();

    try {
      email(to, subject, renderedTemplate);
    } catch (MessagingException | IOException e) {
      throw new EmailSendException(e);
    }
  }

  private void email(String to, Template template) {
    var content = new EmailContent().withTemplate(template);
    var from = mailConfiguration.getDefaultSender();
    emailContent(from, to, content);
  }

  private void email(String to, String subject, String renderedTemplate)
      throws MessagingException, IOException {
    var session = Session.getDefaultInstance(new Properties());

    var content = new MimeMultipart();
    var textAndHtmlPart = mapper.bodyPartOf(renderedTemplate, session);
    content.addBodyPart(textAndHtmlPart);

    var listRequest = mapper.listRequestOf(mailConfiguration.getAttachmentBucket());
    var listResult = storageClient.listObjectsV2(listRequest);
    for (var summary : listResult.getObjectSummaries()) {
      var object = storageClient.getObject(summary.getBucketName(), summary.getKey());
      var attachmentPart = mapper.bodyPartOf(object, summary);
      content.addBodyPart(attachmentPart);
    }

    var from = mailConfiguration.getDefaultSender();
    var rawMessage = mapper.rawMessageOf(from, to, subject, session, content);
    var emailContent = new EmailContent().withRaw(rawMessage);

    emailContent(from, to, emailContent);
  }

  private void emailContent(String from, String to, EmailContent content) {
    var destination = new Destination().withToAddresses(to);
    var request =
        new SendEmailRequest()
            .withDestination(destination)
            .withContent(content)
            .withReplyToAddresses(from)
            .withFromEmailAddress(from);

    try {
      emailClient.sendEmail(request);
    } catch (Exception e) {
      throw new EmailSendException(e);
    }
  }
}
