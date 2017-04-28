package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.chat;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Чат в Битрикс24
 */
@Entity
@Table(name = "chats", uniqueConstraints=@UniqueConstraint(columnNames={"app_id", "chat_id", "type"}))
public class Chat {

  public enum Type {
    PRIVATE, GROUP
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "app_id", unique = false, nullable = false)
  private Application application;

  @Column(name = "chat_id", nullable = false, unique = false)
  private Integer chatId;

  @Enumerated(EnumType.STRING)
  @Column(name="type", unique = false, nullable = false)
  @NotNull(message = "Chat type is empty")
  private Type type;

  public Integer getId() {
    return id;
  }

  public Integer getChatId() {
    return chatId;
  }

  public void setChatId(Integer chatId) {
    this.chatId = chatId;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  @Transient
  public String getDialogId() {
    return type == Type.PRIVATE ? ""+chatId : "chat"+chatId;
  }
}
