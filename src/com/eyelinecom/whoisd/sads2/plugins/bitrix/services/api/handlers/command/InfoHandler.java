package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.UserCounters;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class InfoHandler extends CommonEventHandler implements CommandHandler {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private final ApplicationController applicationController;
  private final QueueController queueController;

  public InfoHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.applicationController = services.getApplicationController();
    this.queueController = services.getQueueController();
  }

  @Override
  public void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    if (logger.isDebugEnabled())
      logger.debug("Info command. Domain: " + domain);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    processIdChatNotJoined(parameters, application);
    final String dialogId = ParamsExtractor.getDialogId(parameters);

    UserCounters counters = queueController.getUserCounters(application);

    if (!counters.hasAwaitingUsers()) {
      messageDeliveryService.sendMessageToChat(application, dialogId, getLocalizedMessage(parameters, "no.users"));
      return;
    }

    String notification = getLocalizedMessage(parameters,"awaiting.users", counters.getAwaitingUsersCount() + "") + "\n" +
      getLocalizedMessage(parameters,"processing.users", counters.getProcessingUsersCount() + "");
    messageDeliveryService.sendMessageToChat(application, dialogId, notification);
  }
}
