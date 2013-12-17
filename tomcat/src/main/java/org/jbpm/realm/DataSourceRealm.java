/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jbpm.realm;

import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.catalina.ServerFactory;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.naming.ContextBindings;

/**
 * Realm implementation that works with any JDBC data source. Unlike the standard
 * {@link org.apache.catalina.realm.DataSourceRealm DataSourceRealm} provided by Tomcat, this
 * implementation allows for a wider variety of database schemas by externalizing the queries
 * used to retrieve users and roles.
 * 
 * @see <a href="http://tomcat.apache.org/tomcat-6.0-doc/realm-howto.html">Realm How-To</a>
 * @author Alejandro Guizar
 */
public class DataSourceRealm extends RealmBase {

  /** The JNDI name of the data source */
  private String dataSourceName;

  /** Context-local datasource. */
  private boolean localDataSource;

  /** SQL query for retrieving the password associated to a user name. */
  private String userQuery;

  /** SQL query for retrieving the roles associated to a user name. */
  private String roleQuery;

  /**
   * Return the JNDI name of the data source.
   */
  public String getDataSourceName() {
    return dataSourceName;
  }

  /**
   * Set the JNDI name of the data source.
   * 
   * @param dataSourceName the JNDI name of the data source.
   */
  public void setDataSourceName(String dataSourceName) {
    this.dataSourceName = dataSourceName;
  }

  /**
   * Tells whether the realm uses a data source defined for the enclosing Context rather than a
   * global data source.
   */
  public boolean getLocalDataSource() {
    return localDataSource;
  }

  /**
   * Sets whether the realm uses a data source defined for the enclosing Context rather than a
   * global data source.
   * 
   * @param localDataSource the new flag value
   */
  public void setLocalDataSource(boolean localDataSource) {
    this.localDataSource = localDataSource;
  }

  /** Returns the SQL query for retrieving the password associated to a user name. */
  public String getUserQuery() {
    return userQuery;
  }

  /**
   * Sets the the SQL query for retrieving the password associated to a user name. The query
   * must
   * 
   * @param userQuery
   */
  public void setUserQuery(String userQuery) {
    this.userQuery = userQuery;
  }

  /** Returns the SQL query for retrieving the roles associated to a user name. */
  public String getRoleQuery() {
    return roleQuery;
  }

  /**
   * Sets the SQL query for retrieving the roles associated to a user name.
   * 
   * @param rolesQuery
   */
  public void setRoleQuery(String rolesQuery) {
    this.roleQuery = rolesQuery;
  }

  protected String getName() {
    return "JbpmConsoleRealm";
  }

  protected String getPassword(String username) {
    // Ensure that we have an open database connection
    Connection dbConnection = open();
    if (dbConnection != null) {
      try {
        return getPassword(dbConnection, username);
      }
      finally {
        close(dbConnection);
      }
    }
    return null;
  }

  /**
   * Return the password associated with the given principal's user name.
   * 
   * @param dbConnection The database connection to be used
   * @param username user for which password should be retrieved
   */
  private String getPassword(Connection dbConnection, String username) {
    PreparedStatement statement = null;
    try {
      statement = dbConnection.prepareStatement(userQuery);
      statement.setString(1, username);

      ResultSet resultSet = statement.executeQuery();
      if (resultSet.next()) return resultSet.getString(1);
    }
    catch (SQLException e) {
      containerLog.error(sm.getString("dataSourceRealm.getPassword.exception", username));
    }
    finally {
      if (statement != null) {
        try {
          // When a Statement is closed, its current ResultSet, if one exists, is also closed
          statement.close();
        }
        catch (SQLException e) {
          containerLog.error(sm.getString("dataSourceRealm.getPassword.exception", username));
        }
      }
    }
    return null;
  }

  protected Principal getPrincipal(String username) {
    Connection dbConnection = open();
    if (dbConnection != null) {
      try {
        return (new GenericPrincipal(this,
          username,
          getPassword(dbConnection, username),
          getRoles(dbConnection, username)));
      }
      finally {
        close(dbConnection);
      }
    }
    return new GenericPrincipal(this, username, null, null);
  }

  private List getRoles(Connection dbConnection, String username) {
    PreparedStatement statement = null;
    try {
      statement = dbConnection.prepareStatement(roleQuery);
      statement.setString(1, username);

      ResultSet rs = statement.executeQuery();
      List roles = new ArrayList();
      while (rs.next()) {
        roles.add(rs.getString(1));
      }
      return roles;
    }
    catch (SQLException e) {
      containerLog.error(sm.getString("dataSourceRealm.getRoles.exception", username));
      return null;
    }
    finally {
      if (statement != null) {
        try {
          // When a Statement is closed, its current ResultSet, if one exists, is also closed
          statement.close();
        }
        catch (SQLException e) {
          containerLog.error(sm.getString("dataSourceRealm.getRoles.exception", username));
        }
      }
    }
  }

  private Connection open() {
    try {
      Context context;
      if (localDataSource) {
        context = (Context) ContextBindings.getClassLoader().lookup("comp/env");
      }
      else {
        StandardServer server = (StandardServer) ServerFactory.getServer();
        context = server.getGlobalNamingContext();
      }
      DataSource dataSource = (DataSource) context.lookup(dataSourceName);
      return dataSource.getConnection();
    }
    catch (NamingException e) {
      containerLog.error(sm.getString("dataSourceRealm.exception"), e);
    }
    catch (SQLException e) {
      containerLog.error(sm.getString("dataSourceRealm.exception"), e);
    }
    return null;
  }

  /**
   * Close the specified database connection.
   * 
   * @param dbConnection The connection to be closed
   */
  private void close(Connection dbConnection) {
    // Commit if not auto committed
    try {
      if (!dbConnection.getAutoCommit()) {
        dbConnection.commit();
      }
    }
    catch (SQLException e) {
      containerLog.error("Exception committing connection before closing:", e);
    }

    // Close this database connection, and log any errors
    try {
      dbConnection.close();
    }
    catch (SQLException e) {
      containerLog.error(sm.getString("dataSourceRealm.close"), e); // Just log it here
    }
  }
}
