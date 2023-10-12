package com.webatspeed.subscription.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import net.datafaker.Faker;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CorsConfigurationTests {

  private static final Faker FAKER = new Faker();

  private Validator validator;

  private CorsConfiguration config;

  @BeforeEach
  public void setUp() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      validator = validatorFactory.getValidator();
    }
    config = null;
  }

  @Test
  public void originUrlShouldNotBeNull() {
    givenCorsConfigurationWithoutOriginUrl();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void originUrlShouldNotBeEmpty() {
    givenCorsConfigurationWithEmptyUrl();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void originUrlShouldNotBeNonUrl() {
    givenCorsConfigurationWithNonUrlOriginUrl();

    var violations = validator.validate(config);

    assertFalse(violations.isEmpty());
  }

  @Test
  public void originUrlShouldBeUrl() {
    givenCorsConfigurationWithUrl();

    var violations = validator.validate(config);

    assertTrue(violations.isEmpty());
  }

  private void givenCorsConfigurationWithoutOriginUrl() {
    config = Instancio.of(CorsConfiguration.class).set(Select.field("originUrl"), null).create();
  }

  private void givenCorsConfigurationWithEmptyUrl() {
    config = Instancio.of(CorsConfiguration.class).set(Select.field("originUrl"), "").create();
  }

  private void givenCorsConfigurationWithNonUrlOriginUrl() {
    config = Instancio.create(CorsConfiguration.class);
  }

  private void givenCorsConfigurationWithUrl() {
    config =
        Instancio.of(CorsConfiguration.class)
            .set(Select.field("originUrl"), FAKER.internet().url())
            .create();
  }
}
