package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.BitrixApiClient;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class AppInstallHandler implements EventHandler {
  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationController applicationController;
  private final BitrixApiClient api;

  public AppInstallHandler(Services services) {
    this.applicationController = services.getApplicationController();
    this.api = services.getBitrixApiClient();
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);
    final String accessToken = ParamsExtractor.getAccessToken(parameters);
    final String refreshToken = ParamsExtractor.getRefreshToken(parameters);

    Application application = applicationController.find(domain);

    if (application != null)
      deleteApplication(application);

    createApplication(domain, accessToken, refreshToken);
  }

  private void deleteApplication(Application application) {
    api.deleteBot(application);
    applicationController.delete(application.getId());

    if (logger.isDebugEnabled())
      logger.debug("Application reinstall case. Domain: " + application.getDomain() + ". Old bot ID: " + application.getBotId() + ". Old access token: " + application.getAccessToken() + ". Old refresh token: " + application.getRefreshToken());
  }

  private void createApplication(String domain, String accessToken, String refreshToken) {
    final int botId = api.createBot(domain, accessToken, refreshToken);
    Application application = applicationController.create(domain, accessToken, refreshToken, botId);
    api.addBotCommands(application);

    if (logger.isDebugEnabled())
      logger.debug("New application installed. Domain: " + application.getDomain() + ". Bot ID: " + application.getBotId() + ". Access token: " + application.getAccessToken() + ". Refresh token: " + application.getRefreshToken());
  }
}
