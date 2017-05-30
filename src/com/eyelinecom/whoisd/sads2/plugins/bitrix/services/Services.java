package com.eyelinecom.whoisd.sads2.plugins.bitrix.services;

import com.eyeline.utils.ThreadFactoryWithCounter;
import com.eyeline.utils.config.ConfigException;
import com.eyeline.utils.config.xml.XmlConfigSection;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.BitrixApiService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.BitrixApiProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;

import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Services {

  private final DBService dbService;
  private final ScheduledExecutorService scheduledExecutorService;
  private final BitrixApiProvider bitrixApiProvider;
  private final ApplicationDAO applicationDAO;
  private final UserDAO userDAO;
  private final QueueDAO queueDAO;
  private final MessageDeliveryProvider messageDeliveryProvider;
  private final ChatDAO chatDAO;
  private final OperatorDAO operatorDAO;
  private final ResourceBundleController resourceBundleController;

  public Services(XmlConfigSection config) throws ServicesException {
    this.dbService = initDBService(config);
    this.scheduledExecutorService = initScheduledExecutorService(config);
    this.applicationDAO = new ApplicationDAO(dbService);
    this.userDAO = new UserDAO(dbService);
    this.queueDAO = new QueueDAO(dbService);
    this.chatDAO = new ChatDAO(dbService);
    this.operatorDAO = new OperatorDAO(dbService);
    this.resourceBundleController = new ResourceBundleController();
    this.bitrixApiProvider = initBitrixApiService(config, scheduledExecutorService, applicationDAO);
    this.messageDeliveryProvider = new MessageDeliveryService(bitrixApiProvider, chatDAO);
  }

  private DBService initDBService(XmlConfigSection config) throws ServicesException {
    try {
      Properties hibernateProperties = config.getSection("db").toProperties(".");
      String hibernateAddCfg = "/" + DBService.class.getPackage().getName().replace('.', '/') + "/hibernate.cfg.xml";
      return new DBService(hibernateProperties, hibernateAddCfg);
    }
    catch(ConfigException e) {
      throw new ServicesException("Error during DBService initialization.", e);
    }
  }

  private BitrixApiService initBitrixApiService(XmlConfigSection config, ScheduledExecutorService scheduledExecutorService, ApplicationDAO applicationDAO) throws ServicesException {
    try {
      String callbackUrl = config.getString("deploy.url");

      XmlConfigSection apiClientSection = config.getSection("bitrix.api.client");
      String appId = apiClientSection.getString("bitrix.api.client.app.id");
      String appSecret = apiClientSection.getString("bitrix.api.client.app.secret");
      String appCode = apiClientSection.getString("bitrix.api.client.app.code");
      int connectTimeout = apiClientSection.getInt("bitrix.api.client.connectTimeout.millis");
      int requestTimeout = apiClientSection.getInt("bitrix.api.client.requestTimeout.millis");
      String botName = apiClientSection.getString("bitrix.api.client.bot.name");
      String botAvatarPath = apiClientSection.getString("bitrix.api.client.bot.avatar.path");
      long notificationTimeoutInSeconds = apiClientSection.getLong("bitrix.api.client.notification.timeout.seconds");
      return new BitrixApiService(scheduledExecutorService, appId, appSecret, appCode, callbackUrl, botName, botAvatarPath, connectTimeout, requestTimeout, notificationTimeoutInSeconds, applicationDAO);
    }
    catch(ConfigException e) {
      throw new ServicesException("Error during BitrixApiService initialization.", e);
    }
  }

  private ScheduledExecutorService initScheduledExecutorService(XmlConfigSection config) throws ServicesException {
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

  public BitrixApiProvider getBitrixApiProvider() {
    return bitrixApiProvider;
  }

  public MessageDeliveryProvider getMessageDeliveryProvider() {
    return messageDeliveryProvider;
  }

  public ApplicationDAO getApplicationDAO() {
    return applicationDAO;
  }

  public UserDAO getUserDAO() {
    return userDAO;
  }

  public QueueDAO getQueueDAO() {
    return queueDAO;
  }

  public ChatDAO getChatDAO() {
    return chatDAO;
  }

  public OperatorDAO getOperatorDAO() {
    return operatorDAO;
  }

  public ResourceBundleController getResourceBundleController() {
    return resourceBundleController;
  }

  public void shutdown() {
    dbService.shutdown();
    scheduledExecutorService.shutdown();
  }

}
