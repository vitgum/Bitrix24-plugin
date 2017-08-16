package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.api.TokenUpdater;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import org.apache.log4j.Logger;

/**
 * author: Artem Voronov
 */
public class AppTokenUpdater implements TokenUpdater {

  private static final Logger logger = Logger.getLogger("BITRIX_TOKEN_UPDATER");
  private final ApplicationDAO applicationDAO;

  public AppTokenUpdater(ApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  @Override
  public void update(String domain, String newAccessToken, String newRefreshToken) {
    Application application = applicationDAO.find(domain);
    applicationDAO.update(application.getId(), newAccessToken, newRefreshToken);
    if(logger.isDebugEnabled())
      logger.debug("App: " + application.getDomain() + ". New access token: " + newAccessToken + ". New refresh token: " + newRefreshToken);
  }
}
