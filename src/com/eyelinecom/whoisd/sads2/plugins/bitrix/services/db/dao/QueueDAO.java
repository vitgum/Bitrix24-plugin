package com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.dao;

import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.app.Application;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.IncomeMessage;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.message.MessageType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.operator.Operator;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.Queue;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.queue.QueueType;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.model.user.User;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.DBService;
import com.eyelinecom.whoisd.sads2.plugins.bitrix.services.db.query.QueueQuery;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * author: Artem Voronov
 */
public class QueueDAO {

  private static final Logger logger = Logger.getLogger("BITRIX_PLUGIN");

  private DBService db;

  public QueueDAO(DBService db) {
    this.db = db;
  }

  public Queue find(Application application, User user, String serviceId) {
    return db.tx(s -> getQueue(application, user, serviceId, s));
  }

  public void storeMessage(Queue queue, MessageType type, String data) {
    db.vtx(s -> {
      IncomeMessage incomeMessage = new IncomeMessage();
      incomeMessage.setQueue(queue);
      incomeMessage.setType(type);

      switch (type) {
        case TEXT:
          incomeMessage.setText(data);
          break;
        case IMAGE:
          incomeMessage.setImageUrl(data);
          break;
        default:
          throw new IllegalArgumentException("Unknown message type: " + type);
      }

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
      Queue queue = getQueue(queueId, s);

      if (queue == null)
        return;

      queue.setType(QueueType.PROCESSING);
      queue.setOperator(operator);
      s.save(queue);
    });
  }

  public void removeFromQueue(Application application, User user, String serviceId) {
    db.vtx(s -> {
      Queue queue = getQueue(application, user, serviceId, s);

      if (queue == null)
        return;

      String hql = "delete from IncomeMessage where queue.id= :queueId";
      s.createQuery(hql).setInteger("queueId", queue.getId()).executeUpdate();
      s.delete(queue);
    });
  }

  public Queue getFirstAwaiting(Application application) {
    return db.tx(s -> getFirstAwaitingQueue(application, s));
  }

  public boolean isOperatorBusy(Operator operator) {
    Queue queue = db.tx(s -> getQueue(operator, s));
    return queue != null;
  }

  public Operator getOperator(Application application, User user, String serviceId) {
    Queue queue = db.tx(s -> getQueue(application, user, serviceId, s));

    if (queue == null)
      return null;

    return queue.getOperator();
  }

  public Queue getProcessingQueue(Operator operator) {
    return db.tx(s -> getQueue(operator, QueueType.PROCESSING, s));
  }

  public UserCounters getUserCounters(Application application) {
    return db.tx(s -> {
      List<Queue> byApplication = getAppQueues(application,s);
      List<Queue> awaitingQueue = byApplication.stream().filter(q -> q.getType() == QueueType.AWAITING).collect(Collectors.toList());
      List<Queue> processingQueue = byApplication.stream().filter(q -> q.getType() == QueueType.PROCESSING).collect(Collectors.toList());

      return new UserCounters(awaitingQueue.size(), processingQueue.size());
    });
  }

  @SuppressWarnings("unchecked")
  public List<IncomeMessage> getMessages(Integer queueId) {
    return db.tx(s -> {
      Queue queue = getQueue(queueId, s);

      if (queue == null)
        return Collections.emptyList();

      List<IncomeMessage> list = queue.getIncomeMessages();
      list.size();
      return list;
    });
  }

  public void deleteMessages(Integer queueId) {
    db.vtx(s -> {
      String hql = "delete from IncomeMessage where queue.id= :queueId";
      s.createQuery(hql).setInteger("queueId", queueId).executeUpdate();
    });
  }

  @SuppressWarnings("unchecked")
  public static List<Queue> getAppQueues(Application application, Session s) {
    try {
      return QueueQuery.byApplication(application, s).list();
    } catch (org.hibernate.ObjectNotFoundException ex) {
      if (logger.isDebugEnabled())
        logger.debug("org.hibernate.ObjectNotFoundException ignored for App: " + application.getDomain());
      return Collections.emptyList();
    }
  }

  private static Queue getQueue(Application application, User user, String serviceId, Session s) {
    try {
      return (Queue) QueueQuery.byUser(application, user, serviceId, s).uniqueResult();
    } catch (org.hibernate.ObjectNotFoundException ex) {
      if (logger.isDebugEnabled())
        logger.debug("org.hibernate.ObjectNotFoundException ignored for App: " + application.getDomain() + ". User: " + user.getUserId() + ". Service ID: " + serviceId);
      return null;
    }
  }

  private static Queue getQueue(Integer queueId, Session s) {
    try {
      return (Queue) QueueQuery.byId(queueId, s).uniqueResult();
    } catch (org.hibernate.ObjectNotFoundException ex) {
      if (logger.isDebugEnabled())
        logger.debug("org.hibernate.ObjectNotFoundException ignored for Queue ID: " + queueId);
      return null;
    }
  }

  private static Queue getQueue(Operator operator, QueueType queueType, Session s) {
    try {
      return (Queue) QueueQuery.byTypeAndOperator(operator, queueType, s).uniqueResult();
    } catch (org.hibernate.ObjectNotFoundException ex) {
      if (logger.isDebugEnabled())
        logger.debug("org.hibernate.ObjectNotFoundException ignored for operator ID: " + operator.getId() + ". Queue type: " + queueType);
      return null;
    }
  }

  private static Queue getQueue(Operator operator, Session s) {
    try {
      return (Queue) QueueQuery.byOperator(operator, s).uniqueResult();
    } catch (org.hibernate.ObjectNotFoundException ex) {
      if (logger.isDebugEnabled())
        logger.debug("org.hibernate.ObjectNotFoundException ignored for operator ID: " + operator.getId());
      return null;
    }
  }
  private static Queue getFirstAwaitingQueue(Application application, Session s) {
    try {
      return (Queue) QueueQuery.byTypeInAscendingOrder(application, QueueType.AWAITING, 1, s).uniqueResult();
    } catch (org.hibernate.ObjectNotFoundException ex) {
      if (logger.isDebugEnabled())
        logger.debug("org.hibernate.ObjectNotFoundException ignored for getFirstAwaitingQueue(). App: " + application.getDomain());
      return null;
    }
  }
}
