package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import org.hibernate.exception.ConstraintViolationException

import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class OperatorTest extends DBTestBase {

  static Operator createCorrectOperator(Map overrides = [:]) {
    def defaultFields = [
      application : ApplicationTest.createCorrectApplication(),
      operatorId : 50
    ]
    return new Operator(defaultFields + overrides)
  }

  void testSaveAndLoad() {
    def operator = createCorrectOperator()

    tx { s ->
      s.save(operator.application)
      s.save(operator)
    }

    assertNotNull(operator.id)

    def operator1 = tx { s -> s.get(Operator, operator.id) as Operator}

    assertNotNull(operator1)
    assertOperatorsEquals(operator, operator1)
  }

  void testDuplicate() {
    def app = ApplicationTest.createCorrectApplication()
    def operator = createCorrectOperator(application: app)
    def same = createCorrectOperator(application: app)
    def another = createCorrectOperator(application: app, operatorId: 51)

    vtx { s ->
      s.save(app)
      s.save(operator)
      s.save(another)
    }

    assertNotNull(operator.id)
    assertNotNull(another.id)

    def msg = shouldFail(ConstraintViolationException) {
      vtx { s ->
        s.save(same)
      }
    }

    assertEquals 'could not execute statement', msg
  }

  static void assertOperatorsEquals(Operator expected, Operator actual) {
    ApplicationTest.assertApplicationsEquals expected.application, actual.application
    assertEquals expected.operatorId, actual.operatorId
  }
}
