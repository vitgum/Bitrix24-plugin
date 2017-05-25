package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Сообщение пользователя
 */
@Entity
@Table(name = "income_messages")
public class IncomeMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name="type", unique = false, nullable = false)
  @NotNull(message = "Type is empty")
  private MessageType type;

  @Column(name = "text", nullable = true, unique = false, length = 1000)
  private String text;

  @Column(name = "image_url", nullable = true, unique = false, columnDefinition = "TEXT", length = 65535)
  @NotNull(message = "Image url is null")
  @Size(min = 1, max = 65535, message = "Image url length should be from 1 to 65535 characters")
  private String imageUrl;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "queue_id", unique = false, nullable = false)
  @NotNull(message = "Queue ID can't be null")
  private Queue queue;

  public Integer getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Queue getQueue() {
    return queue;
  }

  public void setQueue(Queue queue) {
    this.queue = queue;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
}
