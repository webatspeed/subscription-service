package com.webatspeed.subscription.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
public class AwsSesCredentialsConfiguration {

  @NotBlank private String accessKey;

  @NotBlank private String secretKey;
}
