package io.netifi.sdk.auth.exception;

/** */
public class AuthenticationDeniedException extends RuntimeException {
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
