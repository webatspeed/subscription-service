package com.webatspeed.subscription;

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

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> createSubscription(@RequestBody @Valid final SubscriptionDetails details) {
    log.info(details.toString());

    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
