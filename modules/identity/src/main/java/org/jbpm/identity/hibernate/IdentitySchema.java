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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.util.JDBCExceptionReporter;
import org.jbpm.JbpmException;

public class IdentitySchema {

  private static final String IDENTITY_TABLE_PREFIX = "JBPM_ID_";

  Configuration configuration = null;
  Settings settings;
  String[] createSql = null;
  String[] dropSql = null;
  String[] cleanSql = null;

  ConnectionProvider connectionProvider = null;
  Connection connection = null;
  Statement statement = null;

  public IdentitySchema(Configuration configuration) {
    this.configuration = configuration;
    this.settings = configuration.buildSettings();
  }

  // scripts lazy initializations /////////////////////////////////////////////

  public String[] getCreateSql() {
    if (createSql == null) {
      createSql = configuration.generateSchemaCreationScript(settings.getDialect());
    }
    return createSql;
  }

  public String[] getDropSql() {
    if (dropSql == null) {
      dropSql = configuration.generateDropSchemaScript(settings.getDialect());
    }
    return dropSql;
  }

  public String[] getCleanSql() {
    if (cleanSql == null) {
      // loop over all foreign key constraints
      Dialect dialect = settings.getDialect();
      String catalog = settings.getDefaultCatalogName();
      String schema = settings.getDefaultSchemaName();
      Mapping mapping = configuration.buildMapping();

      List<String> dropForeignKeysSql = new ArrayList<String>();
      List<String> createForeignKeysSql = new ArrayList<String>();
      Iterator<?> iter = configuration.getTableMappings();
      while (iter.hasNext()) {
        Table table = (Table) iter.next();
        if (table.isPhysicalTable()) {
          Iterator<?> subIter = table.getForeignKeyIterator();
          while (subIter.hasNext()) {
            ForeignKey fk = (ForeignKey) subIter.next();
            if (fk.isPhysicalConstraint()) {
              // collect the drop key constraint
              dropForeignKeysSql.add(fk.sqlDropString(dialect, catalog, schema));
              createForeignKeysSql.add(fk.sqlCreateString(dialect, mapping, catalog, schema));
            }
          }
        }
      }

      List<String> deleteSql = new ArrayList<String>();
      iter = configuration.getTableMappings();
      while (iter.hasNext()) {
        Table table = (Table) iter.next();
        deleteSql.add("delete from " + table.getName());
      }

      List<String> cleanSqlList = dropForeignKeysSql;
      cleanSqlList.addAll(deleteSql);
      cleanSqlList.addAll(createForeignKeysSql);

      cleanSql = cleanSqlList.toArray(new String[cleanSqlList.size()]);
    }
    return cleanSql;
  }

  // runtime table detection //////////////////////////////////////////////////

  public boolean hasIdentityTables() {
    return (getIdentityTables().size() > 0);
  }

  public List<String> getIdentityTables() {
    // delete all the data in the jbpm tables
    List<String> jbpmTableNames = new ArrayList<String>();
    try {
      createConnection();
      ResultSet resultSet = connection.getMetaData().getTables("", "", null, null);
      while (resultSet.next()) {
        String tableName = resultSet.getString("TABLE_NAME");
        if ((tableName != null)
            && (tableName.length() > 5)
            && (IDENTITY_TABLE_PREFIX.equalsIgnoreCase(tableName.substring(0, 5)))) {
          jbpmTableNames.add(tableName);
        }
      }
    }
    catch (SQLException e) {
      throw new JbpmException("couldn't get the jbpm table names");
    }
    finally {
      closeConnection();
    }
    return jbpmTableNames;
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
      new SchemaExport(configuration).setDelimiter(getSqlDelimiter()).setOutputFile(dir
          + "/"
          + prefix
          + ".drop.create.sql").create(true, false);
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

  public void execute(String[] sqls) {
    String sql = null;
    boolean showSql = settings.isShowSqlEnabled();

    try {
      createConnection();
      statement = connection.createStatement();

      for (int i = 0; i < sqls.length; i++) {
        sql = sqls[i];
        String delimitedSql = sql + getSqlDelimiter();

        if (showSql)
          log.debug(delimitedSql);
        statement.executeUpdate(delimitedSql);
      }

    }
    catch (SQLException e) {
      throw new JbpmException("couldn't execute sql '" + sql + "'", e);
    }
    finally {
      closeConnection();
    }
  }

  private void closeConnection() {
    if (statement != null) {
      try {
        statement.close();
      }
      catch (SQLException e) {
        log.debug("couldn't close jdbc statement", e);
      }
    }
    if (connection != null) {
      try {
        JDBCExceptionReporter.logWarnings(connection.getWarnings());
        connection.clearWarnings();
        connectionProvider.closeConnection(connection);
        connectionProvider.close();
      }
      catch (SQLException e) {
        log.debug("couldn't close jdbc connection", e);
      }
    }
  }

  private void createConnection() throws SQLException {
    connectionProvider = settings.getConnectionProvider();
    connection = connectionProvider.getConnection();
    if (!connection.getAutoCommit()) {
      connection.commit();
      connection.setAutoCommit(true);
    }
  }

  public Properties getProperties() {
    return configuration.getProperties();
  }

  // sql delimiter ////////////////////////////////////////////////////////////

  private static String sqlDelimiter = null;

  private synchronized String getSqlDelimiter() {
    if (sqlDelimiter == null) {
      sqlDelimiter = getProperties().getProperty("jbpm.sql.delimiter", ";");
    }
    return sqlDelimiter;
  }

  // logger ///////////////////////////////////////////////////////////////////

  private static final Log log = LogFactory.getLog(IdentitySchema.class);
}
