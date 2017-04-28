package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.UserQuery;

/**
 * author: Artem Voronov
 */
public class UserController {

  private DBService db;

  public UserController(DBService db) {
    this.db = db;
  }

  public User create(String userId) {
    return db.tx(s -> {
      User user = new User();
      user.setUserId(userId);
      s.save(user);
      return user;
    });
  }

  public User find(String userId) {
    return db.tx(s -> (User) UserQuery.byUserId(userId, s).uniqueResult());
  }
}
