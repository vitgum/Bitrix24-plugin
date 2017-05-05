package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.BitrixApiProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.InitHelper
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ApplicationQuery
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor
import groovy.mock.interceptor.MockFor
import org.hibernate.Session

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.APP_INSTALL
import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */

class AppInstallHandlerTest extends DBTestBase {

  protected AppInstallHandler createAppInstallHandler(BitrixApiProvider bitrixApiProvider) {
    ApplicationController applicationController = new ApplicationController(db)
    return new AppInstallHandler(applicationController, bitrixApiProvider)
  }

  /**
   * Первичная установка приложения. Проверям что:
   * 1. в Битрикс был отправлен запрос на регистрацию бота
   * 2. в Битрикс был отправлен запрос на регистрацию команд к боту
   * 3. в БД создалось приложение
   */
  void testAppInstallEvent() {
    //init
    MockFor bitrixApiProviderMock = new MockFor(BitrixApiProvider)
    bitrixApiProviderMock.demand.createBot(1) { String domain, String accessToken, String refreshToken -> 666}
    bitrixApiProviderMock.demand.addBotCommands(1) { Application app -> }
    BitrixApiProvider bitrixApiProviderDelegate = bitrixApiProviderMock.proxyDelegateInstance()


    //act
    AppInstallHandler appInstallHandler = createAppInstallHandler(bitrixApiProviderDelegate)
    appInstallHandler.processEvent(APP_INSTALL)


    //verify
    bitrixApiProviderMock.verify(bitrixApiProviderDelegate)
    vtx { Session s ->
      String domain = ParamsExtractor.getDomain(APP_INSTALL)
      String language = ParamsExtractor.getLanguage(APP_INSTALL)
      String refreshToken = ParamsExtractor.getRefreshToken(APP_INSTALL)
      String accessToken = ParamsExtractor.getAccessToken(APP_INSTALL)
      Application application = ApplicationQuery.byDomain(domain, s).uniqueResult() as Application

      assertNotNull application
      assertEquals domain, application.domain
      assertEquals language, application.language
      assertEquals refreshToken, application.refreshToken
      assertEquals accessToken, application.accessToken
      assertEquals 666, application.botId
      assertFalse application.deleted
    }
  }

    /**
     * Повторная установка приложения. Проверям что:
     * 1. в Битрикс был отправлен запрос на удаление бота
     * 2. в БД удалилось приложение
     * 3. в Битрикс был отправлен запрос на регистрацию бота
     * 4. в Битрикс был отправлен запрос на регистрацию команд к боту
     * 5. в БД создалось приложение
     */
  void testReinstallEvent() {
    //init
    Application application = InitHelper.preInstallApplication(db)
    MockFor bitrixApiProviderMock = new MockFor(BitrixApiProvider)
    bitrixApiProviderMock.demand.deleteBot(1) { Application app -> }
    bitrixApiProviderMock.demand.createBot(1) { String d, String a, String r -> 666}
    bitrixApiProviderMock.demand.addBotCommands(1) { Application app -> }
    BitrixApiProvider bitrixApiProdiverDelegate = bitrixApiProviderMock.proxyDelegateInstance()


    //act
    AppInstallHandler appInstallHandler = createAppInstallHandler(bitrixApiProdiverDelegate)
    appInstallHandler.processEvent(APP_INSTALL)


    //verify
    assertNotNull(application.id)
    bitrixApiProviderMock.verify(bitrixApiProdiverDelegate)
    vtx { Session s ->

      Application oldApp = ApplicationQuery.byId(application.id, s).uniqueResult() as Application

      assertNotNull oldApp
      assertTrue oldApp.deleted

      Application newApp = ApplicationQuery.byDomain(application.domain, s).uniqueResult() as Application

      assertNotNull newApp
      assertEquals application.domain, newApp.domain
      assertEquals application.language, newApp.language
      assertEquals application.refreshToken, newApp.refreshToken
      assertEquals application.accessToken, newApp.accessToken
      assertEquals 666, newApp.botId
      assertFalse newApp.deleted
    }
  }
}
