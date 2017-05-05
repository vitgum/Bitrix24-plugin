package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.IncomeMessage;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.QueueQuery;

import java.util.List;
import java.util.stream.Collectors;

/**
 * author: Artem Voronov
 */
  public class QueueController {

  private DBService db;

  public QueueController(DBService db) {
    this.db = db;
  }

  public Queue find(Application application, User user, String serviceId) {
    return db.tx(s -> (Queue) QueueQuery.byUser(application, user, serviceId, s).uniqueResult());
  }

  public void storeMessage(Queue queue, String message) {
    db.vtx(s -> {
      IncomeMessage incomeMessage = new IncomeMessage();
      incomeMessage.setQueue(queue);
      incomeMessage.setText(message);
      s.save(incomeMessage);
    });
  }

  public Queue addToAwaitingQueue(Application application, User user, String serviceId, String protocol, String backPage, String language) {
    return db.tx(s -> {
      Queue queue = new Queue();
      queue.setUser(user);
      queue.setApplication(application);
      queue.setType(QueueType.AWAITING);
      queue.setServiceId(serviceId);
      queue.setProtocol(protocol);
      queue.setBackPage(backPage);
      queue.setLanguage(language);
      s.save(queue);
      return queue;
    });
  }

  public void moveToProcessingQueue(Integer queueId, Operator operator) {
    db.vtx(s -> {
      Queue queue = (Queue) QueueQuery.byId(queueId, s).uniqueResult();
      queue.setType(QueueType.PROCESSING);
      queue.setOperator(operator);
      s.save(queue);
    });
  }

  public void removeFromQueue(Application application, User user, String serviceId) {
    db.vtx(s -> {
      Queue queue = (Queue) QueueQuery.byUser(application, user, serviceId, s).uniqueResult();

      if (queue == null)
        return;

      String hql = "delete from IncomeMessage where queue.id= :queueId";
      s.createQuery(hql).setInteger("queueId", queue.getId()).executeUpdate();
      s.delete(queue);
    });
  }

  public Queue getFirstAwaiting(Application application) {
    return db.tx(s -> (Queue) QueueQuery.byTypeInAscendingOrder(application, QueueType.AWAITING, 1, s).uniqueResult());
  }

  public boolean isOperatorBusy(Operator operator) {
    Queue queue = db.tx(s -> (Queue) QueueQuery.byOperator(operator, s).uniqueResult());
    return queue != null;
  }

  public Operator getOperator(Application application, User user, String serviceId) {
    Queue queue = db.tx(s -> (Queue) QueueQuery.byUser(application, user, serviceId, s).uniqueResult());

    if (queue == null)
      return null;

    return queue.getOperator();
  }

  public Queue getProcessingQueue(Operator operator) {
    return db.tx(s -> (Queue) QueueQuery.byTypeAndOperator(operator, QueueType.PROCESSING, s).uniqueResult());
  }

  public Queue getProcessingQueue(Application application, User user, String serviceId) {
    return db.tx(s -> (Queue) QueueQuery.byTypeAndUser(application, user, serviceId, QueueType.PROCESSING, s).uniqueResult());
  }

  public UserCounters getUserCounters(Application application) {
    return db.tx(s -> {
      List<Queue> byApplication = QueueQuery.byApplication(application, s).list();
      List<Queue> awaitingQueue = byApplication.stream().filter(q -> q.getType() == QueueType.AWAITING).collect(Collectors.toList());
      List<Queue> processingQueue = byApplication.stream().filter(q -> q.getType() == QueueType.PROCESSING).collect(Collectors.toList());

      return new UserCounters(awaitingQueue.size(), processingQueue.size());
    });
  }

  public String getMessages(Integer queueId) {
    return db.tx(s -> {
      Queue queue = (Queue) QueueQuery.byId(queueId, s).uniqueResult();
      List<IncomeMessage> list = queue.getIncomeMessages();
      return list.stream().map(IncomeMessage::getText).collect(Collectors.joining("\n"));
    });
  }

  public void deleteMessages(Integer queueId) {
    db.vtx(s -> {
      String hql = "delete from IncomeMessage where queue.id= :queueId";
      s.createQuery(hql).setInteger("queueId", queueId).executeUpdate();
    });
  }
}
