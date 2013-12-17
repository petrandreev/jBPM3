package org.jbpm.tx;

import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

public class TxServiceFactory implements ServiceFactory {

  private static final long serialVersionUID = 1L;

  public Service openService() {
    return new TxService();
  }

  public void close() {
  }
}
