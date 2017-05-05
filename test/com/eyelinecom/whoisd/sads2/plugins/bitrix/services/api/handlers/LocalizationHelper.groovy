package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers

import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController

/**
 * author: Artem Voronov
 */
class LocalizationHelper {

  private static ResourceBundleController resourceBundleController = new ResourceBundleController();

  static String getLocalizedMessage(String lang, String key, String ... args) {
    return resourceBundleController.getMessage(lang, key, args)
  }

  static String getUserCountNotification(String lang, int awaitingCount, int processingCount ) {
    return getLocalizedMessage(lang,"awaiting.users", awaitingCount + "") + "\n" +
      getLocalizedMessage(lang,"processing.users", processingCount + "")
  }

  static String getNewUserArrivedNotification(String lang, int awaitingCount, int processingCount ) {
    return getLocalizedMessage(lang,"new.user.arrived") + "\n" +
      getLocalizedMessage(lang,"awaiting.users", awaitingCount + "") + "\n" +
      getLocalizedMessage(lang,"processing.users", processingCount + "")
  }
}
