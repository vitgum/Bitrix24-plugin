package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.command

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.LocalizationHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.ModelStateChecker
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import groovy.mock.interceptor.MockFor

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.INFO_COMMAND_GROUP_CHAT
import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.INFO_COMMAND_PRIVATE_CHAT

/**
 * author: Artem Voronov
 */
class InfoHandlerTest extends DBTestBase {

  protected InfoHandler createInfoHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationDAO applicationController = new ApplicationDAO(db)
    ChatDAO chatController = new ChatDAO(db)
    OperatorDAO operatorController = new OperatorDAO(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()
    QueueDAO queueController = new QueueDAO(db)
    return new InfoHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController, queueController)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. никаких сообщений не отправлялось
   */
  void testUnknownApplicationGroupChat() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    InfoHandler infoHandler = createInfoHandler(messageDeliveryProviderDelegate)
    infoHandler.handle(INFO_COMMAND_GROUP_CHAT)


    //verify
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. никаких сообщений не отправлялось
   */
  void testUnknownApplicationPrivateChat() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    InfoHandler infoHandler = createInfoHandler(messageDeliveryProviderDelegate)
    infoHandler.handle(INFO_COMMAND_PRIVATE_CHAT)


    //verify
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Чат не был в БД. Проверям что:
   * 1. создали чат в бд
   * 2. отправили сообщение в чат
   */
  void testCreateChatNoUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "no.users"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    InfoHandler infoHandler = createInfoHandler(messageDeliveryProviderDelegate)
    infoHandler.handle(INFO_COMMAND_GROUP_CHAT)


    //verify
    ModelStateChecker.assertChatWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Оператора было в БД. Проверям что:
   * 1. создали оператора в бд
   * 2. отправили сообщение в чат (оператору)
   */
  void testCreateOperatorNoUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "no.users"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    InfoHandler infoHandler = createInfoHandler(messageDeliveryProviderDelegate)
    infoHandler.handle(INFO_COMMAND_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertOperatorWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Чат был в БД. Проверям что:
   * 1. новый чат в бд не сохраняли
   * 2. отправили сообщение в чат
   */
  void testExistingChatAwaitingUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Chat chat = InitHelper.preInstallChat(db, application)
    User user = InitHelper.preInstallUser(db)
     InitHelper.preInstallAwaitingQueue(db, application, user, "SOME_TEXT")
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getUserCountNotification(app.language, 1, 0), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    InfoHandler infoHandler = createInfoHandler(messageDeliveryProviderDelegate)
    infoHandler.handle(INFO_COMMAND_GROUP_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneChat(db, chat)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)

  }

  /**
   * Оператора не было в БД. Проверям что:
   * 1. нового оператора в бд не создавали
   * 2. отпрравили сообщение в чат (оператору)
   */
  void testExistingOperatorProcessingUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    Queue awaiting = InitHelper.preInstallAwaitingQueue(db, application, user, "SOME_TEXT")
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getUserCountNotification(app.language, 1, 0), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    InfoHandler infoHandler = createInfoHandler(messageDeliveryProviderDelegate)
    infoHandler.handle(INFO_COMMAND_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }


}
