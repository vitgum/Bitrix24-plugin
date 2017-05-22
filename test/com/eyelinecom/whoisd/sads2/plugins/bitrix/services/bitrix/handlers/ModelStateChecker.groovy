package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.ChatTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.IncomeMessage
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.OperatorTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.UserTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ChatQuery
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.OperatorQuery
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.QueueQuery
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.UserQuery
import org.hibernate.Session
import org.junit.Ignore

/**
 * author: Artem Voronov
 */
@Ignore
class ModelStateChecker extends GroovyTestCase {

  static void assertOperatorWasCreated(DBService db, Application application) {
    db.vtx { Session s ->
      Operator operator = OperatorQuery.all(s).uniqueResult() as Operator

      assertNotNull operator
      assertNotNull operator.id
      assertNotNull operator.operatorId
      ApplicationTest.assertApplicationsEquals application, operator.application
    }
  }

  static void assertExistsOnlyOneOperator(DBService db, Operator operator) {
    db.vtx { Session s ->
      List<Operator> operators = OperatorQuery.all(s).list()

      assertNotNull operators
      assertEquals 1, operators.size()
      Operator another = operators.get(0)
      OperatorTest.assertOperatorsEquals operator, another
    }
  }

  static void assertChatWasCreated(DBService db, Application application) {
    db.vtx { Session s ->
      Chat chat = ChatQuery.all(s).uniqueResult() as Chat

      assertNotNull chat
      assertNotNull chat.id
      assertNotNull chat.chatId
      assertEquals Chat.Type.GROUP, chat.type
      ApplicationTest.assertApplicationsEquals application, chat.application
    }
  }

  static User assertUserWasCreated(DBService db) {
    db.tx { Session s ->
      User user = UserQuery.all(s).uniqueResult() as User

      assertNotNull user
      assertNotNull user.id
      assertNotNull user.userId

      return user
    }
  }

  static IncomeMessage assertIncomeMessageWasCreated(DBService db) {
    db.tx { Session s ->
      IncomeMessage incomeMessage = s.createCriteria(IncomeMessage.class).uniqueResult() as IncomeMessage

      assertNotNull incomeMessage
      assertNotNull incomeMessage.id
      assertNotNull incomeMessage.text

      return incomeMessage
    }
  }

  static Queue assertAwaitingQueueWasCreated(DBService db, Application application, User user) {
    db.tx { Session s ->
      Queue queue = QueueQuery.all(s).uniqueResult() as Queue

      assertNotNull queue
      assertNotNull queue.id
      assertEquals QueueType.AWAITING, queue.type
      ApplicationTest.assertApplicationsEquals application, queue.application
      UserTest.assertUsersEquals user, queue.user
      return queue
    }
  }

  static void assertExistsOnlyOneChat(DBService db, Chat chat) {
    db.vtx { Session s ->
      List<Chat> chats = ChatQuery.all(s).list()

      assertNotNull chats
      assertEquals 1, chats.size()
      Chat another = chats.get(0)
      ChatTest.assertChatsEquals chat, another
    }
  }

  static void assertExistsOnlyOneUser(DBService db, User user) {
    db.vtx { Session s ->
      List<User> users = UserQuery.all(s).list()

      assertNotNull users
      assertEquals 1, users.size()
      User another = users.get(0)
      UserTest.assertUsersEquals user, another
    }
  }

  static void assertExistsOnlyOneQueue(DBService db, Queue queue) {
    db.tx { Session s ->
      List<Queue> queues = QueueQuery.all(s).list()

      assertNotNull queues
      assertEquals 1, queues.size()
      Queue another = queues.get(0)
      QueueTest.assertQueuesEquals queue, another
    }
  }

  static void assertQueueIsProcessing(DBService db, Queue queue, Operator operator) {
    db.tx { Session s ->
      Queue q = QueueQuery.byId(queue.id, s).uniqueResult() as Queue

      assertNotNull q
      assertNotNull q.operator
      OperatorTest.assertOperatorsEquals operator, q.operator
      assertEquals QueueType.PROCESSING, q.type
    }
  }

  static void assertMessageWasStored(DBService db, Queue queue, IncomeMessage msg) {
    db.vtx { Session s ->
      Queue loaded = QueueQuery.byId(queue.id, s).uniqueResult() as Queue
      List<IncomeMessage> storedMessages = loaded.incomeMessages
      IncomeMessage result = storedMessages.find { it ->
        it.id == msg.id
      }

      assertEquals msg.text, result.text

    }
  }

  static void assertMessageWasStored(DBService db, Queue queue, String text) {
    db.vtx { Session s ->
      Queue loaded = QueueQuery.byId(queue.id, s).uniqueResult() as Queue
      List<IncomeMessage> storedMessages = loaded.incomeMessages
      IncomeMessage result = storedMessages.find { it ->
        it.text == text
      }

      assertNotNull result
    }
  }

  static void assertNoChats(DBService db) {
    db.vtx { Session s ->
      List<Chat> chats = ChatQuery.all(s).list()

      assertNotNull chats
      assertEquals 0, chats.size()
    }
  }

  static void assertNoOperators(DBService db) {
    db.vtx { Session s ->
      List<Operator> operators = OperatorQuery.all(s).list()

      assertNotNull operators
      assertEquals 0, operators.size()
    }
  }

  static void assertNoUsers(DBService db) {
    db.vtx { Session s ->
      List<User> users = UserQuery.all(s).list()

      assertNotNull users
      assertEquals 0, users.size()
    }
  }

  static void assertNoQueues(DBService db) {
    db.vtx { Session s ->
      List<Queue> queues = QueueQuery.all(s).list()

      assertNotNull queues
      assertEquals 0, queues.size()
    }
  }

  static void assertNoIncomeMessages(DBService db) {
    db.vtx { Session s ->
      List<IncomeMessage> incomeMessages = s.createCriteria(IncomeMessage.class).list()

      assertNotNull incomeMessages
      assertEquals 0, incomeMessages.size()
    }
  }

}
