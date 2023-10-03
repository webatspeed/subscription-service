package com.webatspeed.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscriptionDetails(@NotBlank @Email String email, String token) {}
