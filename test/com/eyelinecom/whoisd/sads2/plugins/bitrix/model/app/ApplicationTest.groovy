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
    def same = createCorrectApplication()
    def deleted = createCorrectApplication(deleted: true)

    vtx { s ->
      s.save(deleted)
      s.save(app)
    }

    assertNotNull(deleted.id)
    assertNotNull(app.id)

    def msg = shouldFail(ConstraintViolationException) {
      vtx { s ->
        s.save(same)
      }
    }

    assertEquals 'could not execute statement', msg
  }

  static void assertApplicationsEquals(Application expected, Application actual) {
    assertEquals expected.domain, actual.domain
    assertEquals expected.accessToken, actual.accessToken
    assertEquals expected.refreshToken, actual.refreshToken
    assertEquals expected.botId, actual.botId
    assertEquals expected.deleted, actual.deleted
  }
}
