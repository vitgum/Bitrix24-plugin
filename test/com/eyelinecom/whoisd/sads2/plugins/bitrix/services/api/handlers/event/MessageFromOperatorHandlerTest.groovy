package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.LocalizationHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.ModelStateChecker
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ChatController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.OperatorController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.QueueController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor
import groovy.mock.interceptor.MockFor

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.MESSAGE_FROM_OPERATOR_PRIVATE_CHAT
import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.MESSAGE_FROM_OPERATOR_GROUP_CHAT

/**
 * author: Artem Voronov
 */
class MessageFromOperatorHandlerTest extends DBTestBase {

  protected MessageFromOperatorHandler createMessageFromOperatorHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationController applicationController = new ApplicationController(db)
    ChatController chatController = new ChatController(db)
    OperatorController operatorController = new OperatorController(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()
    QueueController queueController = new QueueController(db)
    return new MessageFromOperatorHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController, queueController)
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
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertNoOperators(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }
  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testUnknownApplicationInGroupChat() {
    //init
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_GROUP_CHAT)


    //verify
    ModelStateChecker.assertNoOperators(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Сообщение от оператора в групповом чате. Оператор ещё не был создан в БД. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testMessageFromNotExistingOperatorInGroupChat() {
    //init
    InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_GROUP_CHAT)


    //verify
    ModelStateChecker.assertNoOperators(db)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Сообщение от оператора в групповом чате. Оператор уже есть в БД. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testMessageFromExistingOperatorInGroupChat() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_GROUP_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Сообщение от оператора в приватном чате. Оператора нет в БД. Сеанс общения с не инициирован. Проверям что:
   * 1. оператор был сохранён в БД
   * 2. оператору высылается сообщение
   */
  void testOperatorCreatingNoUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToOperator(1) { Operator opp, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(application.language, "user.start.command"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertOperatorWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Сообщение от оператора в приватном чате. Оператора есть в БД. Сеанс общения с не инициирован. Проверям что:
   * 1. в БД ровно 1 оператор, новый не появлялся
   * 2. оператору высылается сообщение
   */
  void testOperatorExistsNoUsers() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToOperator(1) { Operator opp, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(application.language, "user.start.command"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Сообщение от оператора в приватном чате. Оператор уже есть в БД. Случай когда сеанс общения с пользователем уже инициирован. Проверям что:
   * 1. оператор не создаётся в БД
   * 2. пользователю высылается сообщение
   */
  void testMessageFromExistingOperatorInPrivateChatUserProcessing() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    User user = InitHelper.preInstallUser(db)
    Queue processing = InitHelper.preInstallProcessingQueue(db, application, user, operator)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToUser(1) { Queue que, String msg ->
      final String message = ParamsExtractor.getMessageWithEncoding(MESSAGE_FROM_OPERATOR_PRIVATE_CHAT)
      assertEquals message, msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    MessageFromOperatorHandler messageFromOperatorHandler = createMessageFromOperatorHandler(messageDeliveryProviderDelegate)
    messageFromOperatorHandler.processEvent(MESSAGE_FROM_OPERATOR_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }
}
