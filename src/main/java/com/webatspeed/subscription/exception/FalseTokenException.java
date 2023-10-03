package com.webatspeed.subscription.exception;

public class FalseTokenException extends RuntimeException {

  public FalseTokenException() {
    super("Token is false");
  }
}
