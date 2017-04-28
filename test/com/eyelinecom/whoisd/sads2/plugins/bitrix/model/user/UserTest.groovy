package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import org.hibernate.exception.ConstraintViolationException

import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class UserTest extends DBTestBase {

  static User createCorrectUser(Map overrides = [:]) {
    def defaultFields = [
      userId : '5a865fae-f6bf-415e-8657-7a7235105c9e'
    ]
    return new User(defaultFields + overrides)
  }

  void testSaveAndLoad() {
    def user = createCorrectUser()

    tx { s ->
      s.save(user)
    }

    assertNotNull(user.id)

    def user1 = tx { s -> s.get(User, user.id) as User}

    assertNotNull(user1)
    assertUsersEquals(user, user1)
  }

  void testDuplicate() {
    def user1 = createCorrectUser()
    def user2 = createCorrectUser()

    def msg = shouldFail(ConstraintViolationException) {
      tx { s ->
        s.save(user1)
        s.save(user2)
      }
    }

    assertEquals 'could not execute statement', msg
  }

  static void assertUsersEquals(User expected, User actual) {
    assertEquals expected.userId, actual.userId
  }
}
