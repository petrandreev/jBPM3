package org.jbpm.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;

/**
 * Utility class to adapt hibernate 4 interface changes.
 * 
 * @author pandreev
 *
 */
public class SessionConnectionProvider implements Work, Serializable {

  private static final long serialVersionUID = 2520702444920678450L;
  
  private transient Connection connection;

  public SessionConnectionProvider(Session session) {
    session.doWork(this);
  }

  public void execute(Connection connection) throws SQLException {
    this.connection = connection;
  }

  public Connection connection() {
    return connection;
  }
}
