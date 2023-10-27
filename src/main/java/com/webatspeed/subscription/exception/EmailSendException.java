package com.webatspeed.subscription.exception;

public class EmailSendException extends RuntimeException {

  public EmailSendException(Exception e) {
    super("Email could not be sent", e);
  }
}
