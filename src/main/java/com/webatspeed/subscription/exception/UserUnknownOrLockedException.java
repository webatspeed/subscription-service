package com.webatspeed.subscription.exception;

public class UserUnknownOrLockedException extends RuntimeException {

  public UserUnknownOrLockedException() {
    super("User unknown or locked");
  }
}
