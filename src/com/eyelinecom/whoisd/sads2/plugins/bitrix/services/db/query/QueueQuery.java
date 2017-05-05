package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

/**
 * author: Artem Voronov
 */
public class QueueQuery {

  public static Criteria all(Session s) {
    Criteria c = s.createCriteria(Queue.class);
    c.setCacheable(true);
    c.setCacheRegion("queues");
    c.setCacheMode(CacheMode.NORMAL);
    return c;
  }

  public static Criteria byId(Integer id, Session s) {
    return all(s).add(Restrictions.idEq(id));
  }

  public static Criteria byApplication(Application application, Session s) {
    return all(s)
      .add(Restrictions.eq("application", application));
  }

  public static Criteria byUser(Application application, User user, String serviceId, Session s) {
    return byApplication(application, s)
      .add(Restrictions.eq("user", user))
      .add(Restrictions.eq("serviceId", serviceId));
  }

  public static Criteria byOperator(Operator operator, Session s) {
    return byApplication(operator.getApplication(), s)
      .add(Restrictions.eq("operator", operator));
  }

  public static Criteria byTypeAndUser(Application application, User user, String serviceId, QueueType queueType, Session s) {
    return byApplication(application, s)
      .add(Restrictions.eq("user", user))
      .add(Restrictions.eq("serviceId", serviceId))
      .add(Restrictions.eq("type", queueType));
  }

  public static Criteria byTypeAndOperator(Operator operator, QueueType queueType, Session s) {
    return byApplication(operator.getApplication(), s)
      .add(Restrictions.eq("operator", operator))
      .add(Restrictions.eq("type", queueType));
  }

  public static Criteria byTypeInAscendingOrder(Application application, QueueType type, Session s) {
    return byApplication(application, s)
      .add(Restrictions.eq("type", type))
      .addOrder( Property.forName("createDate").asc());
  }

  public static Criteria byTypeInAscendingOrder(Application application, QueueType type, int maxResults, Session s) {
    Criteria c = byTypeInAscendingOrder(application, type, s);
    c.setMaxResults(maxResults);
    return c;
  }

}
