package com.eyelinecom.whoisd.sads2.plugins.bitrix.web;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event.EventProcessor;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.model.Event;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.utils.ParamsExtractor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * author: Artem Voronov
 */
public class BitrixServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    handleRequest(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    handleRequest(request, response);
  }

  private static void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    Map<String, String[]> params = request.getParameterMap();
    Event event = ParamsExtractor.getEvent(params);
    EventProcessor.process(event, params, response);
  }
}
