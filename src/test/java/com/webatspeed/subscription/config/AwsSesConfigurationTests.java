package com.webatspeed.subscription.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.InstancioApi;
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
    givenNoRegion();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void regionShouldNotBeEmpty() {
    givenEmptyRegion();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void shouldBeValid() {
    givenValidConfig();

    var violations = validator.validate(config);

    assertTrue(violations.isEmpty());
  }

  private void givenNoCredentials() {
    config = validConfig().set(Select.field("credentials"), null).create();
  }

  private void givenNoRegion() {
    config = validConfig().set(Select.field("region"), null).create();
  }

  private void givenEmptyRegion() {
    config = validConfig().set(Select.field("region"), Map.of()).create();
  }

  private void givenValidConfig() {
    config = validConfig().create();
  }

  private static InstancioApi<AwsSesConfiguration> validConfig() {
    return Instancio.of(AwsSesConfiguration.class)
        .set(Select.field("region"), Map.of("region", FAKER.aws().region()));
  }
}
