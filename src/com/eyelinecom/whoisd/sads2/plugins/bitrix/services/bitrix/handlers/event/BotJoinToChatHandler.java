package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
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
public class BotJoinToChatHandler extends CommonEventHandler implements EventHandler {
  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private final ApplicationDAO applicationDAO;

  public BotJoinToChatHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    if (logger.isDebugEnabled())
      logger.debug("Bot join to chat event. Domain: " + domain);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    processChatJoining(parameters, application);
  }

  private void processChatJoining(Map<String, String[]> parameters, Application application) {
    final Chat.Type chatType = ParamsExtractor.getChatType(parameters);
    final String appLang = application.getLanguage();

    switch (chatType){
      case GROUP:
        Chat chat = getOrCreateChat(parameters, application);
        messageDeliveryProvider.sendMessageToChat(application, chat.getDialogId(), getLocalizedMessage(appLang,"welcome"));
        break;
      case PRIVATE:
        Operator operator = getOrCreateOperator(parameters, application);
        messageDeliveryProvider.sendMessageToChat(application, operator.getDialogId(), getLocalizedMessage(appLang,"welcome"));
        break;
    }
  }
}
