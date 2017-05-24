package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.CommonEventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.EncodingUtils;
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
public class UserStartMessagingHandler extends CommonEventHandler implements EventHandler {

  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private static final Logger loggerMessagingSads = Logger.getLogger("BITRIX_PLUGIN_MESSAGING_WITH_SADS");

  private final ApplicationDAO applicationDAO;

  public UserStartMessagingHandler(ChatDAO chatDAO, OperatorDAO operatorDAO, MessageDeliveryProvider messageDeliveryProvider, ResourceBundleController resourceBundleController, ApplicationDAO applicationDAO) {
    super(chatDAO, operatorDAO, messageDeliveryProvider, resourceBundleController);
    this.applicationDAO = applicationDAO;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);
    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    final String userId = ParamsExtractor.getUserId(parameters);
    final String serviceId = ParamsExtractor.getServiceId(parameters);
    final String protocol = ParamsExtractor.getProtocol(parameters);

    if (logger.isDebugEnabled())
      logger.debug("User start messaging. Domain: " + domain + ".User ID: " + userId + ". Service: " + serviceId + ". Protocol: " + protocol);
  }

  @Override
  public void sendResponse(Map<String, String[]> parameters, HttpServletResponse response) {
    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_START_MESSAGING request:\n" + PrettyPrintUtils.toPrettyMap(parameters) + "\n");

    final String domain = ParamsExtractor.getDomain(parameters);
    Application application = applicationDAO.find(domain);
    final String xml = application == null ? generateErrorPage(parameters) : generateBasicPage(parameters);

    if (loggerMessagingSads.isDebugEnabled())
      loggerMessagingSads.debug("USER_START_MESSAGING response:\n" + PrettyPrintUtils.toPrettyXml(xml));

    response.setCharacterEncoding("UTF-8");
    response.setStatus(HttpServletResponse.SC_OK);
    try {
      try (PrintWriter out = response.getWriter()) {
        out.write(xml);
      }
    } catch (IOException ex) {
      loggerMessagingSads.error("Error during user start messaging event", ex);
    }
  }

  private String generateBasicPage(Map<String, String[]> parameters) {
    final String deployUrl = PluginContext.getInstance().getPluginUrl();
    final String domain = ParamsExtractor.getDomain(parameters);
    final String userId = ParamsExtractor.getUserId(parameters);
    final String serviceId = ParamsExtractor.getServiceId(parameters);
    final String protocol = ParamsExtractor.getProtocol(parameters);
    final String lang = ParamsExtractor.getLanguage(parameters);

    final String backPageOriginal = ParamsExtractor.getBackPageUrl(parameters);
    final String encodedAndEscapedBackPageOriginal = EncodingUtils.encode(EncodingUtils.escape(backPageOriginal));

    final String redirectBackPageUrl = TemplateUtils.createRedirectBackPageUrl(deployUrl, domain, userId, serviceId, protocol, lang, encodedAndEscapedBackPageOriginal);
    final String escapedRedirectBackPageUrl = EncodingUtils.escape(redirectBackPageUrl);
    final String encodedAndEscapedRedirectBackPageUrl = EncodingUtils.encode(escapedRedirectBackPageUrl);
    final String inputTitle = getLocalizedMessage(lang, "init.message.for.user");
    final String inputUrl = TemplateUtils.createInputUrl(deployUrl, domain, lang, encodedAndEscapedRedirectBackPageUrl);
    final String escapedInputUrl = EncodingUtils.escape(inputUrl);
    return TemplateUtils.createBasicPage(inputTitle, escapedInputUrl, escapedRedirectBackPageUrl, lang);
  }
}
