package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.command;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.model.Command;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * author: Artem Voronov
 */
public class CommandProcessor {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private static final Map<Command, CommandHandler> handlers = new HashMap<>();

  public static void addHandler(Command command, CommandHandler handler) {
    handlers.put(command, handler);
  }

  public static void process(Command command, Map<String, String[]> parameters) {
    try {
      CommandHandler handler = handlers.get(command);
      if (handler == null)
        throw new IllegalArgumentException("Missed handler for command: " + command);

      handler.handle(parameters);

    } catch (Exception ex) {
      String error = "Command processing error: " + command;
      logger.error(error, ex);
    }
  }
}
