package com.eyelinecom.whoisd.sads2.plugins.bitrix;

import com.eyeline.utils.config.xml.XmlConfig;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.BitrixApiClient;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;

import javax.ws.rs.Produces;
import java.util.concurrent.CountDownLatch;

/**
 * author: Artem Voronov
 */
public class PluginContext {
  private final static CountDownLatch initLatch = new CountDownLatch(1);
  private static PluginContext instance;

  private final Services services;
  private final String deployUrl;
  private final String sadsPushUrl;
  private final String pluginUrl;


  private PluginContext(XmlConfig config, String deployUrl, String sadsPushUrl, String pluginUrl) throws Exception {
    this.services = new Services(config);
    this.deployUrl = deployUrl;
    this.sadsPushUrl = sadsPushUrl;
    this.pluginUrl = pluginUrl;
  }

  static void init(XmlConfig config, String deployUrl, String sadsPushUrl, String pluginUrl) throws Exception {
    if(instance == null) {
      instance = new PluginContext(config, deployUrl, sadsPushUrl, pluginUrl);
      initLatch.countDown();
    }
  }

  public static PluginContext getInstance() {
    try {
      initLatch.await();
      return instance;
    }
    catch(InterruptedException e) {
      throw new RuntimeException("PluginContext instance is not initialized", e);
    }
  }

  void shutdown() {
    services.shutdown();
  }

  public Services getServices() {
    return services;
  }

  public String getDeployUrl() {
    return deployUrl;
  }

  public String getSadsPushUrl() {
    return sadsPushUrl;
  }

  public String getPluginUrl() {
    return pluginUrl;
  }

  @Produces
  public DBService getDBService() {
    return services.getDbService();
  }

  @Produces
  public BitrixApiClient getBitrixApiClient() {
    return services.getBitrixApiClient();
  }

  @Produces
  public MessageDeliveryService getMessageDeliveryService() {
    return services.getMessageDeliveryService();
  }

  @Produces
  public ApplicationController getApplicationController() {
    return services.getApplicationController();
  }

  @Produces
  public UserController getUserController() {
    return services.getUserController();
  }

  @Produces
  public QueueController getQueueController() {
    return services.getQueueController();
  }

  @Produces
  public ChatController getChatController() {
    return services.getChatController();
  }

  @Produces
  public OperatorController getOperatorController() {
    return services.getOperatorController();
  }

  @Produces
  public ResourceBundleController getResourceBundleController() {
    return services.getResourceBundleController();
  }

}
