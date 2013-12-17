package org.jbpm.ant;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.impl.SessionFactoryImpl;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;

public class ShutDownHsqldb extends Task {

  public void execute() throws BuildException {
    Connection connection = null;
    JbpmConfiguration jbpmConfiguration = AntHelper.getJbpmConfiguration(null);
    JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
    try {
      DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) jbpmContext.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
      SessionFactoryImpl sessionFactory = (SessionFactoryImpl) dbPersistenceServiceFactory.getSessionFactory();
      ConnectionProvider connectionProvider = sessionFactory.getConnectionProvider();
      connection = connectionProvider.getConnection();
      Statement statement = connection.createStatement();
      log("shutting down database");
      statement.executeUpdate("SHUTDOWN");
      connection.close();
      
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      jbpmContext.close();
    }
  }

}
