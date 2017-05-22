package com.eyelinecom.whoisd.sads2.plugins.bitrix.api;

/**
 * author: Artem Voronov
 */
public interface TokenUpdater {

  /**
   * @param domain
   * @param newAccessToken
   * @param newRefreshToken
   */
  void update(String domain, String newAccessToken, String newRefreshToken);
}
