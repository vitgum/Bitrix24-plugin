package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.model.Event;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.PrettyPrintUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * author: Artem Voronov
 */
public class EventProcessor {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private static final Map<Event, EventHandler> handlers = new HashMap<>();

  public static void addHandler(Event event, EventHandler handler) {
    handlers.put(event, handler);
  }

  public static void process(Event event, Map<String, String[]> parameters, HttpServletResponse response) {
    try {
      EventHandler handler = handlers.get(event);
      if (handler == null)
        throw new IllegalArgumentException("Missed handler for event: " + event);

      handler.handle(parameters, response);

    } catch (Exception ex) {
      String errorMsg = "Event processing error: " + event + ". Params: \n" + PrettyPrintUtils.toPrettyMap(parameters) + "\n";
      logger.error(errorMsg, ex);
      response.setCharacterEncoding("UTF-8");
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      try {
        try (PrintWriter out = response.getWriter()) {
          out.write(errorMsg);
        }
      } catch (Exception e) {
        logger.error(e.getMessage(), ex);
      }
    }
  }
}
