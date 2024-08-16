package com.webatspeed.subscription.controller;

import com.webatspeed.subscription.exception.EmailSendException;
import com.webatspeed.subscription.exception.FalseTokenException;
import com.webatspeed.subscription.exception.UserAlreadyExistsException;
import com.webatspeed.subscription.exception.UserUnknownOrLockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

@RestControllerAdvice
public class SubscriptionExceptionAdvice {

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(FalseTokenException.class)
  public ProblemDetail handleFalseTokenException(final FalseTokenException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(UserUnknownOrLockedException.class)
  public ProblemDetail handleUserUnknownOrLockedException(
      final UserUnknownOrLockedException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ProblemDetail handleAlreadyExistsException(final UserAlreadyExistsException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(EmailSendException.class)
  public ProblemDetail handleEmailSendException(final EmailSendException exception) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }

  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(AwsServiceException.class)
  public ProblemDetail handleAwsServiceException(final AwsServiceException exception) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
  }
}
