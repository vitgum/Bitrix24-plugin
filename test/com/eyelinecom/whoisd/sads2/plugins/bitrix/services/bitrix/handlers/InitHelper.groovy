package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.handlers

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.ChatTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.IncomeMessageTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.OperatorTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.UserTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor

/**
 * author: Artem Voronov
 */
class InitHelper {

  static Application preInstallApplication(DBService dbService) {
    String domain = ParamsExtractor.getDomain(EventParametersExamples.APP_INSTALL)
    String language = ParamsExtractor.getLanguage(EventParametersExamples.APP_INSTALL)
    String refreshToken = ParamsExtractor.getRefreshToken(EventParametersExamples.APP_INSTALL)
    String accessToken = ParamsExtractor.getAccessToken(EventParametersExamples.APP_INSTALL)
    def application = ApplicationTest.createCorrectApplication(domain: domain, language: language, refreshToken: refreshToken, accessToken: accessToken)

    dbService.tx { s -> s.save(application) }

    return application
  }

  static Chat preInstallChat(DBService dbService, Application application) {
    Chat.Type chatType = ParamsExtractor.getChatType(EventParametersExamples.BOT_JOIN_GROUP_CHAT)
    int chatId = ParamsExtractor.getChatId(EventParametersExamples.BOT_JOIN_GROUP_CHAT, chatType)
    def chat = ChatTest.createCorrectChat(application: application, chatId: chatId, type: chatType)

    dbService.tx { s -> s.save(chat) }

    return chat
  }

  static Operator preInstallOperator(DBService dbService, Application application) {
    int operatorId = ParamsExtractor.getOperatorId(EventParametersExamples.BOT_JOIN_PRIVATE_CHAT)
    def chat = OperatorTest.createCorrectOperator(application: application, operatorId: operatorId)

    dbService.tx { s -> s.save(chat) }

    return chat
  }

  static Queue preInstallAwaitingQueue(DBService dbService, Application application, User user, String userMessage) {
    def language = ParamsExtractor.getLanguage(EventParametersExamples.MESSAGE_FROM_USER)
    def serviceId = ParamsExtractor.getServiceId(EventParametersExamples.MESSAGE_FROM_USER)
    def protocol = ParamsExtractor.getProtocol(EventParametersExamples.MESSAGE_FROM_USER)
    def redirectBackPage = ParamsExtractor.getRedirectBackPageUrl(EventParametersExamples.MESSAGE_FROM_USER);
    def queue = QueueTest.createCorrectQueue(type: QueueType.AWAITING, application: application, user: user, protocol: protocol, serviceId: serviceId, language: language, backPage: redirectBackPage)

    dbService.tx { s -> s.save(queue) }

    def incomeMessage = IncomeMessageTest.createIncomeMessage(text: userMessage, queue: queue)

    dbService.tx { s -> s.save(incomeMessage) }

    return queue

  }

  static Queue preInstallProcessingQueue(DBService dbService, Application application, User user, Operator operator) {
    def language = ParamsExtractor.getLanguage(EventParametersExamples.MESSAGE_FROM_USER)
    def serviceId = ParamsExtractor.getServiceId(EventParametersExamples.MESSAGE_FROM_USER)
    def protocol = ParamsExtractor.getProtocol(EventParametersExamples.MESSAGE_FROM_USER)
    def redirectBackPage = ParamsExtractor.getRedirectBackPageUrl(EventParametersExamples.MESSAGE_FROM_USER);
    def queue = QueueTest.createCorrectQueue(type: QueueType.PROCESSING, application: application, user: user, protocol: protocol, serviceId: serviceId, language: language, backPage: redirectBackPage, operator: operator)

    dbService.tx { s -> s.save(queue) }

    return queue
  }

  static User preInstallUser(DBService dbService) {
    def userId = ParamsExtractor.getUserId(EventParametersExamples.MESSAGE_FROM_USER)
    def user = UserTest.createCorrectUser(userId: userId)

    dbService.tx { s -> s.save(user) }

    return user
  }
}
