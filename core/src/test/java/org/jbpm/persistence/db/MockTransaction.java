package org.jbpm.persistence.db;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.engine.transaction.spi.LocalStatus;

public class MockTransaction implements Transaction {

  private LocalStatus localStatus = LocalStatus.NOT_ACTIVE;

  public void begin() throws HibernateException {
    localStatus = LocalStatus.ACTIVE;
  }

  public void commit() throws HibernateException {
    localStatus = LocalStatus.COMMITTED;
  }

  public void rollback() throws HibernateException {
    localStatus = LocalStatus.ROLLED_BACK;
  }

  public boolean wasCommitted() throws HibernateException {
    return localStatus == LocalStatus.COMMITTED;
  }

  public boolean wasRolledBack() throws HibernateException {
    return localStatus == LocalStatus.ROLLED_BACK;
  }

  public boolean isActive() throws HibernateException {
    return localStatus == LocalStatus.ACTIVE;
  }

  public void registerSynchronization(Synchronization synchronization)
    throws HibernateException {
  }

  public void setTimeout(int seconds) {
  }

  public boolean isInitiator() {
    return false;
  }

  public LocalStatus getLocalStatus() {
    return localStatus;
  }

  public boolean isParticipating() {
    return false;
  }

  public int getTimeout() {
    return -1;
  }
}
