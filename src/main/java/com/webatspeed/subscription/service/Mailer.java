package com.webatspeed.subscription.service;

import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.config.MailConfiguration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mailer {

  private final AmazonSimpleEmailServiceV2 client;

  private final MailConfiguration mailConfiguration;

  private final ObjectMapper mapper;

  public void emailPleaseConfirm(String to, String token) {
    var args =
        Map.of(
            "token", token,
            "username", to);
    var templateData = marshall(args);
    var template = new Template().withTemplateName("please-confirm").withTemplateData(templateData);

    email(mailConfiguration.getDefaultSender(), to, template);
  }

  public void emailPleaseWait(String to) {
    var args = Map.of("username", to);
    var templateData = marshall(args);
    var template = new Template().withTemplateName("please-wait").withTemplateData(templateData);

    email(mailConfiguration.getDefaultSender(), to, template);
  }

  public void emailPleaseApprove(String username, String token) {
    var args =
        Map.of(
            "token", token,
            "username", username);
    var templateData = marshall(args);
    var template = new Template().withTemplateName("please-approve").withTemplateData(templateData);

    email(mailConfiguration.getDefaultSender(), mailConfiguration.getDefaultSender(), template);
  }

  public void emailFirstCv(String to, String token) {
    var args =
        Map.of(
            "token", token,
            "username", to);
    var templateData = marshall(args);
    var template = new Template().withTemplateName("first-cv").withTemplateData(templateData);

    email(mailConfiguration.getDefaultSender(), to, template);
  }

  @SneakyThrows
  private String marshall(Map<String, String> args) {
    return mapper.writeValueAsString(args);
  }

  private void email(String from, String to, Template template) {
    var content = new EmailContent().withTemplate(template);
    var destination = new Destination().withToAddresses(to);
    var request =
        new SendEmailRequest()
            .withDestination(destination)
            .withContent(content)
            .withFromEmailAddress(from);

    try {
      client.sendEmail(request);
    } catch (Exception e) {
      log.error("The email was not sent. Error message: {}", e.getMessage());
    }
  }
}
