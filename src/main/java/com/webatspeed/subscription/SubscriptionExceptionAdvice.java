package com.webatspeed.subscription;

import com.webatspeed.subscription.exception.FalseTokenException;
import com.webatspeed.subscription.exception.UserAlreadyExistsException;
import com.webatspeed.subscription.exception.UserUnknownOrLockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
