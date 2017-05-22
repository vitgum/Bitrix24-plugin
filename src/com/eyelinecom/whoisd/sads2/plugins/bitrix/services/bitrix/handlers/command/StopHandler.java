package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.command;


import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommandHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueDAO;
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
  private final ApplicationDAO applicationDAO;
  private final QueueDAO queueDAO;

  public StopHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO, QueueDAO queueDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
    this.queueDAO = queueDAO;
  }

  @Override
  public void handle(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    final String appLang = application.getLanguage();

    if (!isPrivateChat(parameters)) {
      final String dialogId = ParamsExtractor.getDialogId(parameters);
      messageDeliveryProvider.sendMessageToChat(application, dialogId, getLocalizedMessage(appLang,"only.for.private.chats"));
      return;
    }

    Operator operator = getOrCreateOperator(parameters, application);
    synchronized (queueDAO) {
      Queue queue = queueDAO.getProcessingQueue(operator);

      if (queue != null) {
        if (logger.isDebugEnabled()) {
          String operatorFullName = ParamsExtractor.getOperatorFullNameWithEncoding(parameters);
          logger.debug("Operator stop messaging. Application domain: " + domain + ". Operator ID: " + operator.getId() + ". Operator name: " + operatorFullName + ". User ID: " + queue.getUser().getUserId() + ". Service ID: " + queue.getServiceId());
        }
        queueDAO.removeFromQueue(queue.getApplication(), queue.getUser(), queue.getServiceId());
        messageDeliveryProvider.sendMessageToUser(queue, getLocalizedMessage(queue.getLanguage(), "operator.quit"));
        messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang, "user.flushed"));
      } else {
        messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang, "user.not.flushed"));
      }
    }
  }
}
