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
package org.jbpm.mock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.hsqldb.jdbc.jdbcDataSource;

public class Jdbc {
  
  public static void clearHsqlInMemoryDatabase() {
    try {
      Connection connection = DriverManager.getConnection("jdbc:hsqldb:mem:jbpm-mock-db");
      PreparedStatement preparedStatement = connection.prepareStatement("SHUTDOWN");
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("problems cleaning hsql in memory db", e);
    }
  }
  
  static Class[] recordedConnectionInterfaces = new Class[]{Connection.class, Recorded.class};
  public static class MockDataSource extends jdbcDataSource {
    private static final long serialVersionUID = 1L;
    public MockDataSource() {
      setDatabase("jdbc:hsqldb:mem:jbpm-mock-db");
      setUser("sa");
      setPassword("");
    }
    public Connection getConnection() throws SQLException {
      Connection connection = super.getConnection();
      return (Connection) ProxyEnhancer.addRecorder(connection, recordedConnectionInterfaces);
    }
    public Connection getConnection(String userName, String password) throws SQLException {
      Connection connection = super.getConnection(userName, password);
      return (Connection) ProxyEnhancer.addRecorder(connection, recordedConnectionInterfaces);
    }
  }

  private static Class[] recordedDataSourceInterfaces = new Class[]{DataSource.class, Recorded.class};
  public static DataSource createRecordedDataSource() {
    MockDataSource dataSource = new MockDataSource();
    return (DataSource) ProxyEnhancer.addRecorder(dataSource, recordedDataSourceInterfaces);
  }
}
