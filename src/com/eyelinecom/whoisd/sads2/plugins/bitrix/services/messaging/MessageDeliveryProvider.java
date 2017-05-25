package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;

/**
 * author: Artem Voronov
 */
public interface MessageDeliveryProvider {
  void sendMessageToUser(Queue queue, String message);
  void sendMessageToOperator(Operator operator, String message);
  void sendImageToOperator(Operator operator, String title, String imageUrl);
  void sendMessageToChat(Application application, String dialogId, String message);
  void sendMessageToAllChats(Application application, String message);
}
