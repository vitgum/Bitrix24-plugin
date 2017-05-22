package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO;
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
  private final ApplicationDAO applicationDAO;

  public HelpHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
  }

  @Override
  public void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    if (logger.isDebugEnabled())
      logger.debug("Help command. Domain: " + domain);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    processIfChatNotJoined(parameters, application);

    final String dialogId = ParamsExtractor.getDialogId(parameters);
    messageDeliveryProvider.sendMessageToChat(application, dialogId, getLocalizedMessage(application.getLanguage(),"help.text"));
  }
}
