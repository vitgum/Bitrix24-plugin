package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.messaging;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * author: Artem Voronov
 */
public class ResourceBundleController {

  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");
  private static final String RESOURCE_BUNDLE_TEMPLATE = "messages_%s.properties";
  private static final Map<String, PropertyResourceBundle> bundles = new HashMap<>();
  private static final List<String> supportedLangs = Arrays.asList("en", "ru");

  public ResourceBundleController() {
    for (String lang : supportedLangs) {
      String path = String.format(RESOURCE_BUNDLE_TEMPLATE, lang);
      try (InputStream is = ResourceBundleController.class.getResourceAsStream(path)) {
        try (InputStreamReader ir = new InputStreamReader(is, "UTF8")) {
          PropertyResourceBundle resourceBundle = new PropertyResourceBundle(ir);
          bundles.put(lang, resourceBundle);
        }

      } catch (IOException e) {
        logger.error("Unable to load resource bundles", e);
      }
    }
  }

  public PropertyResourceBundle getResourceBundle(String lang) {
    if (lang == null || !supportedLangs.contains(lang))
      lang = "en";

    return bundles.get(lang);
  }

  public String getMessage(String lang, String key) {
    PropertyResourceBundle bundle = getResourceBundle(lang);
    return bundle.getString(key);
  }
}
