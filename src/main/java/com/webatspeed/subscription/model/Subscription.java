package com.webatspeed.subscription.model;

import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Subscription {

  @Id private String id;

  @Indexed(unique = true)
  private String email;

  private String userConfirmationToken = UUID.randomUUID().toString();

  private String ownerConfirmationToken = UUID.randomUUID().toString();

  private String userUnsubscribeToken = UUID.randomUUID().toString();

  private int numTokenErrors = 0;

  private Boolean confirmedByUser = false;

  private Boolean confirmedByOwner = false;

  @CreatedDate @EqualsAndHashCode.Exclude private Instant createdAt;

  @LastModifiedDate @EqualsAndHashCode.Exclude private Instant modifiedAt;

  @Version @EqualsAndHashCode.Exclude private Integer version;

  public void incNumTokenErrors() {
    numTokenErrors++;
  }

  public void resetNumTokenErrors() {
    numTokenErrors = 0;
  }
}
