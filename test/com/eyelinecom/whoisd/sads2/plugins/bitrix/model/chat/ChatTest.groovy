package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import org.hibernate.exception.ConstraintViolationException

import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class ChatTest extends DBTestBase {

  static Chat createCorrectChat(Map overrides = [:]) {
    def defaultFields = [
      application : ApplicationTest.createCorrectApplication(),
      chatId : 202,
      type : Chat.Type.GROUP
    ]
    return new Chat(defaultFields + overrides)
  }

  void testSaveAndLoad() {
    def chat = createCorrectChat()

    tx { s ->
      s.save(chat.application)
      s.save(chat)
    }

    assertNotNull(chat.id)

    def chat1 = tx { s -> s.get(Chat, chat.id) as Chat}

    assertNotNull(chat1)
    assertChatsEquals(chat, chat1)
  }

  void testDuplicate() {
    def app = ApplicationTest.createCorrectApplication()
    def chat = createCorrectChat(application: app)
    def same = createCorrectChat(application: app)
    def another = createCorrectChat(application: app, type: Chat.Type.PRIVATE)

    vtx { s ->
      s.save(app)
      s.save(chat)
      s.save(another)
    }

    assertNotNull(chat.id)
    assertNotNull(another.id)

    def msg = shouldFail(ConstraintViolationException) {
      vtx { s ->
        s.save(same)
      }
    }

    assertEquals 'could not execute statement', msg
  }

  static void assertChatsEquals(Chat expected, Chat actual) {
    ApplicationTest.assertApplicationsEquals expected.application, actual.application
    assertEquals expected.type, actual.type
    assertEquals expected.chatId, actual.chatId
  }
}
