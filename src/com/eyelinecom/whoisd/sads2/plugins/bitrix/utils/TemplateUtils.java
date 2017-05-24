package com.eyelinecom.whoisd.sads2.plugins.bitrix.utils;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.PluginContext;

/**
 * author: Artem Voronov
 */
public class TemplateUtils {

  private static final String EMPTY_PAGE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
    "<page version=\"2.0\" attributes=\"keep.session: true\">" +
    "</page>";

  private static final String EMPTY_PAGE_WITH_BACK_BUTTON = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
    "<page version=\"2.0\" attributes=\"keep.session: true\">" +
      "<navigation>" +
        "<link accesskey=\"1\" pageId=\"%s\">%s</link>" +
      "</navigation>" +
    "</page>";

  private static final String REDIRECT_BACK_PAGE_URL = "%s?domain=%s&user_id=%s&service=%s&protocol=%s&locale=%s&back_page_original=%s";

  private static final String INPUT_URL = "%s?domain=%s&locale=%s&redirect_back_page=%s";

  private static final String BASIC_PAGE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
    "<page version=\"2.0\">" +
      "<div>" +
        "<input navigationId=\"submit\" name=\"event.text\" title=\"%s\" />" +
      "</div>" +
      "<navigation id=\"submit\">" +
        "<link accesskey=\"1\" pageId=\"%s\">Ok</link>" +
      "</navigation>" +
      "<navigation>" +
        "<link accesskey=\"2\" pageId=\"%s\">%s</link>" +
      "</navigation>" +
    "</page>";
  private static final String ERROR_PAGE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
    "<page version=\"2.0\">" +
      "<div>" +
        "%s" +
      "</div>" +
    "<navigation>" +
      "<link accesskey=\"1\" pageId=\"http://plugins.miniapps.run/invalidate-session\">%s</link>" +
    "</navigation>" +
    "</page>";

  private static final String PUSH_URL = "%s?service=%s&user_id=%s&protocol=%s&scenario=xmlpush&document=%s";
  private static final String INVALIDATE_SESSION_URL = "http://plugins.miniapps.run/invalidate-session?success_url=%s";

  private static final char HIDDEN_CHAR = '\u2063';
  public static final String BACK_BUTTON_EN = HIDDEN_CHAR + "Back";
  public static final String BACK_BUTTON_RU = HIDDEN_CHAR + "Назад";

  public static String createRedirectBackPageUrl(String startUrl, String domain, String userId, String serviceId, String protocol, String lang, String backPageUrlOriginal) {
    return String.format(REDIRECT_BACK_PAGE_URL, startUrl, domain, userId, serviceId, protocol, lang, backPageUrlOriginal);
  }

  public static String createInputUrl(String startUrl, String domain, String lang, String redirectBackPage) {
    return String.format(INPUT_URL, startUrl, domain, lang, redirectBackPage);
  }

  public static String createBasicPage(String inputTitle, String inputUrl, String backBtnUrl, String lang) {
    final String backButtonText = "en".equals(lang) ? BACK_BUTTON_EN : BACK_BUTTON_RU;
    return String.format(BASIC_PAGE, inputTitle, inputUrl, backBtnUrl, backButtonText);
  }

  public static String createEmptyPage(String backBtnUrl, String lang, String protocol) {
    if ("telegram".equals(protocol) || "viber".equals(protocol))
      return EMPTY_PAGE;

    final String backButtonText = "en".equals(lang) ? BACK_BUTTON_EN : BACK_BUTTON_RU;
    return String.format(EMPTY_PAGE_WITH_BACK_BUTTON, backBtnUrl, backButtonText);
  }

  public static String createPushUrl(String serviceId, String userId, String protocol, String document) {
    String basePushUrl = PluginContext.getInstance().getSadsPushUrl();
    return String.format(PUSH_URL, basePushUrl, serviceId, userId, protocol, document);
  }

  public static String createInvalidateSessionUrl(String successUrl) {
    return String.format(INVALIDATE_SESSION_URL, successUrl);
  }

  public static String createErrorPage(String message, String toStartPageButton) {
    return String.format(ERROR_PAGE, message, toStartPageButton);
  }
}
