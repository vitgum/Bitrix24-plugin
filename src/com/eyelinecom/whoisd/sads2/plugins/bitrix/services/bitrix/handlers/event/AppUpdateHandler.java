package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventHandler;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class AppUpdateHandler implements EventHandler {
  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private final ApplicationDAO applicationDAO;

  public AppUpdateHandler(ApplicationDAO applicationDAO) {
    this.applicationDAO = applicationDAO;
  }

  @Override
  public void processEvent(Map<String, String[]> parameters) {
    final String domain = ParamsExtractor.getDomain(parameters);
    Application application = applicationDAO.find(domain);
    if (application == null)
      return;

    if (logger.isDebugEnabled())
      logger.debug("Application update event. Domain: " + domain);
  }
}
