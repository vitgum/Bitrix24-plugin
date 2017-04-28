package com.eyelinecom.whoisd.sads2.plugins.bitrix.services;

/**
 * Ошибка инициализации сервисов
 */
public class ServicesException extends Exception {

  ServicesException(String message, Throwable cause) {
    super(message, cause);
  }

}
