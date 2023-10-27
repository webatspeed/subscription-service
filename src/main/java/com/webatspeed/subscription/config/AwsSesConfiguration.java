package com.webatspeed.subscription.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2;
import com.amazonaws.services.simpleemailv2.AmazonSimpleEmailServiceV2ClientBuilder;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
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
@ConfigurationProperties("cloud.aws")
public class AwsSesConfiguration {

  @NotNull private AwsSesCredentialsConfiguration credentials;

  @NotEmpty private Map<String, String> region;

  @Bean
  AmazonSimpleEmailServiceV2 amazonSimpleEmailServiceV2Client() {
    return AmazonSimpleEmailServiceV2ClientBuilder.standard().withRegion(region()).build();
  }

  @Bean
  AmazonS3 amazonS3Client() {
    return AmazonS3Client.builder().withRegion(region()).build();
  }

  private String region() {
    return region.get("static");
  }
}
