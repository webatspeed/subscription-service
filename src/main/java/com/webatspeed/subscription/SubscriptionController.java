package com.webatspeed.subscription;

import static org.springframework.http.HttpStatus.*;

import com.webatspeed.subscription.dto.SubscriptionDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RequestMapping("/v1/subscription")
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

  private final SubscriptionRepository subscriptionRepository;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createSubscription(
      @RequestBody @Valid final SubscriptionDetails details) {
    HttpStatus status;
    if (subscriptionRepository.existsByEmail(details.email())) {
      status = CONFLICT;
    } else {
      status = NOT_IMPLEMENTED;
    }

    return ResponseEntity.status(status).build();
  }
}
