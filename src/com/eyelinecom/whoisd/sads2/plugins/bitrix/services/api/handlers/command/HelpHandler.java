package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class HelpHandler extends CommonEventHandler implements CommandHandler {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private final ApplicationController applicationController;

  public HelpHandler(ChatController chatController, OperatorController operatorController, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationController applicationController) {
    super(chatController, operatorController, messageDeliveryProvider, resourceBundleController);
    this.applicationController = applicationController;
  }

  @Override
  public void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    if (logger.isDebugEnabled())
      logger.debug("Help command. Domain: " + domain);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    processIdChatNotJoined(parameters, application);

    final String dialogId = ParamsExtractor.getDialogId(parameters);
    messageDeliveryProvider.sendMessageToChat(application, dialogId, getLocalizedMessage(application.getLanguage(),"help.text"));
  }
}
