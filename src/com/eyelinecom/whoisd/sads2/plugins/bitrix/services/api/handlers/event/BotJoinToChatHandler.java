package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
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
public class BotJoinToChatHandler extends CommonEventHandler implements EventHandler {
  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private final ApplicationController applicationController;

  public BotJoinToChatHandler(ChatController chatController, OperatorController operatorController, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationController applicationController) {
    super(chatController, operatorController, messageDeliveryProvider, resourceBundleController);
    this.applicationController = applicationController;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    if (logger.isDebugEnabled())
      logger.debug("Bot join to chat event. Domain: " + domain);

    Application application = applicationController.find(domain);
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
