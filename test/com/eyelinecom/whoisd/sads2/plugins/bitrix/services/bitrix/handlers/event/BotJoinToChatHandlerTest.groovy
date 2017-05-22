package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.LocalizationHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.ModelStateChecker
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import groovy.mock.interceptor.MockFor

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.BOT_JOIN_GROUP_CHAT
import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.BOT_JOIN_PRIVATE_CHAT

/**
 * author: Artem Voronov
 */
class BotJoinToChatHandlerTest extends DBTestBase {

  protected BotJoinToChatHandler createBotJoinToChatHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationDAO applicationController = new ApplicationDAO(db)
    ChatDAO chatController = new ChatDAO(db)
    OperatorDAO operatorController = new OperatorDAO(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()

    return new BotJoinToChatHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController)
  }

  /**
   * Бот подключился в приватный чат. Оператора в БД ещё нет. Проверям что:
   * 1. в БД был сохранён оператор
   * 2. в чат куда подключился бот было отправлено сообщение
   */
  void testJoinToPrivateChatNotPersisted() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "welcome"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(BOT_JOIN_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertOperatorWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Бот подключился в приватный чат. Оператор уже есть в БД . Проверям что:
   * 1. в БД новый оператор не появился
   * 2. в чат куда подключился бот было отправлено сообщение
   */
  void testJoinToPrivateChatPersisted() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "welcome"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(BOT_JOIN_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Бот подключился в групповой чат. Чата в БД ещё нет. Проверям что:
   * 1. в БД был сохранён чат
   * 2. в этот чат было отправлено сообщение
   */
  void testJoinToGroupChatNotPersisted() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "welcome"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(BOT_JOIN_GROUP_CHAT)


    //verify
    ModelStateChecker.assertChatWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Бот подключился в групповой чат. Чат уже есть в БД. Проверям что:
   * 1. в БД новый чат не появился
   * 2. в чат было отправлено сообщение
   */
  void testJoinToGroupChatPersisted() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Chat chat = InitHelper.preInstallChat(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "welcome"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(BOT_JOIN_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneChat(db, chat)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. чат не создавался
   * 2. никаких сообщение не отправлялось
   */
  void testUnknownApplicationInGroupChat() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(BOT_JOIN_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertNoChats(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testUnknownApplicationInPrivateChat() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    BotJoinToChatHandler botJoinToChatHandler = createBotJoinToChatHandler(messageDeliveryProviderDelegate)
    botJoinToChatHandler.processEvent(BOT_JOIN_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertNoOperators(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }
}
