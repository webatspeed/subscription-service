package com.webatspeed.subscription.config;

import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2ClientBuilder;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties("cloud.aws.credentials")
public class AwsSesConfiguration {

  @NotBlank private String accessKey;

  @NotBlank private String secretKey;

  @Bean
  AmazonSimpleEmailServiceV2 amazonSimpleEmailServiceV2Client() {
    return AmazonSimpleEmailServiceV2ClientBuilder.standard().build();
  }
}
