package com.eyelinecom.whoisd.sads2.plugins.bitrix;

import com.eyeline.utils.config.ConfigException;
import com.eyeline.utils.config.xml.XmlConfig;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.Services;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.command.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event.*;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.model.Command;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.model.Event;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * author: Artem Voronov
 */
public class InitListener implements ServletContextListener {

  private static final String PROPERTY_CONFIG_DIR     = "bitrix.plugin.config.dir";
  private static final String DEFAULT_CONFIG_DIR      = "conf";
  private static final String PROPERTIES_FILE_NAME    = "config.xml";

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    final File configDir = getConfigDir();
    initLog4j(configDir);
    XmlConfig config = loadXmlConfig(configDir);
    String deployUrl = getDeployUrl(config);
    String pushUrl = getPushUrl(config);
    String pluginUrl = getPluginUrl(config);
    initPluginContext(config, deployUrl, pushUrl, pluginUrl);
    initHandlers();
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    PluginContext.getInstance().shutdown();
  }

  private File getConfigDir() {
    String configDir = System.getProperty(PROPERTY_CONFIG_DIR);
    if (configDir == null) {
      configDir = DEFAULT_CONFIG_DIR;
      System.err.println("System property '" + PROPERTY_CONFIG_DIR + "' is not set. Using default value: " + configDir);
    }
    File cfgDir = new File(configDir);

    if (!cfgDir.exists())
      throw new RuntimeException("Config directory '" + cfgDir.getAbsolutePath() + "' does not exist");

    System.out.println("Using properties directory '" + cfgDir.getAbsolutePath() + "'");
    return cfgDir;
  }

  private XmlConfig loadXmlConfig(File configDir) {
    final File cfgFile = new File(configDir, PROPERTIES_FILE_NAME);
    XmlConfig cfg = new XmlConfig();
    try {
      cfg.load(cfgFile);
    } catch (ConfigException e) {
      throw new RuntimeException("Unable to load config.xml", e);
    }
    return cfg;
  }

  private void initLog4j(File configDir) {
    final File log4jProps = new File(configDir, "log4j.properties");
    System.out.println("Log4j conf file: " + log4jProps.getAbsolutePath() + ", exists: " + log4jProps.exists());
    PropertyConfigurator.configureAndWatch(log4jProps.getAbsolutePath(), TimeUnit.MINUTES.toMillis(1));
  }

  private void initPluginContext(XmlConfig config, String deployUrl, String pushUrl, String pluginUrl) {
    try {
      PluginContext.init(config, deployUrl, pushUrl, pluginUrl);
    }
    catch(Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Can't init PluginContext", e);
    }
  }

  private void initHandlers() {
    Services services = PluginContext.getInstance().getServices();
    EventProcessor.addHandler(Event.ONAPPINSTALL, new AppInstallHandler(services.getApplicationController(), services.getBitrixApiProvider()));
    EventProcessor.addHandler(Event.ONIMBOTDELETE, new AppUninstallHandler(services.getApplicationController(), services.getBitrixApiProvider()));
    EventProcessor.addHandler(Event.ONAPPUPDATE, new AppUpdateHandler(services.getApplicationController()));
    EventProcessor.addHandler(Event.ONIMBOTMESSAGEADD, new MessageFromOperatorHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController(), services.getQueueController()));
    EventProcessor.addHandler(Event.ONIMBOTJOINCHAT, new BotJoinToChatHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController()));
    EventProcessor.addHandler(Event.ONIMCOMMANDADD, new CommandFromOperatorHandler());

    EventProcessor.addHandler(Event.MESSAGE, new MessageFromUserHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController(), services.getQueueController(), services.getUserController()));
    EventProcessor.addHandler(Event.LINK, new UserStartMessagingHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController()));
    EventProcessor.addHandler(Event.BACK, new UserStopMessagingHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController(), services.getQueueController(), services.getUserController()));

    CommandProcessor.addHandler(Command.HELP, new HelpHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController()));
    CommandProcessor.addHandler(Command.INFO, new InfoHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController(), services.getQueueController()));
    CommandProcessor.addHandler(Command.START, new StartHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController(), services.getQueueController()));
    CommandProcessor.addHandler(Command.STOP, new StopHandler(services.getChatController(), services.getOperatorController(), services.getMessageDeliveryProvider(), services.getResourceBundleController(), services.getApplicationController(), services.getQueueController()));
  }

  private String getDeployUrl(XmlConfig config) {
    try {
      return config.getString("deploy.url");
    }
    catch(ConfigException e) {
      throw new RuntimeException("Parameter deploy.url is not found.", e);
    }
  }

  private String getPushUrl(XmlConfig config) {
    try {
      return config.getString("push.url");
    }
    catch(ConfigException e) {
      throw new RuntimeException("Parameter deploy.url is not found.", e);
    }
  }

  private String getPluginUrl(XmlConfig config) {
    try {
      return config.getString("plugin.url");
    }
    catch(ConfigException e) {
      throw new RuntimeException("Parameter deploy.url is not found.", e);
    }
  }
}
