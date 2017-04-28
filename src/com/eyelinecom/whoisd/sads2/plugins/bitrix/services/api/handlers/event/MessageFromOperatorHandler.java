package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class MessageFromOperatorHandler extends CommonEventHandler implements EventHandler {
  private final ApplicationController applicationController;
  private final QueueController queueController;

  public MessageFromOperatorHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.applicationController = services.getApplicationController();
    this.queueController = services.getQueueController();
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    if (!isPrivateChat(parameters))
      return;

    Operator operator = operatorController.find(application, ParamsExtractor.getOperatorId(parameters));

    if (operator == null)
     operator = getOrCreateOperator(parameters, application);

    Queue queue = queueController.getProcessingQueue(operator);

    if (queue == null) {
      messageDeliveryService.sendMessageToOperator(operator, getLocalizedMessage(parameters,"user.start.command"));
    } else {
      final String message = ParamsExtractor.getMessageWithEncoding(parameters);
      messageDeliveryService.sendMessageToUser(queue, message);
    }
  }

}
