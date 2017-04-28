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
public class AppUninstallHandler implements EventHandler {

  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationController applicationController;
  private final BitrixApiClient api;

  public AppUninstallHandler() {
    Services services = PluginContext.getInstance().getServices();
    this.applicationController = services.getApplicationController();
    this.api = services.getBitrixApiClient();
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);

    Application application = applicationController.find(domain);
    if (application == null)
      return;

    deleteApplication(application);
  }

  private void deleteApplication(Application application) {
    api.deleteBot(application);
    applicationController.delete(application.getId());

    if (logger.isDebugEnabled())
      logger.debug("Application was deleted. Domain: " + application.getDomain() + ". Bot ID: " + application.getBotId() + ". Access token: " + application.getAccessToken() + ". Refresh token: " + application.getRefreshToken());
  }
}
