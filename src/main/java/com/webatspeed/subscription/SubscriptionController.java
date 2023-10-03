package com.webatspeed.subscription;

import static org.springframework.http.HttpStatus.*;

import com.webatspeed.subscription.dto.SubscriptionDetails;
import com.webatspeed.subscription.exception.FalseTokenException;
import com.webatspeed.subscription.exception.UserUnknownOrLockedException;
import com.webatspeed.subscription.service.Subscriber;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RequestMapping("/v1/subscription")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

  private final SubscriptionRepository repository;

  private final SubscriptionMapper mapper;

  private final Subscriber subscriber;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createSubscription(
      @RequestBody @Valid final SubscriptionDetails details) {
    HttpStatus status;

    if (repository.existsByEmail(details.email())) {
      status = CONFLICT;
    } else {
      var subscription = mapper.subscriptionOf(details);
      repository.save(subscription);
      status = NO_CONTENT;
    }

    return ResponseEntity.status(status).build();
  }

  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> updateSubscription(
      @RequestBody @Valid final SubscriptionDetails details) {
    if (!StringUtils.hasText(details.token())) {
      throw new FalseTokenException();
    }

    repository
        .findByEmailAndNumTokenErrorsLessThan(details.email(), 3)
        .map(s -> subscriber.applyToken(s, details.token()))
        .orElseThrow(UserUnknownOrLockedException::new);

    return ResponseEntity.status(NO_CONTENT).build();
  }
}
