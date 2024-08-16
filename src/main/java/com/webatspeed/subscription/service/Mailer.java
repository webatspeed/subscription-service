package com.webatspeed.subscription.service;

import com.amazonaws.services.s3.AmazonS3;
import com.webatspeed.subscription.SubscriptionMapper;
import com.webatspeed.subscription.config.MailConfiguration;
import com.webatspeed.subscription.exception.EmailSendException;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

import java.io.IOException;
import java.util.Properties;

import static com.webatspeed.subscription.service.TemplateName.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mailer {

  private final SesV2Client emailClient;

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
  public void emailCv(String to, String token, boolean isFirst) {
    var templateName = isFirst ? FIRST_CV : UPDATED_CV;
    var renderRequest = mapper.renderRequestOf(to, token, templateName);
    var renderedTemplate = emailClient.testRenderEmailTemplate(renderRequest).renderedTemplate();
    var templateRequest = mapper.templateRequestOf(templateName);
    var subject = emailClient.getEmailTemplate(templateRequest).templateContent().subject();

    try {
      email(to, subject, renderedTemplate);
    } catch (MessagingException | IOException e) {
      throw new EmailSendException(e);
    }
  }

  private void email(String to, Template template) {
    var content = EmailContent.builder().template(template).build();
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
    var emailContent = EmailContent.builder().raw(rawMessage).build();

    emailContent(from, to, emailContent);
  }

  private void emailContent(String from, String to, EmailContent content) {
    var destination = Destination.builder().toAddresses(to).build();
    var request =
            SendEmailRequest.builder()
                    .destination(destination)
                    .content(content)
                    .replyToAddresses(from)
                    .fromEmailAddress(from)
                    .build();

    try {
      emailClient.sendEmail(request);
    } catch (SesV2Exception e) {
      throw new EmailSendException(e);
    }
  }
}
