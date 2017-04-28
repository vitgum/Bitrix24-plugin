package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.model;

/**
 * author: Artem Voronov
 */
public enum BitrixRequestType {
  CREATE_BOT("imbot.register"),
  DELETE_BOT("imbot.unregister"),
  SEND_MESSAGE_TO_CHAT("imbot.message.add"),
  ADD_BOT_COMMAND("imbot.command.register");

  private final String method;

  BitrixRequestType(String method) {
    this.method = method;
  }

  public String getMethod() {
    return method;
  }
}
