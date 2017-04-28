package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 * author: Artem Voronov
 */
public class ChatQuery {

  public static Criteria all(Session s) {
    Criteria c = s.createCriteria(Chat.class);
    c.setCacheable(true);
    c.setCacheRegion("chats");
    c.setCacheMode(CacheMode.NORMAL);
    return c;
  }

  public static Criteria byType(Application application, Chat.Type type, Session s) {
    return all(s)
      .add(Restrictions.eq("application", application))
      .add(Restrictions.eq("type", type));
  }

  public static Criteria byChatId(Application application, Integer chatId, Session s) {
    return all(s)
      .add(Restrictions.eq("application", application))
      .add(Restrictions.eq("chatId", chatId));
  }

}
