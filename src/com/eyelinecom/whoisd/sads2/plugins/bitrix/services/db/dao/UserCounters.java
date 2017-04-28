package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

/**
 * author: Artem Voronov
 */
public class UserCounters {
  private final int awaitingUsersCount;
  private final int processingUsersCount;

  public UserCounters(int awaitingUsersCount, int processingUsersCount) {
    this.awaitingUsersCount = awaitingUsersCount;
    this.processingUsersCount = processingUsersCount;
  }

  public int getAwaitingUsersCount() {
    return awaitingUsersCount;
  }

  public int getProcessingUsersCount() {
    return processingUsersCount;
  }

  public boolean hasAwaitingUsers() {
    return awaitingUsersCount > 0;
  }
}
