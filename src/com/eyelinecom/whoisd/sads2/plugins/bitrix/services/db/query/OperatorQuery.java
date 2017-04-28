package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 * author: Artem Voronov
 */
public class OperatorQuery {

  public static Criteria all(Session s) {
    Criteria c = s.createCriteria(Operator.class);
    c.setCacheable(true);
    c.setCacheRegion("operators");
    c.setCacheMode(CacheMode.NORMAL);
    return c;
  }

  public static Criteria byApplication(Application application, int operatorId, Session s) {
    return all(s)
      .add(Restrictions.eq("application", application))
      .add(Restrictions.eq("operatorId", operatorId));
  }

}
