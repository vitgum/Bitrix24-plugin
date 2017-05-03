package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.ChatTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.OperatorTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.Examples
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ChatQuery
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.OperatorQuery
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor
import groovy.mock.interceptor.MockFor
import org.hibernate.Session

/**
 * author: Artem Voronov
 */
class BotJoinToChatHandlerTest extends DBTestBase {

  protected BotJoinToChatHandler createBotJoinToChatHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationController applicationController = new ApplicationController(db)
    ChatController chatController = new ChatController(db)
    OperatorController operatorController = new OperatorController(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()

    return new BotJoinToChatHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController)
  }

  static Chat preInstallChat(DBService dbService, Application application) {
    Chat.Type chatType = ParamsExtractor.getChatType(Examples.EVENT_BOT_JOIN_GROUP_CHAT_PARAMETERS)
    int chatId = ParamsExtractor.getChatId(Examples.EVENT_BOT_JOIN_GROUP_CHAT_PARAMETERS, chatType)
    def chat = ChatTest.createCorrectChat(application: application, chatId: chatId, type: chatType)

    dbService.tx { s -> s.save(chat) }

    return chat
  }

  static Operator preInstallOperator(DBService dbService, Application application) {
    int operatorId = ParamsExtractor.getOperatorId(Examples.EVENT_BOT_JOIN_PRIVATE_CHAT_PARAMETERS)
    def chat = OperatorTest.createCorrectOperator(application: application, operatorId: operatorId)

    dbService.tx { s -> s.save(chat) }

    return chat
  }

  /**
   * Бот подключился в приватный чат. Оператора в БД ещё нет. Проверям что:
   * 1. в БД был сохранён оператор
   * 2. в чат куда подключился бот было отправлено сообщение
   */
  void testJoinToPrivateChatNotPersisted() {
    //init
    Application application = AppInstallHandlerTest.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String message ->}
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(Examples.EVENT_BOT_JOIN_PRIVATE_CHAT_PARAMETERS)


    //verify
    vtx { Session s ->
      Operator operator = OperatorQuery.all(s).uniqueResult() as Operator

      assertNotNull operator
      assertNotNull operator.id
      assertNotNull operator.operatorId
      ApplicationTest.assertApplicationsEquals application, operator.application
    }
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Бот подключился в приватный чат. Оператор уже есть в БД . Проверям что:
   * 1. в БД новый оператор не появился
   * 2. в чат куда подключился бот было отправлено сообщение
   */
  void testJoinToPrivateChatPersisted() {
    //init
    Application application = AppInstallHandlerTest.preInstallApplication(db)
    Operator operator = preInstallOperator(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String message ->}
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(Examples.EVENT_BOT_JOIN_PRIVATE_CHAT_PARAMETERS)


    //verify
    vtx { Session s ->
      List<Operator> operators = OperatorQuery.all(s).list()

      assertNotNull operators
      assertEquals 1, operators.size()
      Operator another = operators.get(0)
      OperatorTest.assertOperatorsEquals operator, another
    }

    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Бот подключился в групповой чат. Чата в БД ещё нет. Проверям что:
   * 1. в БД был сохранён чат
   * 2. в этот чат было отправлено сообщение
   */
  void testJoinToGroupChatNotPersisted() {
    //init
    Application application = AppInstallHandlerTest.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String message ->}
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(Examples.EVENT_BOT_JOIN_GROUP_CHAT_PARAMETERS)


    //verify
    vtx { Session s ->
      Chat chat = ChatQuery.all(s).uniqueResult() as Chat

      assertNotNull chat
      assertNotNull chat.id
      assertNotNull chat.chatId
      assertEquals Chat.Type.GROUP, chat.type
      ApplicationTest.assertApplicationsEquals application, chat.application
    }
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Бот подключился в групповой чат. Чат уже есть в БД. Проверям что:
   * 1. в БД новый чат не появился
   * 2. в чат было отправлено сообщение
   */
  void testJoinToGroupChatPersisted() {
    //init
    Application application = AppInstallHandlerTest.preInstallApplication(db)
    Chat chat  = preInstallChat(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String message ->}
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(Examples.EVENT_BOT_JOIN_PRIVATE_CHAT_PARAMETERS)


    //verify
    vtx { Session s ->
      List<Chat> chats = ChatQuery.all(s).list()

      assertNotNull chats
      assertEquals 1, chats.size()
      Chat another = chats.get(0)
      ChatTest.assertChatsEquals chat, another
    }

    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. чат не создавался
   * 2. никаких сообщение не отправлялось
   */
  void testUnknownApplicationInGroupChat() {
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(0) { Application app, String dialogId, String message ->}
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(Examples.EVENT_BOT_JOIN_PRIVATE_CHAT_PARAMETERS)


    //verify
    vtx { Session s ->
      List<Chat> chats = ChatQuery.all(s).list()

      assertNotNull chats
      assertEquals 0, chats.size()
    }

    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testUnknownApplicationInPrivateChat() {
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(0) { Application app, String dialogId, String message ->}
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(Examples.EVENT_BOT_JOIN_PRIVATE_CHAT_PARAMETERS)


    //verify
    vtx { Session s ->
      List<Operator> operators = OperatorQuery.all(s).list()

      assertNotNull operators
      assertEquals 0, operators.size()
    }

    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }
}
