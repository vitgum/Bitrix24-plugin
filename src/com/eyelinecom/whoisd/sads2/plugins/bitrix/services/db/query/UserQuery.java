package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 * author: Artem Voronov
 */
public class UserQuery {

  public static Criteria all(Session s) {
    Criteria c = s.createCriteria(User.class);
    c.setCacheable(true);
    c.setCacheRegion("users");
    c.setCacheMode(CacheMode.NORMAL);
    return c;
  }

  public static Criteria byUserId(String userId, Session s) {
    return all(s)
      .add(Restrictions.eq("userId", userId));
  }

}
