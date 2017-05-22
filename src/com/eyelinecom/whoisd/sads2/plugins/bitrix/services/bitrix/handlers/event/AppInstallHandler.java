package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.BitrixApiProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class AppInstallHandler implements EventHandler {
  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationDAO applicationDAO;
  private final BitrixApiProvider api;

  public AppInstallHandler(ApplicationDAO applicationDAO, BitrixApiProvider bitrixApiProvider) {
    this.applicationDAO = applicationDAO;
    this.api = bitrixApiProvider;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationDAO.find(domain);

    if (application != null)
      deleteApplication(application);

    final String accessToken = ParamsExtractor.getAccessToken(parameters);
    final String refreshToken = ParamsExtractor.getRefreshToken(parameters);
    final String lang = ParamsExtractor.getLanguage(parameters);

    createApplication(domain, accessToken, refreshToken, lang);
  }

  private void deleteApplication(Application application) {
    if (logger.isDebugEnabled())
      logger.debug("Application reinstall case. Domain: " + application.getDomain() + ". Old bot ID: " + application.getBotId() + ". Old access token: " + application.getAccessToken() + ". Old refresh token: " + application.getRefreshToken());

    api.deleteBot(application);
    applicationDAO.delete(application.getId());
  }

  private void createApplication(String domain, String accessToken, String refreshToken, String lang) {
    final int botId = api.createBot(domain, accessToken, refreshToken);
    Application application = applicationDAO.create(domain, accessToken, refreshToken, botId, lang);
    api.addBotCommands(application);

    if (logger.isDebugEnabled())
      logger.debug("New application installed. Domain: " + application.getDomain() + ". Bot ID: " + application.getBotId() + ". Access token: " + application.getAccessToken() + ". Refresh token: " + application.getRefreshToken());
  }
}
