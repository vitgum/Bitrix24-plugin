package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ChatQuery;

import java.util.List;

/**
 * author: Artem Voronov
 */
public class ChatDAO {

  private DBService db;

  public ChatDAO(DBService db) {
    this.db = db;
  }

  public Chat create(Application application, Integer chatId, Chat.Type type) {
    return db.tx(s -> {
      Chat chat = new Chat();
      chat.setApplication(application);
      chat.setChatId(chatId);
      chat.setType(type);
      s.save(chat);
      return chat;
    });
  }

  public Chat find(Application application, Integer chatId) {
    return db.tx(s -> (Chat) ChatQuery.byChatId(application, chatId, s).uniqueResult());
  }

  public List<Chat> find(Application application, Chat.Type type) {
    return db.tx(s -> ChatQuery.byType(application, type, s).list());
  }
}
