package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ApplicationQuery;

/**
 * author: Artem Voronov
 */
public class ApplicationController {

  private DBService db;

  public ApplicationController(DBService db) {
    this.db = db;
  }

  public Application create(String domain, String accessToken, String refreshToken, Integer botId, String language) {
    return db.tx(s -> {
      Application application = new Application();
      application.setDomain(domain);
      application.setAccessToken(accessToken);
      application.setRefreshToken(refreshToken);
      application.setBotId(botId);
      application.setLanguage(language);
      s.save(application);
      return application;
    });
  }

  public void update(Integer id, String accessToken, String refreshToken) {
    db.vtx(s -> {
      Application application = (Application) ApplicationQuery.byId(id, s).uniqueResult();
      application.setAccessToken(accessToken);
      application.setRefreshToken(refreshToken);
      s.save(application);
    });
  }

  public Application find(String domain) {
    return db.tx(s -> (Application) ApplicationQuery.byDomain(domain, s).uniqueResult());
  }

  public void delete(Integer id) {
    db.vtx(s -> {
      Application application = (Application) ApplicationQuery.byId(id, s).uniqueResult();
      s.delete(application);
    });
  }
}
