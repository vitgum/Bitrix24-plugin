package com.eyelinecom.whoisd.sads2.plugins.bitrix.utils;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat.Chat;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.MessageType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.model.Command;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.model.Event;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * author: Artem Voronov
 */
public class ParamsExtractor {

  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  public static String getDomain(Map<String, String[]> parameters) {
    return parameters.containsKey("domain") ? parameters.get("domain")[0] : parameters.get("auth[domain]")[0];
  }

  private static boolean isBackEvent(Map<String, String[]> parameters) {
    String eventText = parameters.containsKey("event.text") ? parameters.get("event.text")[0].trim() : "";
    return eventText.startsWith(TemplateUtils.BACK_BUTTON_EN) || eventText.startsWith(TemplateUtils.BACK_BUTTON_RU);
  }

  public static Event getEvent(Map<String, String[]> parameters) {
    String event = "";
    try {
      if (isBackEvent(parameters))
        return Event.BACK;

      event = parameters.get("event")[0];
      return Event.valueOf(event.toUpperCase());
    } catch (Exception ex) {
      logger.error("unknown event type: " + event, ex);
      return null;
    }
  }

  public static String getAccessToken(Map<String, String[]> parameters) {
    return parameters.get("auth[access_token]")[0];
  }

  public static String getRefreshToken(Map<String, String[]> parameters) {
    return parameters.get("auth[refresh_token]")[0];
  }

  public static Command getCommand(Map<String, String[]> parameters) {
    String text = parameters.get("data[PARAMS][MESSAGE]")[0];
    String command = text.startsWith("/") ? text.split("/")[1].split(" ")[0] : "";
    try {
      return Command.valueOf(command.toUpperCase());
    } catch (Exception ex) {
      logger.error("unknown command type: " + command);
      return null;
    }
  }

  public static String getUserId(Map<String, String[]> parameters) {
    return parameters.get("user_id")[0];
  }

  public static String getServiceId(Map<String, String[]> parameters) {
    return parameters.get("service")[0];
  }

  public static String getProtocol(Map<String, String[]> parameters) {
    return parameters.get("protocol")[0];
  }

  public static String getLanguage(Map<String, String[]> parameters) {
    try {
      if (parameters.containsKey("data[LANGUAGE_ID]"))
        return parameters.get("data[LANGUAGE_ID]")[0];

      return parameters.containsKey("locale") ? parameters.get("locale")[0] : parameters.get("data[PARAMS][LANGUAGE]")[0];
    } catch (Exception ex) {
      logger.error("Unable to extract language", ex);
      return "en";
    }
  }

  public static String getBackPageUrl(Map<String, String[]> parameters) {
    return parameters.get("back_page")[0];
  }

  public static String getRedirectBackPageUrl(Map<String, String[]> parameters) {
    return parameters.get("redirect_back_page")[0];
  }

  public static String getBackPageUrlOriginal(Map<String, String[]> parameters) {
    return parameters.get("back_page_original")[0];
  }

  public static String getDialogId(Map<String, String[]> parameters) {
    return parameters.get("data[PARAMS][DIALOG_ID]")[0];
  }

  public static Chat.Type getChatType(Map<String, String[]> parameters) {
    return "P".equals(parameters.get("data[PARAMS][CHAT_TYPE]")[0]) ? Chat.Type.PRIVATE : Chat.Type.GROUP;
  }

  public static int getChatId(Map<String, String[]> parameters, Chat.Type chatType) {
    return chatType == Chat.Type.PRIVATE ? Integer.parseInt(parameters.get("data[PARAMS][DIALOG_ID]")[0]) : Integer.parseInt(parameters.get("data[PARAMS][CHAT_ID]")[0]);
  }

  public static int getOperatorId(Map<String, String[]> parameters) {
    return Integer.parseInt(parameters.get("data[USER][ID]")[0]);
  }

  public static String getOperatorFullName(Map<String, String[]> parameters) {
    return parameters.get("data[USER][NAME]")[0];
  }

  public static String getOperatorFullNameWithEncoding(Map<String, String[]> parameters) {
    try {
      return EncodingUtils.convert("ISO-8859-1","UTF-8", getOperatorFullName(parameters));
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable extract full name");
    }
  }

  public static String getEventType(Map<String, String[]> parameters) {
    try {
      return parameters.get("event.type")[0];
    } catch (Exception ex) {

      logger.error("Unable to extract event type", ex);
      return null;
    }
  }

  public static String getMediaType(Map<String, String[]> parameters) {
    try {
      return parameters.get("event.media_type")[0];
    } catch (Exception ex) {

      logger.error("Unable to extract event media type", ex);
      return null;
    }
  }

  public static MessageType extractMessageType(Map<String, String[]> parameters) {
    final String eventType = ParamsExtractor.getEventType(parameters);

    if ("text".equals(eventType))
      return MessageType.TEXT;

    if ("file".equals(eventType) && "photo".equals(ParamsExtractor.getMediaType(parameters))) {
      return MessageType.IMAGE;
    }

    return null;
  }

  public static boolean hasMessageText(Map<String, String[]> parameters) {
    return parameters.containsKey("data[PARAMS][MESSAGE]");
  }

  public static String getMessageText(Map<String, String[]> parameters) {
    return parameters.containsKey("event.text") ? parameters.get("event.text")[0] : parameters.get("data[PARAMS][MESSAGE]")[0];
  }

  public static String getImageUrl(Map<String, String[]> parameters) {
    try {
      return parameters.get("event.url")[0];
    } catch (Exception ex) {

      logger.error("Unable to extract image url", ex);
      return null;
    }
  }

  public static String getMessageTextWithEncoding(Map<String, String[]> parameters) {
    try {
      return EncodingUtils.convert("ISO-8859-1","UTF-8", getMessageText(parameters));
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable extract message");
    }
  }

  public static String extractParamFromUrl(String url, String paramName) {
    String unescaped = EncodingUtils.unescape(url);
    String searchTokens[] = unescaped.split("\\?");
    String anchorTokens[] = searchTokens[1].split("#");
    String params[] = anchorTokens[0].split("&");

    for (String param : params) {
      String item[] = param.split("=");

      if (paramName.equals(item[0]))
        return item[1];
    }

    throw new IllegalStateException("Unable to extract " + paramName + " from URL");
  }
}
