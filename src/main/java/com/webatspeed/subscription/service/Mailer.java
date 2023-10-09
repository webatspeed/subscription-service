package com.webatspeed.subscription.service;

import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webatspeed.subscription.config.MailConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Mailer {

  private final AmazonSimpleEmailServiceV2 client;

  private final MailConfiguration mailConfiguration;

  private final ObjectMapper mapper;

  public void emailSignUpConfirmRequest(String to, String token) throws JsonProcessingException {
    var templateData =
        Map.of(
            "token", token,
            "username", to);

    var template =
        new Template()
            .withTemplateName("please-confirm")
            .withTemplateData(mapper.writeValueAsString(templateData));

    email(mailConfiguration.getDefaultSender(), to, template);
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
