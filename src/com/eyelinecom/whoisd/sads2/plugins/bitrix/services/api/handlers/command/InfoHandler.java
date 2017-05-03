package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
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

  public InfoHandler(ChatController chatController, OperatorController operatorController, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationController applicationController, QueueController queueController) {
    super(chatController, operatorController, messageDeliveryProvider, resourceBundleController);
    this.applicationController = applicationController;
    this.queueController = queueController;
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
    final String appLang = application.getLanguage();

    if (!counters.hasAwaitingUsers()) {
      messageDeliveryProvider.sendMessageToChat(application, dialogId, getLocalizedMessage(appLang, "no.users"));
      return;
    }
    String notification = getLocalizedMessage(appLang,"awaiting.users", counters.getAwaitingUsersCount() + "") + "\n" +
      getLocalizedMessage(appLang,"processing.users", counters.getProcessingUsersCount() + "");
    messageDeliveryProvider.sendMessageToChat(application, dialogId, notification);
  }
}
