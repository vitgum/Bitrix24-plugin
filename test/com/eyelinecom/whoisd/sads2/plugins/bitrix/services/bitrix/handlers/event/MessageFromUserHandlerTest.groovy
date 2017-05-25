package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.IncomeMessage
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.LocalizationHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.ModelStateChecker
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.UserDAO
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor
import groovy.mock.interceptor.MockFor

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.MESSAGE_FROM_USER

/**
 * author: Artem Voronov
 */
class MessageFromUserHandlerTest extends DBTestBase {

  protected MessageFromUserHandler createMessageFromUserHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationDAO applicationController = new ApplicationDAO(db)
    ChatDAO chatController = new ChatDAO(db)
    OperatorDAO operatorController = new OperatorDAO(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()
    QueueDAO queueController = new QueueDAO(db)
    UserDAO userController = new UserDAO(db)
    return new MessageFromUserHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController, queueController, userController)
  }

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. пользователь не содавался
   * 2. очередь не создавалась
   * 3. сообщение пользователя не сохранялось
   * 3. никаких сообщений не отправлялось
   */
  void testUnknownApplication() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromUserHandler messageFromUserHandler = createMessageFromUserHandler(messageDeliveryProviderDelegate)
    messageFromUserHandler.processEvent(MESSAGE_FROM_USER)


    //verify
    ModelStateChecker.assertNoUsers(db)
    ModelStateChecker.assertNoQueues(db)
    ModelStateChecker.assertNoIncomeMessages(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Новый пользователь. Проверям что:
   * 1. создали пользователя в БД
   * 2. создали очередь с этим пользователем в БД
   * 3. сохранили его сообщение
   * 4. во все чаты Битрикс отправили нотификацию
   */
  void testNewUser() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToAllChats(1) { Application app, String msg ->
      assertEquals LocalizationHelper.getNewUserArrivedNotification(app.language, 1, 0), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromUserHandler messageFromUserHandler = createMessageFromUserHandler(messageDeliveryProviderDelegate)
    messageFromUserHandler.processEvent(MESSAGE_FROM_USER)


    //verify
    User user = ModelStateChecker.assertUserWasCreated(db)
    Queue queue = ModelStateChecker.assertAwaitingQueueWasCreated(db, application, user)
    IncomeMessage incomeMessage =  ModelStateChecker.assertIncomeMessageWasCreated(db)
    ModelStateChecker.assertMessageWasStored(db , queue, incomeMessage)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Существующий пользователь, но не в очереди. Проверям что:
   * 1. нового пользователя в БД не заводили
   * 2. создали очередь с этим пользователем в БД
   * 3. сохранили его сообщение
   * 4. во все чаты Битрикс отправили нотификацию
   */
  void testOldUser() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToAllChats(1) { Application app, String msg ->
      assertEquals LocalizationHelper.getNewUserArrivedNotification(app.language, 1, 0), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromUserHandler messageFromUserHandler = createMessageFromUserHandler(messageDeliveryProviderDelegate)
    messageFromUserHandler.processEvent(MESSAGE_FROM_USER)


    //verify
    ModelStateChecker.assertExistsOnlyOneUser(db, user)
    Queue queue = ModelStateChecker.assertAwaitingQueueWasCreated(db, application, user)
    IncomeMessage incomeMessage =  ModelStateChecker.assertIncomeMessageWasCreated(db)
    ModelStateChecker.assertMessageWasStored(db , queue, incomeMessage)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Пользователь уже в очереди типа AWAITING. Проверям что:
   * 1. нового пользователя в БД не заводили
   * 2. новую очередь с этим пользователем не заводили в БД
   * 3. сохранили его сообщение
   * 4. никаких нотификаций не было
   */
  void testAwaiting() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    Queue awaiting = InitHelper.preInstallAwaitingQueue(db, application, user, "SOME_TEXT")
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromUserHandler messageFromUserHandler = createMessageFromUserHandler(messageDeliveryProviderDelegate)
    messageFromUserHandler.processEvent(MESSAGE_FROM_USER)


    //verify
    ModelStateChecker.assertExistsOnlyOneUser(db, user)
    ModelStateChecker.assertExistsOnlyOneQueue(db, awaiting)
    Queue queue = ModelStateChecker.assertAwaitingQueueWasCreated(db, application, user)
    ModelStateChecker.assertMessageWasStored(db , queue, ParamsExtractor.getMessageText(MESSAGE_FROM_USER))
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Пользователь уже в очереди типа PROCESSING. Проверям что:
   * 1. нового пользователя в БД не заводили
   * 2. новую очередь с этим пользователем не заводили в БД
   * 3. сообщение в БД не сохраняли
   * 4. отправили нотификацию Оператору
   */
  void testProcessing() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    User user = InitHelper.preInstallUser(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    Queue processing = InitHelper.preInstallProcessingQueue(db, application, user, operator)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToOperator(1) { Operator opp, String msg ->
      assertEquals ParamsExtractor.getMessageText(MESSAGE_FROM_USER), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromUserHandler messageFromUserHandler = createMessageFromUserHandler(messageDeliveryProviderDelegate)
    messageFromUserHandler.processEvent(MESSAGE_FROM_USER)


    //verify
    ModelStateChecker.assertExistsOnlyOneUser(db, user)
    ModelStateChecker.assertExistsOnlyOneQueue(db, processing)
    ModelStateChecker.assertNoIncomeMessages(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }
}
