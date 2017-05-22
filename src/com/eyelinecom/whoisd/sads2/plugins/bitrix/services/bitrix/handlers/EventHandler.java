package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * author: Artem Voronov
 */
public interface EventHandler {

  default void handle(Map<String, String[]> parameters, HttpServletResponse response) {
    sendResponse(parameters, response);
    processEvent(parameters);
  }

  default void sendResponse(Map<String, String[]> parameters, HttpServletResponse response) {
    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  void processEvent(Map<String, String[]> parameters);
}
