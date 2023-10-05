package com.webatspeed.subscription.exception;

public class UserAlreadyExistsException extends RuntimeException {

  public UserAlreadyExistsException() {
    super("User exists already");
  }
}
