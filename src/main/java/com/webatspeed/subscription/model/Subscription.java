package com.webatspeed.subscription.model;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Subscription {

  @Id private String id;

  private String email;
}
