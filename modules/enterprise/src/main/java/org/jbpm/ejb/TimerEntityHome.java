package org.jbpm.ejb;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.FinderException;

public interface TimerEntityHome extends EJBHome {
  public TimerEntity create() throws CreateException, RemoteException;

  public TimerEntity findByPrimaryKey(Long timerId) throws FinderException, RemoteException;

  public Collection<TimerEntity> findByNameAndTokenId(String name, Long tokenId)
      throws FinderException, RemoteException;

  public Collection<TimerEntity> findByProcessInstanceId(Long processInstanceId)
      throws FinderException, RemoteException;
}
