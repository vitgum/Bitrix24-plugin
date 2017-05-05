package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.LocalizationHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.ModelStateChecker
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import groovy.mock.interceptor.MockFor

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.USER_STOP_MESSAGING

/**
 * author: Artem Voronov
 */
class UserStopMessagingHandlerTest extends DBTestBase {

  protected UserStopMessagingHandler createUserStopMessagingHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationController applicationController = new ApplicationController(db)
    ChatController chatController = new ChatController(db)
    OperatorController operatorController = new OperatorController(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()
    QueueController queueController = new QueueController(db)
    UserController userController = new UserController(db)
    return new UserStopMessagingHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController, queueController, userController)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. никаких сообщений не отправлялось
   */
  void testUnknownApplication() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    UserStopMessagingHandler userStopMessagingHandler = createUserStopMessagingHandler(messageDeliveryProviderDelegate)
    userStopMessagingHandler.processEvent(USER_STOP_MESSAGING)


    //verify
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Пользователь выходит уже после того как удалили очередь (оператор раньше прервал сеанс). Проверям что:
   * 1. никаких сообщений не отправлялось
   */
  void testNoProcessingQueue() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    Queue awaiting = InitHelper.preInstallAwaitingQueue(db, application, user, "SOME_TEXT")
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    UserStopMessagingHandler userStopMessagingHandler = createUserStopMessagingHandler(messageDeliveryProviderDelegate)
    userStopMessagingHandler.processEvent(USER_STOP_MESSAGING)


    //verify
    ModelStateChecker.assertExistsOnlyOneQueue(db, awaiting)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Основной сценарий: пользователь прерывает сеанс общения. Проверям что:
   * 1. удалилась очередь
   * 2. оператору отправили нотификацию
   */
  void testStopMessaging() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    InitHelper.preInstallProcessingQueue(db, application, user, operator)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToOperator(1) { Operator opp, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(application.language, "user.quit"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    UserStopMessagingHandler userStopMessagingHandler = createUserStopMessagingHandler(messageDeliveryProviderDelegate)
    userStopMessagingHandler.processEvent(USER_STOP_MESSAGING)


    //verify
    ModelStateChecker.assertNoQueues(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

}
