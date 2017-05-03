package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.api.handlers.event

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.DBTestBase

/**
 * author: Artem Voronov
 */
class MessageFromOperatorHandlerTest extends DBTestBase {

  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testUnknownApplicationInPrivateChat() {
  }
  /**
   * Незарегистрированное приложение присылает запрос. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testUnknownApplicationInGroupChat() {
  }

  /**
   * Сообщение от оператора в групповом чате. Оператор ещё не был создан в БД. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testMessageFromNotExistingOperatorInGroupChat() {
  }

  /**
   * Сообщение от оператора в групповом чате. Оператор уже есть в БД. Проверям что:
   * 1. оператор не создавался
   * 2. никаких сообщений не отправлялось
   */
  void testMessageFromExistingOperatorInGroupChat() {
  }

  /**
   * Сообщение от оператора в приватном чате. Оператор ещё не был создан в БД. Случай когда сеанс общения с пользователем не инициирован. Проверям что:
   * 1. оператор был сохранён в БД
   * 2. оператору высылается сообщение
   */
  void testMessageFromNotExistingOperatorInPrivateChatUserNotProcessing() {
  }

  /**
   * Сообщение от оператора в приватном чате. Оператор ещё не был создан в БД. Случай когда сеанс общения с пользователем уже инициирован. Проверям что:
   * 1. оператор был сохранён в БД
   * 2. пользователю высылается сообщение
   */
  void testMessageFromNotExistingOperatorInPrivateChatUserProcessing() {
  }

  /**
   * Сообщение от оператора в приватном чате. Оператор уже есть в БД. Случай когда сеанс общения с пользователем не инициирован. Проверям что:
   * 1. оператор не создаётся в БД
   * 2. оператору высылается сообщение
   */
  void testMessageFromExistingOperatorInPrivateChatUserNotProcessing() {
  }

  /**
   * Сообщение от оператора в приватном чате. Оператор уже есть в БД. Случай когда сеанс общения с пользователем уже инициирован. Проверям что:
   * 1. оператор не создаётся в БД
   * 2. пользователю высылается сообщение
   */
  void testMessageFromExistingOperatorInPrivateChatUserProcessing() {
  }
}
