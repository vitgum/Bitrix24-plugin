package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.LocalizationHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.ModelStateChecker
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import groovy.mock.interceptor.MockFor

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.STOP_COMMAND_GROUP_CHAT
import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.STOP_COMMAND_PRIVATE_CHAT

/**
 * author: Artem Voronov
 */
class StopHandlerTest extends DBTestBase {

  protected StopHandler createStopHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationController applicationController = new ApplicationController(db)
    ChatController chatController = new ChatController(db)
    OperatorController operatorController = new OperatorController(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()
    QueueController queueController = new QueueController(db)
    return new StopHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController, queueController)
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
    StopHandler stopHandler = createStopHandler(messageDeliveryProviderDelegate)
    stopHandler.handle(STOP_COMMAND_GROUP_CHAT)


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
    StopHandler stopHandler = createStopHandler(messageDeliveryProviderDelegate)
    stopHandler.handle(STOP_COMMAND_PRIVATE_CHAT)


    //verify
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Команду выполнили в групповом чате. Проверям что:
   * 1. оператор не создавался
   * 2. отправили уведомление в чат
   */
  void testOnlyForPrivateChats() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language,"only.for.private.chats"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    StopHandler stopHandler = createStopHandler(messageDeliveryProviderDelegate)
    stopHandler.handle(STOP_COMMAND_GROUP_CHAT)


    //verify
    ModelStateChecker.assertNoOperators(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Нет ожидающих пользователей. Проверям что:
   * 1. оператор не создавался
   * 2. отправили уведомление оператору
   */
  void testNoUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    Queue awaiting = InitHelper.preInstallAwaitingQueue(db, application, user, "SOME_TEXT")
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToOperator(1) { Operator opp, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(application.language,"user.not.flushed"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    StopHandler stopHandler = createStopHandler(messageDeliveryProviderDelegate)
    stopHandler.handle(STOP_COMMAND_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Нет ожидающих пользователей. Проверям что:
   * 1. оператор не создавался
   * 2. очередь удалилась
   * 3. отправили уведомление оператору
   * 4. отправили уведомление пользователю
   */
  void testStopProcessing() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    User user = InitHelper.preInstallUser(db)
    Queue processing = InitHelper.preInstallProcessingQueue(db, application, user, operator)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToUser(1) { Queue que, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(que.language,"operator.quit"), msg
    }
    messageDeliveryProviderMock.demand.sendMessageToOperator(1) { Operator opp, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(application.language,"user.flushed"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    StopHandler stopHandler = createStopHandler(messageDeliveryProviderDelegate)
    stopHandler.handle(STOP_COMMAND_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    ModelStateChecker.assertNoQueues(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }
}
