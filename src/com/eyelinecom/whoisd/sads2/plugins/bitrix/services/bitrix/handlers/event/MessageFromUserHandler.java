package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.MessageType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.PrettyPrintUtils;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.TemplateUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * author: Artem Voronov
 */
public class MessageFromUserHandler extends CommonEventHandler implements EventHandler {

  private static final Logger loggerMessagingSads = Logger.getLogger("BITRIX_PLUGIN_MESSAGING_WITH_SADS");

  private final ApplicationDAO applicationDAO;
  private final QueueDAO queueDAO;
  private final UserDAO userDAO;

  public MessageFromUserHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO, QueueDAO queueDAO, UserDAO userDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
    this.queueDAO = queueDAO;
    this.userDAO = userDAO;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    final MessageType messageType = ParamsExtractor.extractMessageType(parameters);
    if (messageType == null)//only text messages and images are supported
      return;

    final String data = messageType == MessageType.TEXT ? ParamsExtractor.getMessageText(parameters) : ParamsExtractor.getImageUrl(parameters);
    final String serviceId = ParamsExtractor.getServiceId(parameters);

    User user = getOrCreateUser(parameters);
    Queue queue = queueDAO.find(application, user, serviceId);

    if (queue == null) {
      processNewUser(parameters, application, user, messageType, data, serviceId);
    }
    else if (queue.getType() == QueueType.AWAITING) {
      queueDAO.storeMessage(queue, messageType, data);
    }
    else {
      Operator operator = queueDAO.getOperator(application, user, serviceId);
      sendMessageToOperator(operator, messageType, data, application.getLanguage());
    }
  }

  private User getOrCreateUser(Map<String, String[]> parameters) {
    final String userId = ParamsExtractor.getUserId(parameters);
    User user = userDAO.find(userId);

    if (user == null)
      user = userDAO.create(userId);

    return user;
  }

  private void processNewUser(Map<String, String[]> parameters, Application application, User user, MessageType messageType, String data, String serviceId) {
    final String protocol = ParamsExtractor.getProtocol(parameters);
    final String lang = ParamsExtractor.getLanguage(parameters);
    final String redirectBackPage = ParamsExtractor.getRedirectBackPageUrl(parameters);
    Queue queue = queueDAO.addToAwaitingQueue(application, user, serviceId, protocol, redirectBackPage, lang);
    queueDAO.storeMessage(queue, messageType, data);

    final String appLang = application.getLanguage();
    UserCounters counters = queueDAO.getUserCounters(application);
    String notification =  getLocalizedMessage(appLang,"new.user.arrived") + "\n" +
      getLocalizedMessage(appLang,"awaiting.users", counters.getAwaitingUsersCount() + "") + "\n" +
      getLocalizedMessage(appLang,"processing.users", counters.getProcessingUsersCount() + "");

    messageDeliveryProvider.sendMessageToAllChats(application, notification);
  }


  @Override
  public void sendResponse(Map<String, String[]> parameters, HttpServletResponse response) {
    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_SEND_MESSAGE request:\n" + PrettyPrintUtils.toPrettyMap(parameters) + "\n");

    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);

    final String domain = ParamsExtractor.getDomain(parameters);
    Application application = applicationDAO.find(domain);
    final String xml = application == null ? generateErrorPage(parameters) : generateEmptyPage(parameters);

    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_SEND_MESSAGE response:\n" + PrettyPrintUtils.toPrettyXml(xml));

    try {
      try (PrintWriter out = response.getWriter()) {
        out.write(xml);
      }
    } catch (IOException ex) {
      loggerMessagingSads.error("Error during user start messaging event: " + ex.getMessage()  + ". Params: \n" + PrettyPrintUtils.toPrettyMap(parameters) + "\n", ex);
    }
  }

  private String generateEmptyPage(Map<String, String[]> parameters) {
    final String protocol = ParamsExtractor.getProtocol(parameters);
    final String lang = ParamsExtractor.getLanguage(parameters);
    final String redirectBackPage = ParamsExtractor.getRedirectBackPageUrl(parameters);
    return TemplateUtils.createEmptyPage(redirectBackPage, lang, protocol);
  }

  private void sendMessageToOperator(Operator operator, MessageType messageType, String data, String appLang) {
    switch (messageType) {
      case TEXT:
        messageDeliveryProvider.sendMessageToOperator(operator, data);
        break;
      case IMAGE:
        messageDeliveryProvider.sendImageToOperator(operator, getLocalizedMessage(appLang,"image.from.user"), data);
        break;
      default:
        throw new IllegalArgumentException("Unknown message type: " + messageType);
    }
  }

}
