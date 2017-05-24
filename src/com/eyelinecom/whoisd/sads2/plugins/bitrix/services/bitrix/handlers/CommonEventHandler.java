package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.TemplateUtils;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class CommonEventHandler {
  protected final ChatDAO chatDAO;
  protected final OperatorDAO operatorDAO;
  protected final MessageDeliveryProvider messageDeliveryProvider;
  protected final ResourceBundleController resourceBundleController;

  public CommonEventHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController) {
    this.chatDAO = chatDAO;
    this.operatorDAO = operatorDAO;
    this.messageDeliveryProvider = messageDeliveryProvider;
    this.resourceBundleController = resourceBundleController;
  }

  protected Operator getOrCreateOperator(Map<String, String[]> parameters, Application application) {
    final int operatorId = ParamsExtractor.getOperatorId(parameters);
    Operator operator = operatorDAO.find(application, operatorId);

    if (operator == null) {
      operator =  operatorDAO.create(application, operatorId);
    }

    return operator;
  }

  protected Chat getOrCreateChat(Map<String, String[]> parameters, Application application) {
    final Chat.Type chatType = ParamsExtractor.getChatType(parameters);
    final int chatId = ParamsExtractor.getChatId(parameters, chatType);
    Chat chat = chatDAO.find(application, chatId);

    if (chat == null)
      chat = chatDAO.create(application, chatId, chatType);

    return chat;
  }

  protected boolean isPrivateChat(Map<String, String[]> parameters) {
    return ParamsExtractor.getChatType(parameters) == Chat.Type.PRIVATE;
  }

  protected void processIfChatNotJoined(Map<String, String[]> parameters, Application application) {
    final Chat.Type chatType = ParamsExtractor.getChatType(parameters);

    switch (chatType){
      case GROUP:
        getOrCreateChat(parameters, application);
        break;
      case PRIVATE:
        getOrCreateOperator(parameters, application);
        break;
    }
  }

  protected String getLocalizedMessage(String lang, String key, String ... args) {
    return resourceBundleController.getMessage(lang, key, args);
  }

  protected String generateErrorPage(Map<String, String[]> parameters) {
    final String lang = ParamsExtractor.getLanguage(parameters);
    return TemplateUtils.createErrorPage(getLocalizedMessage(lang, "error.service.not.unavailable"), getLocalizedMessage(lang, "start.again"));
  }
}
