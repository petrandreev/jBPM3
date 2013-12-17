package org.jbpm.ejb;

import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;
import javax.ejb.FinderException;

public interface LocalTimerEntityHome extends EJBLocalHome {

  public LocalTimerEntity create() throws CreateException;

  public LocalTimerEntity findByPrimaryKey(Long timerId) throws FinderException;

  public Collection<LocalTimerEntity> findByNameAndTokenId(String name, Long tokenId)
      throws FinderException;

  public Collection<LocalTimerEntity> findByProcessInstanceId(Long processInstanceId)
      throws FinderException;
}
