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
public class AppUninstallHandler implements EventHandler {

  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationDAO applicationDAO;
  private final BitrixApiProvider api;

  public AppUninstallHandler(ApplicationDAO applicationDAO, BitrixApiProvider api) {
    this.applicationDAO = applicationDAO;
    this.api = api;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    deleteApplication(application);
  }

  private void deleteApplication(Application application) {
    api.deleteBot(application);
    applicationDAO.delete(application.getId());

    if (logger.isDebugEnabled())
      logger.debug("Application was deleted. Domain: " + application.getDomain() + ". Bot ID: " + application.getBotId() + ". Access token: " + application.getAccessToken() + ". Refresh token: " + application.getRefreshToken());
  }
}
