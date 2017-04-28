package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

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

  @Column(name = "text", nullable = false, unique = false, length = 1000)//TODO
  private String text;

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
}
