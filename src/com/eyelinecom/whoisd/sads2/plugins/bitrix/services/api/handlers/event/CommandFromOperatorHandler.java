package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command.CommandProcessor;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.model.Command;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class CommandFromOperatorHandler implements EventHandler {

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    Command command = ParamsExtractor.getCommand(parameters);
    CommandProcessor.process(command, parameters);
  }
}
