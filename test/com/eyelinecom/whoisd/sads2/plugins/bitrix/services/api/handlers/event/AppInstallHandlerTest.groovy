package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.BitrixApiClient
import groovy.mock.interceptor.MockFor
import org.junit.Ignore

/**
 * author: Artem Voronov
 */
@Ignore
class AppInstallHandlerTest extends DBTestBase{

  void testAppInstallEvent() {
    //1. проверить, что BitrixApiClient отправил запрос на регистрацию бота
    //2. проверить вызов что в БД создалось приложение

    MockFor bitrixApiClientMock = new MockFor(BitrixApiClient)
    bitrixApiClientMock.demand.createBot(1) {}
    BitrixApiClient bitrixApiClientDelegate = bitrixApiClientMock.proxyDelegateInstance()



    bitrixApiClientMock.verify(bitrixApiClientDelegate)
  }
  void testReinstallEvent() {
    //1. проверить, что BitrixApiClient отправил запрос на удаление бота
    //2. проверить вызов что в БД удалилось приложение
    //3. проверить, что BitrixApiClient отправил запрос на регистрацию бота
    //4. проверить вызов что в БД создалось приложение
  }
}
