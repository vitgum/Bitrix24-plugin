package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.OperatorQuery;

/**
 * author: Artem Voronov
 */
public class OperatorDAO {

  private DBService db;

  public OperatorDAO(DBService db) {
    this.db = db;
  }

  public Operator create(Application application, int operatorId) {
    return db.tx(s -> {
      Operator operator = new Operator();
      operator.setApplication(application);
      operator.setOperatorId(operatorId);
      s.save(operator);
      return operator;
    });
  }

  public Operator find(Application application, int operatorId) {
    return db.tx(s -> (Operator) OperatorQuery.byApplication(application, operatorId, s).uniqueResult());
  }
}
