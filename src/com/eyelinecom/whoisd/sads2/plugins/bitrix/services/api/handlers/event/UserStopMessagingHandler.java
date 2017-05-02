package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.EncodingUtils;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.PrettyPrintUtils;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.TemplateUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * author: Artem Voronov
 */
public class UserStopMessagingHandler extends CommonEventHandler implements EventHandler {

  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private static final Logger loggerMessagingSads = Logger.getLogger("BITRIX_PLUGIN_MESSAGING_WITH_SADS");

  private final ApplicationController applicationController;
  private final QueueController queueController;
  private final UserController userController ;

  public UserStopMessagingHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.applicationController = services.getApplicationController();
    this.queueController = services.getQueueController();
    this.userController = services.getUserController();
  }

  @Override
  public synchronized void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);
    Application application = applicationController.find(domain);
    if (application == null)
      return;

    final String userId = ParamsExtractor.getUserId(parameters);
    final String serviceId = ParamsExtractor.getServiceId(parameters);
    final String protocol = ParamsExtractor.getProtocol(parameters);
    if (logger.isDebugEnabled())
      logger.debug("User stop messaging. User ID: " + userId + ". Service: " + serviceId + ". Protocol: " + protocol);

    Queue queue = getQueue(userId, serviceId, application);
    if (queue == null)
      return;

    queueController.removeFromQueue(queue.getApplication(), queue.getUser(), queue.getServiceId());
    messageDeliveryService.sendMessageToOperator(queue.getOperator(), getLocalizedMessage(application.getLanguage(), "user.quit"));
  }

  @Override
  public void sendResponse(Map<String, String[]> parameters, HttpServletResponse response) {
    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_STOP_MESSAGING request:\n" + PrettyPrintUtils.toPrettyMap(parameters) + "\n");
    try {
      final String backPageOriginal = ParamsExtractor.getBackPageUrlOriginal(parameters);
      String redirectUrl = EncodingUtils.unescape(backPageOriginal);
      redirect(response, redirectUrl);
    } catch (Exception ex) {
      try {
        final String domain = ParamsExtractor.getDomain(parameters);
        Application application = applicationController.find(domain);
        if (application == null)
          return;

        final String userId = ParamsExtractor.getUserId(parameters);
        final String serviceId = ParamsExtractor.getServiceId(parameters);

        Queue queue = getQueue(userId, serviceId, application);
        if (queue == null) {
          loggerMessagingSads.error("USER_STOP_MESSAGING response:\n MISSED QUEUE" + "\n");
          response.setCharacterEncoding("UTF-8");
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return;
        }

        try {
          final String escapedRedirectBackBtnUrl = queue.getBackPage();
          final String unescapedRedirectBackPageUrl = EncodingUtils.unescape(escapedRedirectBackBtnUrl);
          final String encodedAndEscapedBackPageOriginal = ParamsExtractor.extractParamFromUrl(unescapedRedirectBackPageUrl, "back_page_original");
          final String resultUrl = EncodingUtils.unescape(EncodingUtils.decode(encodedAndEscapedBackPageOriginal));
          redirect(response, resultUrl);
        } catch (Exception e) {
          loggerMessagingSads.error("Unable to redirect", e);
        }
      } catch (Exception e) {
        loggerMessagingSads.error("Unable to redirect", e);
      }
    }
  }

  private void redirect(HttpServletResponse response, String url) {
    try {

      String redirectUrl = TemplateUtils.createInvalidateSessionUrl(EncodingUtils.encode(url));

      if (loggerMessagingSads.isDebugEnabled())
        loggerMessagingSads.debug("USER_STOP_MESSAGING response:\nredirects to " + redirectUrl + "\n" );

      response.sendRedirect(redirectUrl);
    } catch (IOException ex) {
      loggerMessagingSads.error("Error during user stop messaging event", ex);
    }
  }

  private Queue getQueue(String userId, String serviceId, Application application) {
    User user = userController.find(userId);

    if (user == null)
      user = userController.create(userId);

    return queueController.getProcessingQueue(application, user, serviceId);
  }
}
