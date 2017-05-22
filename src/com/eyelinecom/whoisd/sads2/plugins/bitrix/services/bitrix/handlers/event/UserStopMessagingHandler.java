package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
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

  private final ApplicationDAO applicationDAO;
  private final QueueDAO queueDAO;
  private final UserDAO userDAO;

  public UserStopMessagingHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO, QueueDAO queueDAO, UserDAO userDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
    this.queueDAO = queueDAO;
    this.userDAO = userDAO;
  }

  @Override
  public synchronized void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);
    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    final String userId = ParamsExtractor.getUserId(parameters);
    final String serviceId = ParamsExtractor.getServiceId(parameters);
    final String protocol = ParamsExtractor.getProtocol(parameters);
    if (logger.isDebugEnabled())
      logger.debug("User stop messaging. Domain: " + domain + ".User ID: " + userId + ". Service: " + serviceId + ". Protocol: " + protocol);

    Queue queue = getProcessingQueue(userId, serviceId, application);
    if (queue == null)
      return;

    queueDAO.removeFromQueue(queue.getApplication(), queue.getUser(), queue.getServiceId());
    messageDeliveryProvider.sendMessageToOperator(queue.getOperator(), getLocalizedMessage(application.getLanguage(), "user.quit"));
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
        Application application = applicationDAO.find(domain);
        if (application == null)
          return;

        final String userId = ParamsExtractor.getUserId(parameters);
        final String serviceId = ParamsExtractor.getServiceId(parameters);

        Queue queue = getProcessingQueue(userId, serviceId, application);
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

  private Queue getProcessingQueue(String userId, String serviceId, Application application) {
    User user = userDAO.find(userId);

    if (user == null)
      user = userDAO.create(userId);

    return queueDAO.getProcessingQueue(application, user, serviceId);
  }
}
