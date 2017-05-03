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
public class StopHandler extends CommonEventHandler implements CommandHandler  {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private final ApplicationController applicationController;
  private final QueueController queueController;

  public StopHandler(ChatController chatController, OperatorController operatorController, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationController applicationController, QueueController queueController) {
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
    Queue queue = queueController.getProcessingQueue(operator);

    if (queue!=null) {
      if (logger.isDebugEnabled()) {
        String operatorFullName = ParamsExtractor.getOperatorFullNameWithEncoding(parameters);
        logger.debug("Operator stop messaging. Application domain: " + domain + ". Operator ID: " + operator.getId() + ". Operator name: " + operatorFullName + ". User ID: " + queue.getUser().getUserId() + ". Service ID: " + queue.getServiceId());
      }
      queueController.removeFromQueue(queue.getApplication(), queue.getUser(), queue.getServiceId());
      messageDeliveryProvider.sendMessageToUser(queue, getLocalizedMessage(queue.getLanguage(),"operator.quit"));
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang,"user.flushed")) ;
    } else {
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang,"user.not.flushed"));
    }
  }
}
