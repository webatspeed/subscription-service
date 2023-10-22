package com.webatspeed.subscription.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AwsSesCredentialsConfigurationTests {

  private Validator validator;

  private AwsSesCredentialsConfiguration config;

  @BeforeEach
  public void setUp() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      validator = validatorFactory.getValidator();
    }
    config = null;
  }

  @Test
  public void accessKeyShouldNotBeNull() {
    givenNoAccessKey();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void accessKeyShouldNotBeBlank() {
    givenEmptyAccessKey();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void secretKeyShouldNotBeNull() {
    givenNoSecretKey();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void secretKeyShouldNotBeBlank() {
    givenEmptySecretKey();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void shouldBeValid() {
    givenFullConfig();

    var violations = validator.validate(config);

    assertTrue(violations.isEmpty());
  }

  private void givenNoAccessKey() {
    config =
        Instancio.of(AwsSesCredentialsConfiguration.class)
            .set(Select.field("accessKey"), null)
            .create();
  }

  private void givenEmptyAccessKey() {
    config =
        Instancio.of(AwsSesCredentialsConfiguration.class)
            .set(Select.field("accessKey"), "")
            .create();
  }

  private void givenNoSecretKey() {
    config =
        Instancio.of(AwsSesCredentialsConfiguration.class)
            .set(Select.field("secretKey"), null)
            .create();
  }

  private void givenEmptySecretKey() {
    config =
        Instancio.of(AwsSesCredentialsConfiguration.class)
            .set(Select.field("secretKey"), "")
            .create();
  }

  private void givenFullConfig() {
    config = Instancio.create(AwsSesCredentialsConfiguration.class);
  }
}
