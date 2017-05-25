package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class MessageFromOperatorHandler extends CommonEventHandler implements EventHandler {
  private final ApplicationDAO applicationDAO;
  private final QueueDAO queueDAO;

  public MessageFromOperatorHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO, QueueDAO queueDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
    this.queueDAO = queueDAO;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    if (!isPrivateChat(parameters))
      return;

    Operator operator = getOrCreateOperator(parameters, application);
    Queue queue = queueDAO.getProcessingQueue(operator);

    if (queue == null) {
      messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(application.getLanguage(),"user.start.command"));
    } else {

      if (!ParamsExtractor.hasMessageText(parameters))//only text messages are supported
        return;

      final String message = ParamsExtractor.getMessageTextWithEncoding(parameters);
      messageDeliveryProvider.sendMessageToUser(queue, message);
    }
  }

}
