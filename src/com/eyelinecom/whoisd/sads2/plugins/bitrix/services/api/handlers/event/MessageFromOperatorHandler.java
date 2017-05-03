package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class MessageFromOperatorHandler extends CommonEventHandler implements EventHandler {
  private final ApplicationController applicationController;
  private final QueueController queueController;

  public MessageFromOperatorHandler(ChatController chatController, OperatorController operatorController, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationController applicationController, QueueController queueController) {
    super(chatController, operatorController, messageDeliveryProvider, resourceBundleController);
    this.applicationController = applicationController;
    this.queueController = queueController;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    if (!isPrivateChat(parameters))
      return;

    Operator operator = getOrCreateOperator(parameters, application);
    Queue queue = queueController.getProcessingQueue(operator);

    if (queue == null) {
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(application.getLanguage(),"user.start.command"));
    } else {
      final String message = ParamsExtractor.getMessageWithEncoding(parameters);
      messageDeliveryProvider.sendMessageToUser(queue, message);
    }
  }

}
