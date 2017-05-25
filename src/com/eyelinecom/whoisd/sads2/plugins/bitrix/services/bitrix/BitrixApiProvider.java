package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;

/**
 * author: Artem Voronov
 */
public interface BitrixApiProvider {
  int createBot(String domain, String accessToken, String refreshToken);
  void deleteBot(Application application);
  void addBotCommands(Application application);
  void sendMessage(Application application, String dialogId, String message);
  void sendImage(Application application, String dialogId, String imageUrl);
}
