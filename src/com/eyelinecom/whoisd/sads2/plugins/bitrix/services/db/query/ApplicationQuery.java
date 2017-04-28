package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 * author: Artem Voronov
 */
public class ApplicationQuery {

  public static Criteria all(Session s) {
    Criteria c = s.createCriteria(Application.class);
    c.setCacheable(true);
    c.setCacheRegion("applications");
    c.setCacheMode(CacheMode.NORMAL);
    c.add(Restrictions.ne("deleted", true));
    return c;
  }

  public static Criteria byId(Integer id, Session s) {
    return all(s).add(Restrictions.idEq(id));
  }

  public static Criteria byDomain(String domain, Session s) {
    return all(s).add(Restrictions.eq("domain", domain));
  }

}
