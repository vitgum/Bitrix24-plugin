package com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;

import javax.persistence.*;

/**
 * Оператор, пользователь Битрикс24
 */
@Entity
@Table(name = "operators", uniqueConstraints=@UniqueConstraint(columnNames={"app_id", "operator_id"}))
public class Operator {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "operator_id", nullable = false, unique = false)
  private Integer operatorId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "app_id", unique = false, nullable = false)
  private Application application;

  public Integer getId() {
    return id;
  }

  public Integer getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(Integer operatorId) {
    this.operatorId = operatorId;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  @Transient
  public String getDialogId() {
    return operatorId.toString();
  }
}
