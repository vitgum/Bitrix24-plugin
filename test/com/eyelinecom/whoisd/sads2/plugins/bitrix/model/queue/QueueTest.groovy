package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.ApplicationTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.OperatorTest
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.UserTest
import org.hibernate.exception.ConstraintViolationException

import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class QueueTest extends DBTestBase {

  static Queue createCorrectQueue(Map overrides = [:]) {
    def defaultFields = [
      type : QueueType.AWAITING,
      application : ApplicationTest.createCorrectApplication(),
      user : UserTest.createCorrectUser(),
      protocol : 'telegram',
      serviceId : 'lk.64.1492410911647',
      language : 'ru',
      backPage : 'http://hosting-api-test.eyeline.mobi/index?sid=660&sver=5&pid=2&answer=back_page'
    ]
    return new Queue(defaultFields + overrides)
  }

  void testSaveAndLoad() {
    def queue = createCorrectQueue()

    tx { s ->
      s.save(queue.application)
      s.save(queue.user)
      s.save(queue)
    }

    assertNotNull(queue.id)

    def queue1 = tx { s -> s.get(Queue, queue.id) as Queue}

    assertNotNull(queue1)
    assertQueuesEquals(queue, queue1)
  }

  void testDuplicate() {
    def app = ApplicationTest.createCorrectApplication()
    def user = UserTest.createCorrectUser()
    def queue = createCorrectQueue(application: app, user: user)
    def same = createCorrectQueue(application: app, user: user)
    def another = createCorrectQueue(application: app, user: user,  serviceId : 'SERVICE_ID')

    vtx { s ->
      s.save(app)
      s.save(user)
      s.save(queue)
      s.save(another)
    }

    assertNotNull(queue.id)
    assertNotNull(another.id)

    def msg = shouldFail(ConstraintViolationException) {
      vtx { s ->
        s.save(same)
      }
    }

    assertEquals 'could not execute statement', msg
  }

  static void assertQueuesEquals(Queue expected, Queue actual) {
    ApplicationTest.assertApplicationsEquals expected.application, actual.application
    UserTest.assertUsersEquals expected.user, actual.user

    if (expected.operator && actual.operator)
      OperatorTest.assertOperatorsEquals expected.operator, actual.operator

    assertEquals expected.protocol, actual.protocol
    assertEquals expected.serviceId, actual.serviceId
    assertEquals expected.backPage, actual.backPage
    assertEquals expected.language, actual.language
  }
}
