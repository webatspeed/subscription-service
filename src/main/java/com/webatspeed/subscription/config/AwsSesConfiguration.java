package com.webatspeed.subscription.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.regions.Region;
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
    return SesV2Client.builder().region(Region.of(region())).build();
  }

  @Bean
  AmazonS3 amazonS3Client() {
    return AmazonS3Client.builder().withRegion(region()).build();
  }

  private String region() {
    return region.get("static");
  }
}
