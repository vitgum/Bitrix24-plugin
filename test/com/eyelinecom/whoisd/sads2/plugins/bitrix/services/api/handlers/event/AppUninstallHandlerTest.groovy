package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.BitrixApiProvider
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ApplicationQuery
import groovy.mock.interceptor.MockFor
import org.hibernate.Session

import static com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.EventParametersExamples.APP_INSTALL
import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class AppUninstallHandlerTest extends DBTestBase {

  protected AppUninstallHandler createAppUninstallHandler(BitrixApiProvider bitrixApiProvider) {
    ApplicationController applicationController = new ApplicationController(db)
    return new AppUninstallHandler(applicationController, bitrixApiProvider)
  }

  /**
   * Повторная установка приложения. Проверям что:
   * 1. в Битрикс был отправлен запрос на удаление бота
   * 2. в БД удалилось приложение
   */
  void testAppUninstallEvent() {
    //init
    def application = ApplicationTest.createCorrectApplication()
    tx { s -> s.save(application) }
    MockFor bitrixApiProviderMock = new MockFor(BitrixApiProvider)
    bitrixApiProviderMock.demand.deleteBot(1) { Application app -> }
    BitrixApiProvider bitrixApiProdiverDelegate = bitrixApiProviderMock.proxyDelegateInstance()


    //act
    AppUninstallHandler appUninstallHandler = createAppUninstallHandler(bitrixApiProdiverDelegate)
    appUninstallHandler.processEvent(APP_INSTALL)


    //verify
    assertNotNull(application.id)
    bitrixApiProviderMock.verify(bitrixApiProdiverDelegate)
    vtx { Session s ->
      Application oldApp = ApplicationQuery.byId(application.id, s).uniqueResult() as Application

      assertNotNull oldApp
      assertTrue oldApp.deleted
    }
  }
}
