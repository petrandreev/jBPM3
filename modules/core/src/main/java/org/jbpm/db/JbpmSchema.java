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
package org.jbpm.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;
import org.hibernate.util.JDBCExceptionReporter;
import org.jbpm.JbpmException;

/**
 * utilities for the jBPM database schema.
 */
public class JbpmSchema {

  final Configuration configuration;
  final Settings settings;

  ConnectionProvider connectionProvider = null;
  Connection connection = null;

  final List<SQLException> exceptions = new ArrayList<SQLException>();

  public JbpmSchema(Configuration configuration) {
    this.configuration = configuration;
    this.settings = configuration.buildSettings();
  }

  public String[] getCreateSql() {
    return configuration.generateSchemaCreationScript(settings.getDialect());
  }

  public String[] getDropSql() {
    return configuration.generateDropSchemaScript(settings.getDialect());
  }

  public String[] getCleanSql() {
    return concat(getDropSql(), getCreateSql());
  }

  public Set<String> getJbpmTables() {
    Set<String> jbpmTables = new HashSet<String>();
    for (Iterator<?> i = configuration.getTableMappings(); i.hasNext();) {
      Table table = (Table) i.next();
      if (table.isPhysicalTable()) {
        jbpmTables.add(table.getName());
      }
    }
    return jbpmTables;
  }

  public Set<String> getExistingTables() {
    try {
      createConnection();
      Set<String> existingTables = new HashSet<String>();

      DatabaseMetaData metaData = connection.getMetaData();
      boolean storesLowerCaseIdentifiers = metaData.storesLowerCaseIdentifiers();
      ResultSet resultSet = metaData.getTables(settings.getDefaultCatalogName(),
          settings.getDefaultSchemaName(), null, new String[] { "TABLE" });
      try {
        while (resultSet.next()) {
          String tableName = resultSet.getString("TABLE_NAME");
          if (storesLowerCaseIdentifiers) {
            tableName = tableName.toUpperCase();
          }
          existingTables.add(tableName);
        }
      }
      finally {
        resultSet.close();
      }
      return existingTables;
    }
    catch (SQLException e) {
      throw new JbpmException("could not get available table names", e);
    }
    finally {
      closeConnection();
    }
  }

  public Map<String, Long> getRowsPerTable() {
    Map<String, Long> rowsPerTable = new HashMap<String, Long>();
    try {
      createConnection();
      Statement statement = connection.createStatement();
      for (String tableName : getJbpmTables()) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        ResultSet resultSet = statement.executeQuery(sql);
        if (!resultSet.next()) throw new JbpmException("empty result set: " + sql);

        long count = resultSet.getLong(1);
        if (resultSet.wasNull()) throw new JbpmException("count was null: " + sql);

        rowsPerTable.put(tableName, count);
        resultSet.close();
      }
      statement.close();
    }
    catch (SQLException e) {
      throw new JbpmException("could not count records", e);
    }
    finally {
      closeConnection();
    }
    return rowsPerTable;
  }

  public void dropSchema() {
    try {
      execute(getDropSql());
    }
    catch (SQLException e) {
      throw new JbpmException("could not drop schema", e);
    }
  }

  public void createSchema() {
    try {
      execute(getCreateSql());
    }
    catch (SQLException e) {
      throw new JbpmException("could not create schema", e);
    }
  }

  public void cleanSchema() {
    try {
      execute(getCleanSql());
    }
    catch (SQLException e) {
      throw new JbpmException("could not clean schema", e);
    }
  }

  public void saveSqlScripts(String dir, String prefix) {
    try {
      new File(dir).mkdirs();
      saveSqlScript(dir + "/" + prefix + ".drop.sql", getDropSql());
      saveSqlScript(dir + "/" + prefix + ".create.sql", getCreateSql());
      saveSqlScript(dir + "/" + prefix + ".clean.sql", getCleanSql());
      saveSqlScript(dir + "/" + prefix + ".drop.create.sql", concat(getDropSql(), getCreateSql()));
    }
    catch (IOException e) {
      throw new JbpmException("couldn't generate scripts", e);
    }
  }

  private static String[] concat(String[] array1, String[] array2) {
    int length1 = array1.length;
    int length2 = array2.length;
    String[] result = new String[length1 + length2];
    System.arraycopy(array1, 0, result, 0, length1);
    System.arraycopy(array2, 0, result, length1, length2);
    return result;
  }

  public boolean tableExists(String tableName) {
    Table table = findTableMapping(tableName);
    try {
      createConnection();
      return getTableMetadata(table) != null;
    }
    catch (SQLException e) {
      throw new JbpmException("could not tell whether table exists: " + tableName, e);
    }
    finally {
      closeConnection();
    }
  }

  public void createTable(String tableName) {
    Table table = findTableMapping(tableName);
    String sql = table.sqlCreateString(settings.getDialect(), configuration.buildMapping(),
        settings.getDefaultCatalogName(), settings.getDefaultSchemaName());
    try {
      execute(sql);
    }
    catch (SQLException e) {
      throw new JbpmException("could not create table: " + tableName, e);
    }
  }

  public void updateTable(String tableName) {
    Table table = findTableMapping(tableName);
    try {
      createConnection();
      Iterator<?> sqls = table.sqlAlterStrings(settings.getDialect(), configuration.buildMapping(),
          getTableMetadata(table), settings.getDefaultCatalogName(),
          settings.getDefaultSchemaName());

      Statement statement = connection.createStatement();
      while (sqls.hasNext()) {
        String sql = (String) sqls.next();
        statement.executeUpdate(sql);
      }
      statement.close();
    }
    catch (SQLException e) {
      throw new JbpmException("could not update table: " + tableName, e);
    }
    finally {
      closeConnection();
    }
  }

  public List<SQLException> getExceptions() {
    return exceptions;
  }

  private Table findTableMapping(String tableName) {
    for (Iterator<?> i = configuration.getTableMappings(); i.hasNext();) {
      Table table = (Table) i.next();
      if (tableName.equals(table.getName())) {
        return table;
      }
    }
    throw new JbpmException("no mapping found for table: " + tableName);
  }

  private TableMetadata getTableMetadata(Table table) throws SQLException {
    DatabaseMetadata databaseMetadata = new DatabaseMetadata(connection, settings.getDialect());
    return databaseMetadata.getTableMetadata(table.getName(),
        table.getSchema() == null ? settings.getDefaultSchemaName() : table.getSchema(),
        table.getCatalog() == null ? settings.getDefaultCatalogName() : table.getCatalog(),
        table.isQuoted());
  }

  public static void main(String[] args) {
    if ((args == null) || (args.length == 0)) {
      syntax();
    }
    else if ("create".equalsIgnoreCase(args[0]) && args.length <= 3) {
      Configuration configuration = createConfiguration(args, 1);
      new JbpmSchema(configuration).createSchema();
    }
    else if ("drop".equalsIgnoreCase(args[0]) && args.length <= 3) {
      Configuration configuration = createConfiguration(args, 1);
      new JbpmSchema(configuration).dropSchema();
    }
    else if ("clean".equalsIgnoreCase(args[0]) && args.length <= 3) {
      Configuration configuration = createConfiguration(args, 1);
      new JbpmSchema(configuration).cleanSchema();
    }
    else if ("scripts".equalsIgnoreCase(args[0]) && args.length >= 3 && args.length <= 5) {
      Configuration configuration = createConfiguration(args, 3);
      new JbpmSchema(configuration).saveSqlScripts(args[1], args[2]);
    }
    else {
      syntax();
    }
  }

  private static void syntax() {
    System.err.println("syntax:");
    System.err.println("JbpmSchema create [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema drop [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema clean [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema scripts <dir> <prefix> [<hibernate.cfg.xml> [<hibernate.properties>]]");
  }

  static Configuration createConfiguration(String[] args, int index) {
    String hibernateCfgXml = (args.length > index ? args[index] : "hibernate.cfg.xml");
    String hibernateProperties = (args.length > (index + 1) ? args[index + 1] : null);

    Configuration configuration = new Configuration();
    configuration.configure(new File(hibernateCfgXml));
    if (hibernateProperties != null) {
      try {
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(hibernateProperties);
        properties.load(inputStream);
        configuration.setProperties(properties);
      }
      catch (IOException e) {
        throw new JbpmException("couldn't load hibernate configuration", e);
      }
    }

    return configuration;
  }

  void saveSqlScript(String fileName, String[] sql) throws IOException {
    PrintStream out = new PrintStream(new FileOutputStream(fileName));
    try {
      for (String line : sql) {
        out.println(line + getSqlDelimiter());
      }
    }
    finally {
      out.close();
    }
  }

  void execute(String... sqls) throws SQLException {
    boolean showSql = settings.isShowSqlEnabled();
    exceptions.clear();
    try {
      createConnection();
      Statement statement = connection.createStatement();
      for (String sql : sqls) {
        if (showSql) System.out.println(sql);
        log.debug(sql);
        try {
          statement.executeUpdate(sql);
        }
        catch (SQLException e) {
          exceptions.add(e);
          log.debug(e.getMessage());
        }
      }
      statement.close();
    }
    finally {
      closeConnection();
    }
  }

  void createConnection() throws SQLException {
    connectionProvider = settings.getConnectionProvider();
    connection = connectionProvider.getConnection();
    if (!connection.getAutoCommit()) {
      connection.commit();
      connection.setAutoCommit(true);
    }
  }

  void closeConnection() {
    if (connection != null) {
      try {
        JDBCExceptionReporter.logAndClearWarnings(connection);
        connectionProvider.closeConnection(connection);
      }
      catch (SQLException e) {
        log.debug("could not close " + connection, e);
      }
      finally {
        connectionProvider.close();
      }
    }
  }

  public Properties getProperties() {
    return configuration.getProperties();
  }

  // sql delimiter ////////////////////////////////////////////////////////////

  static String sqlDelimiter = null;

  synchronized String getSqlDelimiter() {
    if (sqlDelimiter == null) {
      sqlDelimiter = getProperties().getProperty("jbpm.sql.delimiter", ";");
    }
    return sqlDelimiter;
  }

  // logger ///////////////////////////////////////////////////////////////////

  private static final Log log = LogFactory.getLog(JbpmSchema.class);
}
