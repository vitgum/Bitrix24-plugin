package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

/**
 * установка Битрикс24
 */
@Entity
@Table(name = "applications", uniqueConstraints=@UniqueConstraint(columnNames={"domain", "deleted"}))
public class Application {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "domain", nullable = false, unique = false, length = 255)
  private String domain;

  @Column(name = "access_token", nullable = false, unique = false)
  private String accessToken;

  @Column(name = "refresh_token", nullable = false, unique = false)
  private String refreshToken;

  @Column(name = "bot_id", nullable = false, unique = false)
  private Integer botId;

  @Column(name="deleted", nullable = false, unique = false)
  private boolean deleted;

  @CreationTimestamp
  @Column(name="create_date", unique = false, nullable = false)
  private Date createDate;

  @UpdateTimestamp
  @Column(name="last_modified", unique = false, nullable = false)
  private Date lastModified;

  public Integer getId() {
    return id;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public Integer getBotId() {
    return botId;
  }

  public void setBotId(Integer botId) {
    this.botId = botId;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public Date getCreateDate() {
    return createDate;
  }

  public Date getLastModified() {
    return lastModified;
  }
}
