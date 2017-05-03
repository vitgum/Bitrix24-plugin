package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.model.BitrixRequestType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao.ApplicationController;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ApplicationQuery;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.ChatQuery;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.QueueQuery;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.MessageDeliveryProvider;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging.ResourceBundleController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
public class BitrixApiClient implements BitrixApiProvider {
  private static final Logger logger = Logger.getLogger("BITRIX_API_CLIENT");
  private static final Logger loggerRateLimiting = Logger.getLogger("BITRIX_API_CLIENT_RATE_LIMITING");
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final int RATE_PER_SECOND = 2;
  private final Client client;
  private final RequestParamsFactory requestParamsFactory;
  private final NotificationDaemon notificationDaemon;
  private final RenewRefreshTokensDaemon renewRefreshTokensDaemon;
  private final ScheduledExecutorService scheduledExecutorService;
  private final Map<String, java.util.Queue<Runnable>> queues = new ConcurrentHashMap<>();
  private final Map<String, Map<String, String[]>> commands = new HashMap<>(4); // key -> lang
  private static final String ENGLISH = "en";
  private static final String RUSSIAN = "ru";

  public BitrixApiClient(ScheduledExecutorService scheduledExecutorService, String appId, String appSecret, String appCode,
                         String callbackUrl, String botName, String botAvatarPath, int connectTimeout, int requestTimeout, long notificationTimeoutInSeconds) {
    ClientConfig config = new ClientConfig();
    config.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
    config.property(ClientProperties.READ_TIMEOUT, requestTimeout);
    config.property(ClientProperties.USE_ENCODING, "UTF-8");
    this.client = ClientBuilder.newBuilder().withConfig(config).build();

    String botPhotoInBase64 = readBotAvatar(botAvatarPath);
    this.requestParamsFactory = new RequestParamsFactory(botName, botPhotoInBase64, appId, appSecret, appCode, callbackUrl);

    this.notificationDaemon = new NotificationDaemon();
    this.renewRefreshTokensDaemon = new RenewRefreshTokensDaemon();
    this.scheduledExecutorService = scheduledExecutorService;
    scheduledExecutorService.scheduleWithFixedDelay(notificationDaemon, 60L, notificationTimeoutInSeconds, TimeUnit.SECONDS);
    scheduledExecutorService.scheduleWithFixedDelay(renewRefreshTokensDaemon, 1L, 60L, TimeUnit.MINUTES);

    initCommands();
  }

  private String readBotAvatar(String path) {
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
    ObjectNode response = makeRequest(domain, BitrixRequestType.CREATE_BOT, accessToken, refreshToken, requestParamsFactory.getCreateBotJsonParams());
    return response.get("result").asInt();
  }

  public void deleteBot(Application application) {
    executeWithRateLimiting(application.getDomain(), () -> makeRequest(
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

      executeWithRateLimiting(domain, () -> makeRequest(
        application.getDomain(),
        BitrixRequestType.ADD_BOT_COMMAND,
        application.getAccessToken(),
        application.getRefreshToken(),
        requestParamsFactory.getAddCommandJsonParams(botId, commandName, englishDescription, englishParamsDescription, russianDescription, russianParamsDescription))
      );
    }
  }

  public void sendMessage(Application application, String dialogId, String message) {
    executeWithRateLimiting(application.getDomain(), () -> makeRequest(
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
      }, 0, 1000/RATE_PER_SECOND, TimeUnit.MILLISECONDS);

      tasks = newTasks;
    }
    tasks.add(task);
  }

  private ObjectNode makeRequest(String domain, BitrixRequestType requestType, String accessToken, String refreshToken, String json) {
    final String endpoint = getRestEndpoint(domain) + requestType.getMethod() + "?auth=" + accessToken;
    ObjectNode response = makeRequest(endpoint, json);

    if (isExpiredToken(response)) {
      final String newAccessToken = refreshToken(domain, refreshToken);
      final String newEndpoint = getRestEndpoint(domain) + requestType.getMethod() + "?auth=" + newAccessToken;
      response = makeRequest(newEndpoint, json);
    }

    return response;
  }

  private ObjectNode makeRequest(String endpoint, String json) {
    if (logger.isDebugEnabled())
      logger.debug("Request to: " + endpoint + ". Params: " + json);

    ObjectNode result = null;
    try {
      Invocation invocation = client.target(endpoint)
        .request()
        .buildPost(Entity.entity(json, MediaType.APPLICATION_JSON));

      Response response = null;
      try {
        response = invocation.invoke();
        result = response.hasEntity() ? response.readEntity(ObjectNode.class) : null;

        if (result == null)
          throw new IllegalArgumentException("Null response from Bitrix REST API");

        if (logger.isDebugEnabled())
          logger.debug("Response: " + printJson(result));

      } catch (Exception ex) {
        logger.error("Error! Cause: " + ex.getMessage(), ex);
      } finally {
        if (response != null)
          response.close();
      }
    } catch (Exception ex) {
      logger.error("Error! Cause: " + ex.getMessage(), ex);
    }

    return result;
  }

  /**
   * @return new access token
   */
  private String refreshToken(String domain, String refreshToken) {
    final String queryParams = requestParamsFactory.getRefreshTokenQueryParams(refreshToken);
    final String endpoint = getRefreshTokenEndpoint(domain) + queryParams;
    ObjectNode response = makeRequest(endpoint, "{}");

    if (isError(response))
      throw new IllegalStateException("Unable to refresh token for: " + domain);

    final String newAccessToken = response.get("access_token").asText();
    final String newRefreshToken = response.get("refresh_token").asText();

    ApplicationController applicationController = PluginContext.getInstance().getApplicationController();
    Application application = applicationController.find(domain);
    applicationController.update(application.getId(), newAccessToken, newRefreshToken);
    return newAccessToken;
  }

  private static boolean isExpiredToken(ObjectNode response) {
    if (!isError(response))
      return false;

    final String errorCode = response.get("error").asText();

    return "expired_token".equals(errorCode) || "invalid_token".equals(errorCode);
  }

  private static boolean isError(ObjectNode response) {
    return response.has("error");
  }

  private static String getRestEndpoint(String domain) {
    return "https://" + domain + "/rest/";
  }

  private static String getRefreshTokenEndpoint(String domain) {
    return "https://" + domain + "/oauth/token/";
  }

  private static String printJson(ObjectNode json) {
    if (json == null)
      throw new IllegalStateException("null json object");

    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception ex) {
      String errorMsg = "Error parsing json";
      logger.error(errorMsg, ex);
      throw new IllegalStateException(errorMsg);
    }
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

    private void renewRefreshTokens() {
      DBService db = PluginContext.getInstance().getDBService();
      db.vtx(s -> {
        List<Application> applications = ApplicationQuery.all(s).list();
        for (Application application : applications) {
          refreshToken(application.getDomain(), application.getRefreshToken());
        }
      });
    }
  }
}
