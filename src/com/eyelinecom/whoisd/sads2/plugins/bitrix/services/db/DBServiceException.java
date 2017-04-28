package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db;

public class DBServiceException extends Exception
{
  public DBServiceException() {
  }

  public DBServiceException(String message) {
    super(message);
  }

  public DBServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public DBServiceException(Throwable cause) {
    super(cause);
  }
}
