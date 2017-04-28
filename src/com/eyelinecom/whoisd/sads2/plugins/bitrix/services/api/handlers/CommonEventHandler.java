package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class CommonEventHandler {
  protected final ChatController chatController;
  protected final OperatorController operatorController;
  protected final MessageDeliveryService messageDeliveryService;
  protected final ResourceBundleController resourceBundleController;

  public CommonEventHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.chatController = services.getChatController();
    this.operatorController = services.getOperatorController();
    this.messageDeliveryService = services.getMessageDeliveryService();
    this.resourceBundleController = services.getResourceBundleController();
  }

  protected Operator getOrCreateOperator(Map<String, String[]> parameters, Application application) {
    final int operatorId = ParamsExtractor.getOperatorId(parameters);
    Operator operator = operatorController.find(application, operatorId);

    if (operator == null) {
      operator =  operatorController.create(application, operatorId);
    }

    return operator;
  }

  protected Chat getOrCreateChat(Map<String, String[]> parameters, Application application) {
    final Chat.Type chatType = ParamsExtractor.getChatType(parameters);
    final int chatId = ParamsExtractor.getChatId(parameters, chatType);
    Chat chat = chatController.find(application, chatId);

    if (chat == null)
      chat = chatController.create(application, chatId, chatType);

    return chat;
  }

  protected boolean isPrivateChat(Map<String, String[]> parameters) {
    return ParamsExtractor.getChatType(parameters) == Chat.Type.PRIVATE;
  }

  protected boolean isGroupChat(Map<String, String[]> parameters) {
    return ParamsExtractor.getChatType(parameters) == Chat.Type.GROUP;
  }

  protected void processIdChatNotJoined(Map<String, String[]> parameters, Application application) {
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
    String template = resourceBundleController.getMessage(lang, key);
    return args.length > 0 ? String.format(template, args) : template;
  }

  protected String getLocalizedMessage(Map<String, String[]> parameters, String key, String ... args) {
    final String lang = ParamsExtractor.getLanguage(parameters);
    String template = resourceBundleController.getMessage(lang, key);
    return args.length > 0 ? String.format(template, args) : template;
  }
}
