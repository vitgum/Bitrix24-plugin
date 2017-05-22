package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.command

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

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.HELP_COMMAND_GROUP_CHAT
import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers.EventParametersExamples.HELP_COMMAND_PRIVATE_CHAT

/**
 * author: Artem Voronov
 */
class HelpHandlerTest extends DBTestBase {

  protected HelpHandler createHelpHandler(MessageDeliveryProvider messageDeliveryProvider) {
    ApplicationDAO applicationController = new ApplicationDAO(db)
    ChatDAO chatController = new ChatDAO(db)
    OperatorDAO operatorController = new OperatorDAO(db)
    ResourceBundleController resourceBundleController = new ResourceBundleController()
    return new HelpHandler(chatController, operatorController, messageDeliveryProvider, resourceBundleController, applicationController)
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
    HelpHandler helpHandler = createHelpHandler(messageDeliveryProviderDelegate)
    helpHandler.handle(HELP_COMMAND_GROUP_CHAT)


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
    HelpHandler helpHandler = createHelpHandler(messageDeliveryProviderDelegate)
    helpHandler.handle(HELP_COMMAND_PRIVATE_CHAT)


    //verify
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Чат не был в БД. Проверям что:
   * 1. создали чат в бд
   * 2. отправили сообщение в чат
   */
  void testCreateChat() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "help.text"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    HelpHandler helpHandler = createHelpHandler(messageDeliveryProviderDelegate)
    helpHandler.handle(HELP_COMMAND_GROUP_CHAT)


    //verify
    ModelStateChecker.assertChatWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Оператора не было в БД. Проверям что:
   * 1. создали оператора в бд
   * 2. отправили сообщение в чат (оператору)
   */
  void testCreateOperator() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "help.text"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    HelpHandler helpHandler = createHelpHandler(messageDeliveryProviderDelegate)
    helpHandler.handle(HELP_COMMAND_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertOperatorWasCreated(db, application)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }

  /**
   * Чат был в БД. Проверям что:
   * 1. новый чат в бд не сохраняли
   * 2. отправили сообщение в чат
   */
  void testExistingChat() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Chat chat = InitHelper.preInstallChat(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "help.text"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    HelpHandler helpHandler = createHelpHandler(messageDeliveryProviderDelegate)
    helpHandler.handle(HELP_COMMAND_GROUP_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneChat(db, chat)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)

  }

  /**
   * Оператора был в БД. Проверям что:
   * 1. нового оператора в бд не создавали
   * 2. отпрравили сообщение в чат (оператору)
   */
  void testExistingOperator() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    Operator operator = InitHelper.preInstallOperator(db, application)
    MockFor messageDeliveryProviderMock = new MockFor(MessageDeliveryProvider)
    messageDeliveryProviderMock.demand.sendMessageToChat(1) { Application app, String dialogId, String msg ->
      assertEquals LocalizationHelper.getLocalizedMessage(app.language, "help.text"), msg
    }
    MessageDeliveryProvider messageDeliveryProviderDelegate = messageDeliveryProviderMock.proxyDelegateInstance()


    //act
    HelpHandler helpHandler = createHelpHandler(messageDeliveryProviderDelegate)
    helpHandler.handle(HELP_COMMAND_PRIVATE_CHAT)


    //verify
    ModelStateChecker.assertExistsOnlyOneOperator(db, operator)
    messageDeliveryProviderMock.verify(messageDeliveryProviderDelegate)
  }


}
