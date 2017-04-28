package com.eyelinecom.whoisd.sads2.plugins.bitrix.services;

import com.eyeline.utils.ThreadFactoryWithCounter;
import com.eyeline.utils.config.ConfigException;
import com.eyeline.utils.config.xml.XmlConfig;
import com.eyeline.utils.config.xml.XmlConfigSection;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.BitrixApiClient;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Services {

  private final DBService dbService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final BitrixApiClient bitrixApiClient;
  private final ApplicationController applicationController;
  private final UserController userController;
  private final QueueController queueController;
  private final MessageDeliveryService messageDeliveryService;
  private final ChatController chatController;
  private final OperatorController operatorController;
  private final ResourceBundleController resourceBundleController;

  public Services(XmlConfig config) throws ServicesException {
    this.dbService = initDBService(config);
    this.scheduledExecutorService = initScheduledExecutorService(config);
    this.messageDeliveryService = new MessageDeliveryService();
    this.bitrixApiClient = initBitrixApiClient(config, scheduledExecutorService);
    this.applicationController = new ApplicationController(dbService);
    this.userController = new UserController(dbService);
    this.queueController = new QueueController(dbService);
    this.chatController = new ChatController(dbService);
    this.operatorController = new OperatorController(dbService);
    this.resourceBundleController = new ResourceBundleController();
  }

  private DBService initDBService(XmlConfig config) throws ServicesException {
    try {
      Properties hibernateProperties = config.getSection("db").toProperties(".");
      return new DBService(hibernateProperties);
    }
    catch(ConfigException e) {
      throw new ServicesException("Error during DBService initialization.", e);
    }
  }

  private BitrixApiClient initBitrixApiClient(XmlConfig config, ScheduledExecutorService scheduledExecutorService) throws ServicesException {
    try {
      String callbackUrl = config.getString("deploy.url");

      XmlConfigSection apiClientSection = config.getSection("bitrix.api.client");
      String appId = apiClientSection.getString("bitrix.api.client.app.id");
      String appSecret = apiClientSection.getString("bitrix.api.client.app.secret");
      String appCode = apiClientSection.getString("bitrix.api.client.app.code");
      int connectTimeout = apiClientSection.getInt("bitrix.api.client.connectTimeout.millis");
      int requestTimeout = apiClientSection.getInt("bitrix.api.client.requestTimeout.millis");
      String botName = apiClientSection.getString("bitrix.api.client.bot.name");
      long notificationTimeoutInSeconds = apiClientSection.getLong("bitrix.api.client.notification.timeout.seconds");
      return new BitrixApiClient(scheduledExecutorService, appId, appSecret, appCode, callbackUrl, botName, connectTimeout, requestTimeout, notificationTimeoutInSeconds);
    }
    catch(ConfigException e) {
      throw new ServicesException("Error during BitrixApiClient initialization.", e);
    }
  }

  private ScheduledExecutorService initScheduledExecutorService(XmlConfig config) throws ServicesException {
    try {
      XmlConfigSection threadPoolSection = config.getSection("thread.pool");
      int corePoolSize = threadPoolSection.getInt("thread.pool.core.pool.size");
      int maximumPoolSize = threadPoolSection.getInt("thread.pool.maximum.pool.size");
      long keepAliveTime = threadPoolSection.getInt("thread.pool.keep.alive.time.sec");

      ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(corePoolSize, new ThreadFactoryWithCounter("ServicesWorker-", 0));
      executor.setMaximumPoolSize(maximumPoolSize);
      executor.setKeepAliveTime(keepAliveTime, TimeUnit.SECONDS);

      return executor;
    } catch (ConfigException e) {
      throw new ServicesException("Error during ScheduledExecutorService initialization", e);
    }
  }

  public DBService getDbService() {
    return dbService;
  }

  public BitrixApiClient getBitrixApiClient() {
    return bitrixApiClient;
  }

  public MessageDeliveryService getMessageDeliveryService() {
    return messageDeliveryService;
  }

  public ApplicationController getApplicationController() {
    return applicationController;
  }

  public UserController getUserController() {
    return userController;
  }

  public QueueController getQueueController() {
    return queueController;
  }

  public ChatController getChatController() {
    return chatController;
  }

  public OperatorController getOperatorController() {
    return operatorController;
  }

  public ResourceBundleController getResourceBundleController() {
    return resourceBundleController;
  }

  public void shutdown() {
    dbService.shutdown();
    scheduledExecutorService.shutdown();
  }

}
