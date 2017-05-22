package com.eyelinecom.whoisd.sads2.plugins.bitrix;

import com.eyeline.utils.config.xml.XmlConfigSection;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.BitrixApiProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;

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


  private PluginContext(XmlConfigSection config, String deployUrl, String sadsPushUrl, String pluginUrl) throws Exception {
    this.services = new Services(config);
    this.deployUrl = deployUrl;
    this.sadsPushUrl = sadsPushUrl;
    this.pluginUrl = pluginUrl;
  }

  static void init(XmlConfigSection config, String deployUrl, String sadsPushUrl, String pluginUrl) throws Exception {
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

  public DBService getDBService() {
    return services.getDbService();
  }

  public BitrixApiProvider getBitrixApiProvider() {
    return services.getBitrixApiProvider();
  }

  public MessageDeliveryProvider getMessageDeliveryProvider() {
    return services.getMessageDeliveryProvider();
  }

  public ResourceBundleController getResourceBundleController() {
    return services.getResourceBundleController();
  }

}
