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
public class StartHandler extends CommonEventHandler implements CommandHandler {
  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationDAO applicationDAO;
  private final QueueDAO queueDAO;

  public StartHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO, QueueDAO queueDAO) {
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
      messageDeliveryProvider.sendMessageToChat(application, dialogId, getLocalizedMessage(appLang, "only.for.private.chats"));
      return;
    }

    Operator operator = getOrCreateOperator(parameters, application);

    synchronized (queueDAO) {
      if (queueDAO.isOperatorBusy(operator)) {
        messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang, "already.messaging.with.user"));
        return;
      }

      Queue firstAwaiting = queueDAO.getFirstAwaiting(application);

      if (firstAwaiting == null) {
        messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang, "no.users"));
      } else {
        Integer queueId = firstAwaiting.getId();
        queueDAO.moveToProcessingQueue(queueId, operator);

        //TODO: process images and texts separately
        String userMessages = queueDAO.getMessages(queueId);
        messageDeliveryProvider.sendMessageToOperator(operator, getLocalizedMessage(appLang, "message.from", firstAwaiting.getProtocol()) + "\n" + userMessages);

        queueDAO.deleteMessages(queueId);

        String operatorFullName = ParamsExtractor.getOperatorFullNameWithEncoding(parameters);

        if (logger.isDebugEnabled())
          logger.debug("Operator start messaging. Application domain: " + domain + ". Operator ID: " + operator.getId() + ". Operator name: " + operatorFullName + ". User ID: " + firstAwaiting.getUser().getUserId() + ". Service ID: " + firstAwaiting.getServiceId());

        messageDeliveryProvider.sendMessageToUser(firstAwaiting, getLocalizedMessage(firstAwaiting.getLanguage(), "operator.greetings", operatorFullName));
      }
    }
  }
}
