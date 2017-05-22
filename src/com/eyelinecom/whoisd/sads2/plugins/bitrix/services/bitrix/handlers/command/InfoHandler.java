package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
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
  private final ApplicationDAO applicationDAO;
  private final QueueDAO queueDAO;

  public InfoHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO, QueueDAO queueDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
    this.queueDAO = queueDAO;
  }

  @Override
  public void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    if (logger.isDebugEnabled())
      logger.debug("Info command. Domain: " + domain);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    processIfChatNotJoined(parameters, application);
    final String dialogId = ParamsExtractor.getDialogId(parameters);

    UserCounters counters = queueDAO.getUserCounters(application);
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
