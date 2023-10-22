package com.webatspeed.subscription.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwsSesConfigurationTests {

  private static final Faker FAKER = new Faker();
  private Validator validator;

  private AwsSesConfiguration config;

  @BeforeEach
  public void setUp() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      validator = validatorFactory.getValidator();
    }
    config = null;
  }

  @Test
  public void credentialsShouldNotBeNull() {
    givenNoCredentials();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void regionShouldNotBeNull() {
    givenCredentials();
    givenNoRegion();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void regionShouldNotBeEmpty() {
    givenCredentials();
    givenEmptyRegion();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void regionShouldBeEmpty() {
    givenCredentials();
    givenEmptyRegion();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void shouldBeValid() {
    givenCredentials();
    givenRegion();

    var violations = validator.validate(config);

    assertTrue(violations.isEmpty());
  }

  private void givenNoCredentials() {
    config =
        Instancio.of(AwsSesConfiguration.class).set(Select.field("credentials"), null).create();
  }

  private void givenCredentials() {
    config = Instancio.create(AwsSesConfiguration.class);
  }

  private void givenNoRegion() {
    config = Instancio.of(AwsSesConfiguration.class).set(Select.field("region"), null).create();
  }

  private void givenEmptyRegion() {
    config = Instancio.of(AwsSesConfiguration.class).set(Select.field("region"), Map.of()).create();
  }

  private void givenRegion() {
    config =
        Instancio.of(AwsSesConfiguration.class)
            .set(Select.field("region"), Map.of("region", FAKER.aws().region()))
            .create();
  }
}
