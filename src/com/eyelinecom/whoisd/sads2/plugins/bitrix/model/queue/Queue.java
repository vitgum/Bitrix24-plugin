package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.IncomeMessage;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Очередь пользователей. Это либо те кто уже общается с операторами, либо те кто ждёт пока ему ответят.
 */
@Entity(name = "queue")
@Table(name = "queues", uniqueConstraints=@UniqueConstraint(columnNames={"app_id", "user_id", "service_id"}))
public class Queue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name="type", unique = false, nullable = false)
  @NotNull(message = "Type is empty")
  private QueueType type;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "app_id", unique = false, nullable = false)
  private Application application;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="user_id", unique = false, nullable = false)
  @NotNull(message = "User can't be null")
  private User user;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name="operator_id", unique = false, nullable = true)
  private Operator operator;

  @Column(name = "protocol", nullable = false, unique = false, length = 255)
  private String protocol;

  @Column(name = "service_id", nullable = false, unique = false, length = 255)
  private String serviceId;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "queue", cascade = {CascadeType.PERSIST})
  private List<IncomeMessage> incomeMessages = new ArrayList<>();

  @Column(name = "back_page", nullable = true, unique = false, columnDefinition = "TEXT", length = 65535)
  @NotNull(message = "Back page is null")
  @Size(min = 1, max = 65535, message = "Back page length should be from 1 to 65535 characters")
  private String backPage;

  @Column(name = "lang", nullable = false, unique = false, length = 7)
  private String language;

  @CreationTimestamp
  @Column(name="create_date", unique = false, nullable = false)
  private Date createDate;

  @UpdateTimestamp
  @Column(name="last_modified", unique = false, nullable = false)
  private Date lastModified;

  public Integer getId() {
    return id;
  }

  public QueueType getType() {
    return type;
  }

  public void setType(QueueType type) {
    this.type = type;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Operator getOperator() {
    return operator;
  }

  public void setOperator(Operator operator) {
    this.operator = operator;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public List<IncomeMessage> getIncomeMessages() {
    return incomeMessages;
  }

  public void setIncomeMessages(List<IncomeMessage> incomeMessages) {
    this.incomeMessages = incomeMessages;
  }

  public String getBackPage() {
    return backPage;
  }

  public void setBackPage(String backPage) {
    this.backPage = backPage;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public Date getLastModified() {
    return lastModified;
  }

}
