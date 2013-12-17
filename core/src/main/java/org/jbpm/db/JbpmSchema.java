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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.tool.hbm2ddl.ColumnMetadata;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;
import org.hibernate.util.JDBCExceptionReporter;

import org.jbpm.JbpmException;
import org.jbpm.util.IoUtil;

/**
 * utilities for the jBPM database schema.
 */
public class JbpmSchema {

  private final Configuration configuration;
  private ConnectionProvider connectionProvider;
  private String delimiter;
  private final List exceptions = new ArrayList();

  private static final String[] EMPTY_STRING_ARRAY = {};
  private static final String[] TABLE_TYPES = { "TABLE" };

  public JbpmSchema(Configuration configuration) {
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

  private boolean getShowSql() {
    return "true".equalsIgnoreCase(configuration.getProperty(Environment.SHOW_SQL));
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public List getExceptions() {
    return exceptions;
  }

  public String[] getCreateSql() {
    return configuration.generateSchemaCreationScript(getDialect());
  }

  public String[] getDropSql() {
    return configuration.generateDropSchemaScript(getDialect());
  }

  public String[] getCleanSql() {
    return concat(getDropSql(), getCreateSql());
  }

  private static String[] concat(String[] array1, String[] array2) {
    int length1 = array1.length;
    int length2 = array2.length;
    String[] result = new String[length1 + length2];
    System.arraycopy(array1, 0, result, 0, length1);
    System.arraycopy(array2, 0, result, length1, length2);
    return result;
  }

  public String[] getUpdateSql() {
    Connection connection = null;
    try {
      connection = createConnection();
      return configuration
        .generateSchemaUpdateScript(getDialect(), getDatabaseMetadata(connection));
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to generate update sql");

      return EMPTY_STRING_ARRAY;
    }
    finally {
      closeConnection(connection);
    }
  }

  public void dropSchema() {
    try {
      execute(getDropSql());
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to drop schema");
    }
  }

  public void createSchema() {
    try {
      execute(getCreateSql());
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to create schema");
    }
  }

  public void cleanSchema() {
    try {
      execute(getCleanSql());
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to clean schema");
    }
  }

  public void updateSchema() {
    try {
      execute(getUpdateSql());
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to update schema");
    }
  }

  private void execute(String[] script) throws SQLException {
    Connection connection = null;
    try {
      connection = createConnection();
      Statement statement = connection.createStatement();
      try {
        final boolean showSql = getShowSql();
        for (int i = 0; i < script.length; i++) {
          String sql = script[i];
          if (showSql) System.out.println(sql);
          execute(sql, statement);
        }
      }
      finally {
        statement.close();
      }
    }
    finally {
      closeConnection(connection);
    }
  }

  private void execute(String sql, Statement statement) {
    try {
      statement.executeUpdate(sql);

      SQLWarning warning = statement.getWarnings();
      if (warning != null) {
        JDBCExceptionReporter.logWarnings(warning);
        statement.clearWarnings();
      }
    }
    catch (SQLException e) {
      JDBCExceptionReporter.logExceptions(e, "failed to execute update");
      exceptions.add(e);
    }
  }

  public void saveSqlScripts(String dir, String prefix) {
    File path = new File(dir);
    if (!path.isDirectory()) {
      throw new JbpmException(path + " is not a directory");
    }

    try {
      saveSqlScript(new File(path, prefix + ".drop.sql"), getDropSql());
      saveSqlScript(new File(path, prefix + ".create.sql"), getCreateSql());
      saveSqlScript(new File(path, prefix + ".clean.sql"), getCleanSql());
    }
    catch (IOException e) {
      throw new JbpmException("failed to generate scripts", e);
    }
  }

  private void saveSqlScript(File file, String[] script) throws IOException {
    Writer writer = new FileWriter(file);
    try {
      writeSql(writer, script);
    }
    finally {
      writer.close();
    }
  }

  public void writeSql(Writer writer, String[] script) throws IOException {
    for (int i = 0; i < script.length; i++) {
      writer.write(script[i]);
      if (delimiter != null) writer.write(delimiter);
      writer.write(IoUtil.lineSeparator);
    }
  }

  public Set getJbpmTables() {
    Set jbpmTables = new HashSet();
    for (Iterator i = configuration.getTableMappings(); i.hasNext();) {
      Table table = (Table) i.next();
      if (table.isPhysicalTable()) {
        jbpmTables.add(table.getName());
      }
    }
    return jbpmTables;
  }

  public Map getRowsPerTable() {
    Connection connection = null;
    try {
      connection = createConnection();
      Map rowsPerTable = new HashMap();

      Statement statement = connection.createStatement();
      try {
        for (Iterator i = getJbpmTables().iterator(); i.hasNext();) {
          String tableName = (String) i.next();
          String sql = "SELECT COUNT(*) FROM " + tableName;

          ResultSet resultSet = statement.executeQuery(sql);
          if (!resultSet.next()) continue;

          long count = resultSet.getLong(1);
          if (resultSet.wasNull()) continue;

          rowsPerTable.put(tableName, new Long(count));
          resultSet.close();
        }
      }
      finally {
        statement.close();
      }

      return rowsPerTable;
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "could not count records");

      return Collections.EMPTY_MAP;
    }
    finally {
      closeConnection(connection);
    }
  }

  public Set getExistingTables() {
    Connection connection = null;
    try {
      connection = createConnection();
      Set existingTables = new HashSet();

      DatabaseMetaData metaData = connection.getMetaData();
      boolean storesLowerCaseIdentifiers = metaData.storesLowerCaseIdentifiers();
      ResultSet resultSet = metaData
        .getTables(getDefaultCatalog(), getDefaultSchema(), null, TABLE_TYPES);
      try {
        while (resultSet.next()) {
          String tableName = resultSet.getString("TABLE_NAME");
          if (storesLowerCaseIdentifiers) tableName = tableName.toUpperCase();
          existingTables.add(tableName);
        }
      }
      finally {
        resultSet.close();
      }

      return existingTables;
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "could not get available table names");

      return Collections.EMPTY_SET;
    }
    finally {
      closeConnection(connection);
    }
  }

  public boolean tableExists(String tableName) {
    Connection connection = null;
    try {
      connection = createConnection();

      DatabaseMetaData metaData = connection.getMetaData();
      ResultSet resultSet = metaData
        .getTables(getDefaultCatalog(), getDefaultSchema(), tableName, TABLE_TYPES);
      try {
        return resultSet.next();
      }
      finally {
        resultSet.close();
      }
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "could not determine whether table exists");

      return false;
    }
    finally {
      closeConnection(connection);
    }
  }

  public void updateTable(String tableName) {
    Table table = findTableMapping(tableName);

    Connection connection = null;
    try {
      connection = createConnection();
      TableMetadata tableInfo = getTableMetadata(connection, table);

      Statement statement = connection.createStatement();
      try {
        if (tableInfo != null) {
          for (Iterator script = sqlAlterStrings(table, tableInfo); script.hasNext();) {
            String sql = (String) script.next();
            execute(sql, statement);
          }
        }
        else {
          execute(sqlCreateString(table), statement);
        }
      }
      finally {
        statement.close();
      }
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to update table");
    }
    finally {
      closeConnection(connection);
    }
  }

  private Table findTableMapping(String tableName) {
    for (Iterator i = configuration.getTableMappings(); i.hasNext();) {
      Table table = (Table) i.next();
      if (tableName.equals(table.getName())) return table;
    }
    throw new JbpmException("no mapping found for table: " + tableName);
  }

  private TableMetadata getTableMetadata(Connection connection, Table table)
    throws SQLException {
    String tableSchema = table.getSchema();
    if (tableSchema == null) tableSchema = getDefaultSchema();

    String tableCatalog = table.getCatalog();
    if (tableCatalog == null) tableCatalog = getDefaultCatalog();

    return getDatabaseMetadata(connection)
      .getTableMetadata(table.getName(), tableSchema, tableCatalog, table.isQuoted());
  }

  private DatabaseMetadata getDatabaseMetadata(Connection connection) throws SQLException {
    return new DatabaseMetadata(connection, getDialect());
  }

  /**
   * Workaround for bug in {@link Table#sqlAlterStrings}.
   * 
   * @param table TODO
   * @param tableMetadata TODO
   * @see <a
   *      href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-4457">HHH-4457</a>
   */
  private Iterator sqlAlterStrings(Table table, TableMetadata tableMetadata)
    throws SQLException {
    Dialect dialect = getDialect();
    Mapping mapping = configuration.buildMapping();

    StringBuffer root = new StringBuffer("alter table ")
      .append(table.getQualifiedName(dialect, getDefaultCatalog(), getDefaultSchema()))
      .append(' ')
      .append(dialect.getAddColumnString());
    int rootLength = root.length();

    List results = new ArrayList();
    for (Iterator iter = table.getColumnIterator(); iter.hasNext();) {
      Column column = (Column) iter.next();

      ColumnMetadata columnInfo = tableMetadata.getColumnMetadata(column.getName());
      if (columnInfo == null) {
        // the column doesn't exist at all
        root.setLength(rootLength);
        StringBuffer alter = root
          .append(' ')
          .append(column.getQuotedName(dialect))
          .append(' ')
          .append(column.getSqlType(dialect, mapping));

        String defaultValue = column.getDefaultValue();
        if (defaultValue != null) {
          alter.append(" default ").append(defaultValue);
        }

        if (column.isNullable()) {
          alter.append(dialect.getNullColumnString());
        }
        else {
          alter.append(" not null");
        }

        boolean useUniqueConstraint = column.isUnique()
          && dialect.supportsUnique()
          && (!column.isNullable() || dialect.supportsNotNullUnique());
        if (useUniqueConstraint) {
          alter.append(" unique");
        }

        if (column.hasCheckConstraint() && dialect.supportsColumnCheck()) {
          alter.append(" check(").append(column.getCheckConstraint()).append(")");
        }

        String columnComment = column.getComment();
        if (columnComment != null) {
          alter.append(dialect.getColumnComment(columnComment));
        }

        results.add(alter.toString());
      }
    }
    return results.iterator();
  }

  public void createTable(String tableName) {
    Table table = findTableMapping(tableName);
    String sql = sqlCreateString(table);
    try {
      execute(new String[] { sql });
    }
    catch (SQLException e) {
      exceptions.add(e);
      JDBCExceptionReporter.logExceptions(e, "failed to create table");
    }
  }

  private String sqlCreateString(Table table) {
    return table
      .sqlCreateString(getDialect(), configuration.buildMapping(), getDefaultCatalog(), getDefaultSchema());
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      String action = args[0];

      if ("create".equalsIgnoreCase(action)) {
        getJbpmSchema(args, 1).createSchema();
      }
      else if ("drop".equalsIgnoreCase(action)) {
        getJbpmSchema(args, 1).dropSchema();
      }
      else if ("clean".equalsIgnoreCase(action)) {
        getJbpmSchema(args, 1).cleanSchema();
      }
      else if ("update".equalsIgnoreCase(action)) {
        getJbpmSchema(args, 1).updateSchema();
      }
      else if ("scripts".equalsIgnoreCase(action) && args.length > 2) {
        getJbpmSchema(args, 3).saveSqlScripts(args[1], args[2]);
      }
      else
        syntax();
    }
    else
      syntax();
  }

  private static void syntax() {
    System.err.println("Syntax:");
    System.err.println("JbpmSchema create [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema drop [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema clean [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err.println("JbpmSchema update [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.err
      .println("JbpmSchema scripts <dir> <prefix> [<hibernate.cfg.xml> [<hibernate.properties>]]");
    System.exit(1);
  }

  private static JbpmSchema getJbpmSchema(String[] args, int index) {
    Configuration configuration = new Configuration();

    if (index < args.length) {
      // read configuration xml file
      String hibernateCfgXml = args[index];
      configuration.configure(new File(hibernateCfgXml));

      // read extra properties
      if (index + 1 < args.length) {
        String hibernateProperties = args[index + 1];
        try {
          InputStream fileSource = new FileInputStream(hibernateProperties);
          Properties properties = new Properties();
          properties.load(fileSource);
          fileSource.close();
          configuration.addProperties(properties);
        }
        catch (IOException e) {
          throw new JbpmException("failed to load hibernate properties", e);
        }
      }
    }
    else {
      // read configuration from default resource
      configuration.configure();
    }

    return new JbpmSchema(configuration);
  }

  private Connection createConnection() throws SQLException {
    try {
      connectionProvider = ConnectionProviderFactory.newConnectionProvider(configuration
        .getProperties());
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

  private void closeConnection(Connection connection) {
    if (connectionProvider != null) {
      try {
        if (connection != null) {
          JDBCExceptionReporter.logAndClearWarnings(connection);
          connectionProvider.closeConnection(connection);
        }
      }
      catch (SQLException e) {
        exceptions.add(e);
        JDBCExceptionReporter.logExceptions(e);
      }
      finally {
        connectionProvider.close();
        connectionProvider = null;
      }
    }
  }

}
