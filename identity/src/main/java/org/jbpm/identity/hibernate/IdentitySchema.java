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
package org.jbpm.identity.hibernate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.util.JDBCExceptionReporter;

import org.jbpm.JbpmException;

public class IdentitySchema {

  private static final String IDENTITY_TABLE_PATTERN = "JBPM_ID_%";
  private static final String[] TABLE_TYPES = {
    "TABLE"
  };

  private final Configuration configuration;
  private ConnectionProvider connectionProvider;

  public IdentitySchema(Configuration configuration) {
    this.configuration = configuration;
  }

  private Dialect getDialect() {
    return Dialect.getDialect(configuration.getProperties());
  }

  private String getDefaultCatalog() {
    return configuration.getProperty(Environment.DEFAULT_CATALOG);
  }

  private String getDefaultSchema() {
    return configuration.getProperty(Environment.DEFAULT_SCHEMA);
  }

  // scripts lazy initializations /////////////////////////////////////////////

  public String[] getCreateSql() {
    return configuration.generateSchemaCreationScript(getDialect());
  }

  public String[] getDropSql() {
    return configuration.generateDropSchemaScript(getDialect());
  }

  public String[] getCleanSql() {
    List dropForeignKeysSql = new ArrayList();
    List createForeignKeysSql = new ArrayList();

    Dialect dialect = getDialect();
    String defaultCatalog = getDefaultCatalog();
    String defaultSchema = getDefaultSchema();
    Mapping mapping = configuration.buildMapping();

    // loop over all table mappings
    for (Iterator tm = configuration.getTableMappings(); tm.hasNext();) {
      Table table = (Table) tm.next();
      if (!table.isPhysicalTable()) continue;

      for (Iterator subIter = table.getForeignKeyIterator(); subIter.hasNext();) {
        ForeignKey foreignKey = (ForeignKey) subIter.next();
        if (foreignKey.isPhysicalConstraint()) {
          // collect the drop key constraint
          dropForeignKeysSql.add(foreignKey.sqlDropString(dialect, defaultCatalog, defaultSchema));
          createForeignKeysSql.add(foreignKey.sqlCreateString(dialect, mapping, defaultCatalog, defaultSchema));
        }
      }
    }

    List deleteSql = dropForeignKeysSql;
    for (Iterator iter = configuration.getTableMappings(); iter.hasNext();) {
      Table table = (Table) iter.next();
      deleteSql.add("delete from " + table.getName());
    }

    List cleanSqlList = dropForeignKeysSql;
    cleanSqlList.addAll(createForeignKeysSql);

    return (String[]) cleanSqlList.toArray(new String[cleanSqlList.size()]);
  }

  // runtime table detection //////////////////////////////////////////////////

  public boolean hasIdentityTables() {
    return !getIdentityTables().isEmpty();
  }

  public List getIdentityTables() {
    // delete all the data in the jbpm tables
    Connection connection = null;
    try {
      connection = createConnection();

      List identityTables = new ArrayList();
      ResultSet resultSet = connection.getMetaData()
        .getTables(null, null, IDENTITY_TABLE_PATTERN, TABLE_TYPES);
      try {
        while (resultSet.next()) {
          String tableName = resultSet.getString("TABLE_NAME");
          if (tableName != null && tableName.length() > 5
            && IDENTITY_TABLE_PATTERN.equalsIgnoreCase(tableName.substring(0, 5))) {
            identityTables.add(tableName);
          }
        }
      }
      finally {
        resultSet.close();
      }
      return identityTables;
    }
    catch (SQLException e) {
      throw new JbpmException("could not get identity tables");
    }
    finally {
      closeConnection(connection);
    }
  }

  // script execution methods /////////////////////////////////////////////////

  public void dropSchema() {
    execute(getDropSql());
  }

  public void createSchema() {
    execute(getCreateSql());
  }

  public void cleanSchema() {
    execute(getCleanSql());
  }

  public void saveSqlScripts(String dir, String prefix) {
    try {
      new File(dir).mkdirs();
      saveSqlScript(dir + "/" + prefix + ".drop.sql", getDropSql());
      saveSqlScript(dir + "/" + prefix + ".create.sql", getCreateSql());
      saveSqlScript(dir + "/" + prefix + ".clean.sql", getCleanSql());
      new SchemaExport(configuration).setDelimiter(getSqlDelimiter()).setOutputFile(dir + "/"
        + prefix + ".drop.create.sql").create(true, false);
    }
    catch (IOException e) {
      throw new JbpmException("couldn't generate scripts", e);
    }
  }

  // main /////////////////////////////////////////////////////////////////////

  public static void main(String[] args) {
    if (args == null || args.length == 0) {
      syntax();
    }
    else if ("create".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).createSchema();
    }
    else if ("drop".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).dropSchema();
    }
    else if ("clean".equalsIgnoreCase(args[0])) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).cleanSchema();
    }
    else if ("scripts".equalsIgnoreCase(args[0]) && args.length == 3) {
      new IdentitySchema(IdentitySessionFactory.createConfiguration()).saveSqlScripts(args[1], args[2]);
    }
    else {
      syntax();
    }
  }

  private static void syntax() {
    System.err.println("syntax:");
    System.err.println("IdentitySchema create");
    System.err.println("IdentitySchema drop");
    System.err.println("IdentitySchema clean");
    System.err.println("IdentitySchema scripts <dir> <prefix>");
  }

  private void saveSqlScript(String fileName, String[] sql) throws FileNotFoundException {
    FileOutputStream fileOutputStream = new FileOutputStream(fileName);
    PrintStream printStream = new PrintStream(fileOutputStream);
    for (int i = 0; i < sql.length; i++) {
      printStream.println(sql[i] + getSqlDelimiter());
    }
  }

  // sql script execution /////////////////////////////////////////////////////

  public void execute(String[] script) {
    Connection connection = null;
    try {
      connection = createConnection();
      Statement statement = connection.createStatement();
      try {
        boolean showSql = getShowSql();
        for (int i = 0; i < script.length; i++) {
          String sql = script[i];
          if (showSql) System.out.println(sql);
          statement.executeUpdate(sql);
        }
      }
      finally {
        statement.close();
      }
    }
    catch (SQLException e) {
      throw new JbpmException("failed to execute sql", e);
    }
    finally {
      closeConnection(connection);
    }
  }

  private boolean getShowSql() {
    return "true".equalsIgnoreCase(configuration.getProperty(Environment.SHOW_SQL));
  }

  private void closeConnection(Connection connection) {
    if (connectionProvider != null) {
      try {
        if (connection != null) {
          JDBCExceptionReporter.logAndClearWarnings(connection);
          connectionProvider.closeConnection(connection);
        }
      }
      catch (SQLException e) {
        JDBCExceptionReporter.logExceptions(e);
      }
      finally {
        connectionProvider.close();
        connectionProvider = null;
      }
    }
  }

  private Connection createConnection() throws SQLException {
    try {
      connectionProvider = ConnectionProviderFactory.newConnectionProvider(configuration.getProperties());
    }
    catch (HibernateException e) {
      throw new SQLException(e.getMessage());
    }
    Connection connection = connectionProvider.getConnection();
    if (connection.getAutoCommit() == false) {
      connection.commit();
      connection.setAutoCommit(true);
    }
    return connection;
  }

  public Properties getProperties() {
    return configuration.getProperties();
  }

  // sql delimiter ////////////////////////////////////////////////////////////

  private static String sqlDelimiter;

  private synchronized String getSqlDelimiter() {
    if (sqlDelimiter == null) {
      sqlDelimiter = getProperties().getProperty("jbpm.sql.delimiter", ";");
    }
    return sqlDelimiter;
  }

}
