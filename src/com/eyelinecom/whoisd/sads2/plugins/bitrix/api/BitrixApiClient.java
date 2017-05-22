package com.eyelinecom.whoisd.sads2.plugins.bitrix.api;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.bitrix.model.BitrixRequestType;
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

/**
 * author: Artem Voronov
 */
public class BitrixApiClient {
  private static final Logger logger = Logger.getLogger("BITRIX_API_CLIENT");
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String GRANT_TYPE = "refresh_token";
  private static final String SCOPE = "imbot";
  private final Client client;
  private final String appId;
  private final String appSecret;
  private final TokenUpdater tokenUpdater;

  public BitrixApiClient(int connectTimeout, int requestTimeout, String appId, String appSecret, TokenUpdater tokenUpdater) {
    ClientConfig config = new ClientConfig();
    config.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
    config.property(ClientProperties.READ_TIMEOUT, requestTimeout);
    config.property(ClientProperties.USE_ENCODING, "UTF-8");
    this.client = ClientBuilder.newBuilder().withConfig(config).build();
    this.appId = appId;
    this.appSecret = appSecret;
    this.tokenUpdater = tokenUpdater;
  }


  public ObjectNode makeRequest(String domain, BitrixRequestType requestType, String accessToken, String refreshToken, String json) {
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
  public String refreshToken(String domain, String refreshToken) {
    final String queryParams = getRefreshTokenQueryParams(refreshToken);
    final String endpoint = getRefreshTokenEndpoint(domain) + queryParams;
    ObjectNode response = makeRequest(endpoint, "{}");

    if (isError(response))
      throw new IllegalStateException("Unable to refresh token for: " + domain);

    final String newAccessToken = response.get("access_token").asText();
    final String newRefreshToken = response.get("refresh_token").asText();

    tokenUpdater.update(domain, newAccessToken, newRefreshToken);

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

  private String getRefreshTokenQueryParams(String refreshToken) {
    return "?grant_type=" + GRANT_TYPE +
      "&client_id=" + appId +
      "&client_secret=" + appSecret +
      "&refresh_token=" + refreshToken +
      "&scope=" + SCOPE;
  }
}
