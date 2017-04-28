package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user;


import javax.persistence.*;

/**
 * Пользователь Telegram, Skype, Viber, VK и др. каналов, которые поддерживаются платформой SADS
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "user_id", nullable = false, unique = true)
  private String userId; //в SADS это 32-х символьная строка, которая фигурирует в URL, в параметре user_id

  public Integer getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

}
