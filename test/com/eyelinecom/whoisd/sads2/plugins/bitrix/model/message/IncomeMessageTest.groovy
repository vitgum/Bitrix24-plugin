package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueTest

import static org.junit.Assert.assertNotNull

/**
 * author: Artem Voronov
 */
class IncomeMessageTest extends DBTestBase {

  static IncomeMessage createIncomeMessage(Map overrides = [:]) {
    def defaultFields = [
      text : 'hello',
      type : MessageType.TEXT,
      queue : QueueTest.createCorrectQueue()
    ]
    return new IncomeMessage(defaultFields + overrides)
  }

  void testSaveAndLoad() {
    def msg = createIncomeMessage()

    tx { s ->
      s.save(msg.queue.application)
      s.save(msg.queue.user)
      s.save(msg.queue)
      s.save(msg)
    }

    assertNotNull(msg.id)

    def msg1 = tx { s -> s.get(IncomeMessage, msg.id) as IncomeMessage}

    assertNotNull(msg1)
    assertIncomeMessagesEquals(msg, msg1)
  }



  static void assertIncomeMessagesEquals(IncomeMessage expected, IncomeMessage actual) {
    assertEquals expected.text, actual.text
    assertEquals expected.type, actual.type
    assertEquals expected.imageUrl, actual.imageUrl
    QueueTest.assertQueuesEquals expected.queue, actual.queue
  }
}
