package com.eyelinecom.whoisd.sads2.plugins.bitrix.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: Artem Voronov
 */
public class EncodingUtils {

  public static String encode(String value) {
    try {
      return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Value can not be encoded: " + value);
    }
  }

  public static String decode(String value) {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Value can not be encoded: " + value);
    }
  }

  public static String escape(String value) {
    Matcher matcher = Pattern.compile("(&.{4})", Pattern.CASE_INSENSITIVE).matcher(value);

    StringBuffer sb = new StringBuffer();
    while(matcher.find()) {
      String group = matcher.group();

      if ("&amp;".equals(group))
        continue;

      matcher.appendReplacement(sb, group.replaceAll("&", "&amp;"));
    }

    matcher.appendTail(sb);

    return sb.toString();
  }

  public static String unescape(String value) {
    return value.replaceAll("&amp;", "&");
  }

  public static String convert(String inputEncoding, String outputEncoding, String value) {
    try {
      return new String(value.getBytes(inputEncoding), outputEncoding);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unsupported encoding");
    }
  }


}
