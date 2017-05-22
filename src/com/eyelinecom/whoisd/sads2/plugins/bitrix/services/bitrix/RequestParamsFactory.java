package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.log4j.Logger;

/**
 * author: Artem Voronov
 */
class RequestParamsFactory {
  private final static Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private static final String NO = "N";
  private static final String SCOPE = "imbot";
  private static final String GRANT_TYPE = "refresh_token";

  private final ObjectMapper mapper;
  private final String botName;
  private final String botPhotoInBase64;
  private final String appCode;
  private final String callbackUrl;

  RequestParamsFactory(String botName, String botPhotoInBase64, String appCode, String callbackUrl) {
    this.mapper = new ObjectMapper();
    this.botName = botName;
    this.botPhotoInBase64 = botPhotoInBase64;
    this.appCode = appCode;
    this.callbackUrl = callbackUrl;
  }

  String getCreateBotJsonParams() {
    try {
      ObjectNode json = mapper.createObjectNode();
      ObjectNode botProperties = mapper.createObjectNode();
      botProperties.put("NAME", botName);
      botProperties.put("PERSONAL_PHOTO", botPhotoInBase64);
      json.put("CODE", appCode);
      json.put("EVENT_MESSAGE_ADD", callbackUrl);
      json.put("EVENT_WELCOME_MESSAGE", callbackUrl);
      json.put("EVENT_BOT_DELETE", callbackUrl);
      json.set("PROPERTIES", botProperties);

      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception ex) {
      String errorMsg = "Error during creating bot registration request";
      logger.error(errorMsg, ex);
      throw new IllegalStateException(errorMsg);
    }
  }

  String getDeleteBotJsonParams(Integer botId) {
    try {
      ObjectNode json = mapper.createObjectNode();

      json.put("BOT_ID", botId);

      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception ex) {
      String errorMsg = "Error during creating bot deleting request";
      logger.error(errorMsg, ex);
      throw new IllegalStateException(errorMsg);
    }
  }

  String getSendMessageJsonParams(Integer botId, String dialogId, String message) {
    try {
      ObjectNode json = mapper.createObjectNode();

      json.put("BOT_ID", botId);
      json.put("DIALOG_ID", dialogId);
      json.put("MESSAGE", message);

      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception ex) {
      String errorMsg = "Error during creating send message to Bitrix request";
      logger.error(errorMsg, ex);
      throw new IllegalStateException(errorMsg);
    }
  }

  String getAddCommandJsonParams(Integer botId, String commandName,
                                 String englishDescription, String englishParamsDescription,
                                 String russianDescription, String russianParamsDescription) {
    try {
      ObjectNode json = mapper.createObjectNode();

      ObjectNode english = mapper.createObjectNode();
      english.put("LANGUAGE_ID", "en");
      english.put("TITLE", englishDescription);
      english.put("PARAMS", englishParamsDescription);

      ObjectNode russian = mapper.createObjectNode();
      russian.put("LANGUAGE_ID", "ru");
      russian.put("TITLE", russianDescription);
      russian.put("PARAMS", russianParamsDescription);

      ObjectNode language = mapper.createObjectNode();
      language.set("en", english);
      language.set("ru", russian);

      json.put("BOT_ID", botId);
      json.put("COMMAND", commandName);
      json.put("COMMON", NO);
      json.put("HIDDEN", NO);
      json.put("EXTRANET_SUPPORT", NO);
      json.set("LANG", language);
      json.put("EVENT_COMMAND_ADD", callbackUrl);

      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception ex) {
      String errorMsg = "Error during creating add command request";
      logger.error(errorMsg, ex);
      throw new IllegalStateException(errorMsg);
    }
  }
}
