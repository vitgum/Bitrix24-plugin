package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.api.BitrixApiClient;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.model.BitrixRequestType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationDAO;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ApplicationQuery;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ChatQuery;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.QueueQuery;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * author: Artem Voronov
 */
public class BitrixApiService implements BitrixApiProvider {
  private static final Logger logger = Logger.getLogger("BITRIX_API_CLIENT");
  private static final Logger loggerRateLimiting = Logger.getLogger("BITRIX_API_CLIENT_RATE_LIMITING");
  private final RequestParamsFactory requestParamsFactory;
  private final Map<String, java.util.Queue<Runnable>> queues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, String[]>> commands = new HashMap<>(4); // key -> lang
  private static final String ENGLISH = "en";
  private static final String RUSSIAN = "ru";

  private final BitrixApiClient bitrixApiClient;
  private final ScheduledExecutorService scheduledExecutorService;

  public BitrixApiService(ScheduledExecutorService scheduledExecutorService, String appId, String appSecret, String appCode,
                          String callbackUrl, String botName, String botAvatarPath, int connectTimeout, int requestTimeout,
                          long notificationTimeoutInSeconds, ApplicationDAO applicationDAO) {
    this.scheduledExecutorService = scheduledExecutorService;

    this.bitrixApiClient = new BitrixApiClient(connectTimeout, requestTimeout, appId, appSecret, new AppTokenUpdater(applicationDAO));
    this.requestParamsFactory = new RequestParamsFactory(botName, readBotAvatar(botAvatarPath), appCode, callbackUrl);

    NotificationDaemon notificationDaemon = new NotificationDaemon();
    RenewRefreshTokensDaemon renewRefreshTokensDaemon = new RenewRefreshTokensDaemon();

    scheduledExecutorService.scheduleWithFixedDelay(notificationDaemon, 60L, notificationTimeoutInSeconds, TimeUnit.SECONDS);
    scheduledExecutorService.scheduleWithFixedDelay(renewRefreshTokensDaemon, 1L, 60L, TimeUnit.MINUTES);

    initCommands();
  }

  private static String readBotAvatar(String path) {
    File originalFile = new File(path);
    try {
      FileInputStream fileInputStreamReader = new FileInputStream(originalFile);
      byte[] bytes = new byte[(int)originalFile.length()];
      fileInputStreamReader.read(bytes);
      return Base64.getEncoder().encodeToString(bytes);
    } catch (IOException e) {
      logger.error("Can't read bot avatar", e);
    }
    return "";
  }

  private void initCommands() {
    final Map<String, String[]> helpCommand = new HashMap<>(2);
    final Map<String, String[]> infoCommand = new HashMap<>(2);
    final Map<String, String[]> startCmmand = new HashMap<>(2);
    final Map<String, String[]> stopCommand = new HashMap<>(2);

    helpCommand.put(ENGLISH, new String[]{"description of the bot", ""});
    helpCommand.put(RUSSIAN, new String[]{"описание бота", ""});

    infoCommand.put(ENGLISH, new String[]{"get current count of users", ""});
    infoCommand.put(RUSSIAN, new String[]{"количество активных пользователей", ""});

    startCmmand.put(ENGLISH, new String[]{"start messaging with next user", ""});
    startCmmand.put(RUSSIAN, new String[]{"начать сеанс общения со следующим пользователем", ""});

    stopCommand.put(ENGLISH, new String[]{"stop messaging with current user", ""});
    stopCommand.put(RUSSIAN, new String[]{"завершить сеанс общения с текущим пользователем", ""});

    commands.put("help", helpCommand);
    commands.put("info", infoCommand);
    commands.put("start", startCmmand);
    commands.put("stop", stopCommand);
  }

  public synchronized int createBot(String domain, String accessToken, String refreshToken) {
    ObjectNode response = bitrixApiClient.makeRequest(domain, BitrixRequestType.CREATE_BOT, accessToken, refreshToken, requestParamsFactory.getCreateBotJsonParams());
    return response.get("result").asInt();
  }

  public void deleteBot(Application application) {
    executeWithRateLimiting(application.getDomain(), () -> bitrixApiClient.makeRequest(
      application.getDomain(),
      BitrixRequestType.DELETE_BOT,
      application.getAccessToken(),
      application.getRefreshToken(),
      requestParamsFactory.getDeleteBotJsonParams(application.getBotId()))
    );
  }

  public void addBotCommands(Application application) {
    Integer botId = application.getBotId();
    String domain = application.getDomain();
    for (Map.Entry<String, Map<String, String[]>> commandAndLocalizedDescriptions : commands.entrySet()) {
      String commandName = commandAndLocalizedDescriptions.getKey();
      Map<String, String[]> localizationMap = commandAndLocalizedDescriptions.getValue();

      String[] ru = localizationMap.get(RUSSIAN);
      String[] en = localizationMap.get(ENGLISH);

      String englishDescription = en[0];
      String englishParamsDescription = en[1];
      String russianDescription = ru[0];
      String russianParamsDescription = ru[1];

      executeWithRateLimiting(domain, () -> bitrixApiClient.makeRequest(
        application.getDomain(),
        BitrixRequestType.ADD_BOT_COMMAND,
        application.getAccessToken(),
        application.getRefreshToken(),
        requestParamsFactory.getAddCommandJsonParams(botId, commandName, englishDescription, englishParamsDescription, russianDescription, russianParamsDescription))
      );
    }
  }

  public void sendMessage(Application application, String dialogId, String message) {
    executeWithRateLimiting(application.getDomain(), () -> bitrixApiClient.makeRequest(
      application.getDomain(),
      BitrixRequestType.SEND_MESSAGE_TO_CHAT,
      application.getAccessToken(),
      application.getRefreshToken(),
      requestParamsFactory.getSendMessageJsonParams(application.getBotId(), dialogId, message))
    );
  }

  private void executeWithRateLimiting(String domain, Runnable task) {
    java.util.Queue<Runnable> tasks = queues.get(domain);
    if (tasks == null) {
      final java.util.Queue<Runnable> newTasks = new ConcurrentLinkedQueue<>();
      queues.put(domain, newTasks);
      scheduledExecutorService.scheduleAtFixedRate(() -> {
        final Runnable next = newTasks.poll();
        if (next == null)
          return;

        if (loggerRateLimiting.isDebugEnabled())
          loggerRateLimiting.debug("polled task from queue for: " + domain);

        scheduledExecutorService.submit(next);
      }, 0, 500L, TimeUnit.MILLISECONDS);

      tasks = newTasks;
    }
    tasks.add(task);
  }

  class NotificationDaemon implements Runnable {
    private final Logger loggerDaemon = Logger.getLogger("BITRIX_NOTIFICATION_DAEMON");

    @Override
    public void run() {
      try {
        long start = System.currentTimeMillis();

        sendNotifications();

        if (loggerDaemon.isInfoEnabled()) {
          loggerDaemon.info("Finished for " + (System.currentTimeMillis() - start) / 1000 + " seconds");
        }

      } catch (Exception ex) {
        loggerDaemon.error("Unexpected error occurred.", ex);
      }
    }

    @SuppressWarnings("unchecked")
    private void sendNotifications() {
      DBService db = PluginContext.getInstance().getDBService();
      MessageDeliveryProvider messageDeliveryProvider = PluginContext.getInstance().getMessageDeliveryProvider();
      ResourceBundleController resourceBundleController = PluginContext.getInstance().getResourceBundleController();
      db.vtx(s -> {
        List<Application> applications = ApplicationQuery.all(s).list();
        for (Application application : applications) {

          List<Queue> byApplication = QueueQuery.byApplication(application, s).list();

          List<Queue> awaitingQueue = byApplication.stream().filter(q->q.getType() == QueueType.AWAITING).collect(Collectors.toList());
          List<Queue> processingQueue = byApplication.stream().filter(q->q.getType() == QueueType.PROCESSING).collect(Collectors.toList());

          if (awaitingQueue.isEmpty())
            continue;

          String appLang = application.getLanguage();
          String message = resourceBundleController.getMessage(appLang,"awaiting.users", awaitingQueue.size() + "") + "\n" +
            resourceBundleController.getMessage(appLang,"processing.users", processingQueue.size() + "");

          List<Chat> chats = ChatQuery.byType(application, Chat.Type.GROUP, s).list();
          for (Chat chat : chats) {
            messageDeliveryProvider.sendMessageToChat(application, chat.getDialogId(), message);
          }
        }
      });
    }
  }

  class RenewRefreshTokensDaemon implements Runnable {
    private final Logger loggerDaemon = Logger.getLogger("BITRIX_RENEW_REFRESH_TOKENS_DAEMON");

    @Override
    public void run() {
      try {
        long start = System.currentTimeMillis();

        renewRefreshTokens();

        if (loggerDaemon.isInfoEnabled()) {
          loggerDaemon.info("Finished for " + (System.currentTimeMillis() - start) / 1000 + " seconds");
        }

      } catch (Exception ex) {
        loggerDaemon.error("Unexpected error occurred.", ex);
      }
    }

    @SuppressWarnings("unchecked")
    private void renewRefreshTokens() {
      DBService db = PluginContext.getInstance().getDBService();
      db.vtx(s -> {
        List<Application> applications = ApplicationQuery.all(s).list();
        for (Application application : applications) {
          bitrixApiClient.refreshToken(application.getDomain(), application.getRefreshToken());
        }
      });
    }
  }
}
