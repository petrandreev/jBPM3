package org.jbpm.db.compatibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.util.ReflectHelper;

import org.jbpm.db.JbpmSchema;

/**
 * This is a modified version of the hibernate tools schema update. The modification is to
 * support saving of the update script to a file.
 * 
 * @author Christoph Sturm
 * @author Koen Aers
 * @deprecated superseded by {@link JbpmSchema}
 */
public class JbpmSchemaUpdate {

  private static final Log log = LogFactory.getLog(JbpmSchemaUpdate.class);
  private ConnectionProvider connectionProvider;
  private Configuration configuration;
  private Dialect dialect;
  private List exceptions;

  public JbpmSchemaUpdate(Configuration cfg) throws HibernateException {
    this(cfg, cfg.getProperties());
  }

  public JbpmSchemaUpdate(Configuration cfg, Properties connectionProperties)
    throws HibernateException {
    this.configuration = cfg;
    dialect = Dialect.getDialect(connectionProperties);
    Properties props = new Properties();
    props.putAll(dialect.getDefaultProperties());
    props.putAll(connectionProperties);
    connectionProvider = ConnectionProviderFactory.newConnectionProvider(props);
    exceptions = new ArrayList();
  }

  public JbpmSchemaUpdate(Configuration cfg, Settings settings) throws HibernateException {
    this.configuration = cfg;
    dialect = settings.getDialect();
    connectionProvider = settings.getConnectionProvider();
    exceptions = new ArrayList();
  }

  public static void main(String[] args) {
    try {
      Configuration cfg = new Configuration();

      boolean script = true;
      // If true then execute db updates, otherwise just generate and display updates
      boolean doUpdate = true;
      String propFile = null;
      File out = null;

      for (int i = 0; i < args.length; i++) {
        if (args[i].startsWith("--")) {
          if (args[i].equals("--quiet")) {
            script = false;
          }
          else if (args[i].startsWith("--properties=")) {
            propFile = args[i].substring(13);
          }
          else if (args[i].startsWith("--config=")) {
            cfg.configure(args[i].substring(9));
          }
          else if (args[i].startsWith("--text")) {
            doUpdate = false;
          }
          else if (args[i].startsWith("--naming=")) {
            cfg.setNamingStrategy((NamingStrategy) ReflectHelper.classForName(args[i].substring(9))
              .newInstance());
          }
          else if (args[i].startsWith("--output=")) {
            out = new File(args[i].substring(9));
          }
        }
        else {
          cfg.addFile(args[i]);
        }
      }

      if (propFile != null) {
        Properties props = new Properties();
        props.putAll(cfg.getProperties());
        props.load(new FileInputStream(propFile));
        cfg.setProperties(props);
      }

      new JbpmSchemaUpdate(cfg).execute(script, doUpdate, out);
    }
    catch (Exception e) {
      log.error("Error running schema update", e);
    }
  }

  /**
   * Execute the schema updates
   * 
   * @param script print all DDL to the console
   */
  public void execute(boolean script, boolean doUpdate, File out) {
    log.info("Running hbm2ddl schema update");

    Connection connection = null;
    Statement stmt = null;
    boolean autoCommitWasEnabled = true;
    FileWriter writer = null;

    if (script && out != null) {
      try {
        log.info("opening file for writing: " + out.getAbsolutePath());
        writer = new FileWriter(out);
      }
      catch (IOException e) {
        log.error("could not open file for writing", e);
      }
    }

    exceptions.clear();
    try {
      DatabaseMetadata meta;
      try {
        log.info("fetching database metadata");
        connection = connectionProvider.getConnection();
        if (!connection.getAutoCommit()) {
          connection.commit();
          connection.setAutoCommit(true);
          autoCommitWasEnabled = false;
        }
        meta = new DatabaseMetadata(connection, dialect);
        stmt = connection.createStatement();
      }
      catch (SQLException sqle) {
        exceptions.add(sqle);
        log.error("could not get database metadata", sqle);
        throw sqle;
      }

      log.info("updating schema");
      boolean debug = log.isDebugEnabled();
      String[] createSQL = configuration.generateSchemaUpdateScript(dialect, meta);
      for (int j = 0; j < createSQL.length; j++) {
        final String sql = createSQL[j];
        try {
          if (script) {
            System.out.println(sql);
            if (writer != null) writer.write(sql + ";\n");
          }
          if (doUpdate) {
            if (debug) log.debug(sql);
            stmt.executeUpdate(sql);
          }
        }
        catch (SQLException e) {
          exceptions.add(e);
          log.error("Unsuccessful: " + sql);
          log.error(e.getMessage());
        }
      }

      if (writer != null) writer.close();
      log.info("schema update complete");
    }
    catch (Exception e) {
      exceptions.add(e);
      log.error("could not complete schema update", e);
    }
    finally {
      try {
        if (stmt != null) stmt.close();
        if (!autoCommitWasEnabled) connection.setAutoCommit(false);
        if (connection != null) connection.close();
        if (connectionProvider != null) connectionProvider.close();
      }
      catch (Exception e) {
        exceptions.add(e);
        log.error("Error closing connection", e);
      }
    }
  }

  /**
   * Returns a List of all Exceptions which occured during the export.
   * 
   * @return A List containig the Exceptions occured during the export
   */
  public List getExceptions() {
    return exceptions;
  }
}
