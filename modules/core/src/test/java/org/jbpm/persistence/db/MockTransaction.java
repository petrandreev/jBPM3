package org.jbpm.persistence.db;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;

public class MockTransaction implements Transaction {

  boolean wasCommitted = false;
  boolean wasRolledBack = false;

  public void begin() throws HibernateException {
  }

  public void commit() throws HibernateException {
    wasCommitted = true;
  }

  public void rollback() throws HibernateException {
    wasRolledBack = true;
  }

  public boolean wasCommitted() throws HibernateException {
    return wasCommitted;
  }

  public boolean wasRolledBack() throws HibernateException {
    return wasRolledBack;
  }

  public boolean isActive() throws HibernateException {
    return (!wasCommitted) && (!wasRolledBack);
  }

  public void registerSynchronization(Synchronization synchronization) throws HibernateException {
  }

  public void setTimeout(int seconds) {
  }

}
