package com.webatspeed.subscription.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.util.Map;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties("cloud.aws")
public class AwsSesConfiguration {

  @NotNull private AwsSesCredentialsConfiguration credentials;

  @NotEmpty private Map<String, String> region;

  @Bean
  SesV2Client sesV2Client() {
    return SesV2Client.builder().region(region()).build();
  }

  @Bean
  S3Client amazonS3Client() {
    return S3Client.builder().region(region()).build();
  }

  private Region region() {
    return Region.of(region.get("static"));
  }
}
