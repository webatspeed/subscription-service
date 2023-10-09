package com.webatspeed.subscription;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.exception.FalseTokenException;
import com.webatspeed.subscription.exception.UserAlreadyExistsException;
import com.webatspeed.subscription.exception.UserUnknownOrLockedException;
import com.webatspeed.subscription.service.Mailer;
import com.webatspeed.subscription.service.Subscriber;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RequestMapping("/v1/subscription")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

  private final SubscriptionRepository repository;

  private final SubscriptionMapper mapper;

  private final Subscriber subscriber;

  private final Mailer mailer;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createSubscription(@RequestBody @Valid final SubscriptionDetails details)
      throws JsonProcessingException {
    if (repository.existsByEmail(details.email())) {
      throw new UserAlreadyExistsException();
    }

    var subscription = mapper.subscriptionOf(details);
    repository.save(subscription);
    mailer.emailSignUpConfirmRequest(
        subscription.getEmail(), subscription.getUserConfirmationToken());

    return ResponseEntity.status(CREATED).build();
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateSubscription(
      @RequestBody @Valid final SubscriptionDetails details) {
    if (!StringUtils.hasText(details.token())) {
      throw new FalseTokenException();
    }

    repository
        .findByEmailAndNumTokenErrorsLessThan(details.email(), 3)
        .map(s -> subscriber.applyUpdateToken(s, details.token()))
        .orElseThrow(UserUnknownOrLockedException::new);

    return ResponseEntity.status(NO_CONTENT).build();
  }

  @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> deleteSubscription(
      @RequestBody @Valid final SubscriptionDetails details) {
    if (!StringUtils.hasText(details.token())) {
      throw new FalseTokenException();
    }

    if (repository.existsByEmail(details.email())) {
      repository
          .findByEmailAndNumTokenErrorsLessThan(details.email(), 3)
          .map(s -> subscriber.applyDeleteToken(s, details.token()))
          .orElseThrow(UserUnknownOrLockedException::new);
    }

    return ResponseEntity.status(NO_CONTENT).build();
  }
}
