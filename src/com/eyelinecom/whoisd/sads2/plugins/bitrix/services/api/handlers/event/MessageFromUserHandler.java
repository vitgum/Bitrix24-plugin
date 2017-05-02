package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.TemplateUtils;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.PrettyPrintUtils;
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

  private final ApplicationController applicationController;
  private final QueueController queueController;
  private final UserController userController ;

  public MessageFromUserHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.applicationController = services.getApplicationController();
    this.queueController = services.getQueueController();
    this.userController = services.getUserController();
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    final String message = ParamsExtractor.getMessage(parameters);
    final String serviceId = ParamsExtractor.getServiceId(parameters);

    User user = getOrCreateUser(parameters);
    Queue queue = queueController.find(application, user, serviceId);

    if (queue == null) {
      processNewUser(parameters, application, user, message, serviceId);
    }
    else if (queue.getType() == QueueType.AWAITING) {
      queueController.storeMessage(queue, message);
    }
    else {
      Operator operator = queueController.getOperator(application, user, serviceId);
      messageDeliveryService.sendMessageToOperator(operator, message);
    }
  }

  private User getOrCreateUser(Map<String, String[]> parameters) {
    final String userId = ParamsExtractor.getUserId(parameters);
    User user = userController.find(userId);

    if (user == null)
      user = userController.create(userId);

    return user;
  }

  private void processNewUser(Map<String, String[]> parameters, Application application, User user, String message, String serviceId) {
    final String protocol = ParamsExtractor.getProtocol(parameters);
    final String lang = ParamsExtractor.getLanguage(parameters);
    final String redirectBackPage = ParamsExtractor.getRedirectBackPageUrl(parameters);
    Queue queue = queueController.addToAwaitingQueue(application, user, serviceId, protocol, redirectBackPage, lang);
    queueController.storeMessage(queue, message);

    final String appLang = application.getLanguage();
    UserCounters counters = queueController.getUserCounters(application);
    String notification =  getLocalizedMessage(appLang,"new.user.arrived") + "\n" +
      getLocalizedMessage(appLang,"awaiting.users", counters.getAwaitingUsersCount() + "") + "\n" +
      getLocalizedMessage(appLang,"processing.users", counters.getProcessingUsersCount() + "");

    messageDeliveryService.sendMessageToAllChats(application, notification);
  }


  @Override
  public void sendResponse(Map<String, String[]> parameters, HttpServletResponse response) {
    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_SEND_MESSAGE request:\n" + PrettyPrintUtils.toPrettyMap(parameters) + "\n");

    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);

    final String protocol = ParamsExtractor.getProtocol(parameters);
    final String lang = ParamsExtractor.getLanguage(parameters);
    final String redirectBackPage = ParamsExtractor.getRedirectBackPageUrl(parameters);
    final String xml = TemplateUtils.createEmptyPage(redirectBackPage, lang, protocol);

    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_SEND_MESSAGE response:\n" + PrettyPrintUtils.toPrettyXml(xml));

    try {
      try (PrintWriter out = response.getWriter()) {
        out.write(xml);
      }
    } catch (IOException ex) {
      loggerMessagingSads.error("Error during user start messaging event", ex);
    }
  }

}
