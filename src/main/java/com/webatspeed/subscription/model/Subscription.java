package com.webatspeed.subscription.model;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Subscription {

  @Id private String id;

  @Indexed(unique = true)
  private String email;

  @CreatedDate @EqualsAndHashCode.Exclude private Instant createdAt;

  @LastModifiedDate @EqualsAndHashCode.Exclude private Instant modifiedAt;

  @Version @EqualsAndHashCode.Exclude private Integer version;
}
