package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class StartHandler extends CommonEventHandler implements CommandHandler {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationController applicationController;
  private final QueueController queueController;

  public StartHandler(ChatController chatController, OperatorController operatorController, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationController applicationController, QueueController queueController) {
    super(chatController, operatorController, messageDeliveryProvider, resourceBundleController);
    this.applicationController = applicationController;
    this.queueController = queueController;
  }

  @Override
  public synchronized void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    final String appLang = application.getLanguage();

    if (!isPrivateChat(parameters)) {
      final String dialogId = ParamsExtractor.getDialogId(parameters);
      messageDeliveryProvider.sendMessageToChat(application, dialogId, getLocalizedMessage(appLang,"only.for.private.chats"));
      return;
    }

    Operator operator = getOrCreateOperator(parameters, application);
    if (queueController.isOperatorBusy(operator)) {
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang,"already.messaging.with.user"));
      return;
    }

    Queue firstAwaiting = queueController.getFirstAwaiting(application);

    if (firstAwaiting == null) {
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang,"no.users"));
    } else {
      Integer queueId = firstAwaiting.getId();
      queueController.moveToProcessingQueue(queueId, operator);

      String userMessages = queueController.getMessages(queueId);
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang, "message.from", firstAwaiting.getProtocol()) + "\n" + userMessages);

      queueController.deleteMessages(queueId);

      String operatorFullName = ParamsExtractor.getOperatorFullNameWithEncoding(parameters);

      if (logger.isDebugEnabled())
        logger.debug("Operator start messaging. Application domain: " + domain + ". Operator ID: " + operator.getId() + ". Operator name: " + operatorFullName + ". User ID: " + firstAwaiting.getUser().getUserId()+ ". Service ID: " + firstAwaiting.getServiceId());

      messageDeliveryProvider.sendMessageToUser(firstAwaiting, getLocalizedMessage(firstAwaiting.getLanguage(), "operator.greetings", operatorFullName));
    }
  }
}
