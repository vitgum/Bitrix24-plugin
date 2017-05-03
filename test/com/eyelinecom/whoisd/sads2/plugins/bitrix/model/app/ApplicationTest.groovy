package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import org.hibernate.exception.ConstraintViolationException

import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class ApplicationTest extends DBTestBase {

  static Application createCorrectApplication(Map overrides = [:]) {
    def defaultFields = [
      domain : 'company.bitrix24.ru',
      accessToken : 'TOKEN',
      refreshToken : 'TOKEN',
      botId : 56,
      language: "ru",
      deleted : false
    ]
    return new Application(defaultFields + overrides)
  }

  void testSaveAndLoad() {
    def application = createCorrectApplication()

    tx { s ->
      s.save(application)
    }

    assertNotNull(application.id)

    def application1 = tx { s -> s.get(Application, application.id) as Application}

    assertNotNull(application1)
    assertApplicationsEquals(application, application1)
  }

  void testDuplicate() {
    def app = createCorrectApplication()
    def deleted1 = createCorrectApplication(deleted: true)
    def deleted2 = createCorrectApplication(deleted: true)

    vtx { s ->
      s.save(deleted1)
      s.save(deleted2)
      s.save(app)
    }

    assertNotNull(deleted1.id)
    assertNotNull(deleted2.id)
    assertNotNull(app.id)
  }

  static void assertApplicationsEquals(Application expected, Application actual) {
    assertEquals expected.domain, actual.domain
    assertEquals expected.accessToken, actual.accessToken
    assertEquals expected.refreshToken, actual.refreshToken
    assertEquals expected.botId, actual.botId
    assertEquals expected.deleted, actual.deleted
    assertEquals expected.language, actual.language
  }
}
