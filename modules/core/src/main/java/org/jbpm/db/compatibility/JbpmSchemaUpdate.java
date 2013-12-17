package org.jbpm.db.compatibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.util.JDBCExceptionReporter;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;

/**
 * This is a modified version of the hibernate tools schema update. The modification is to support
 * saving of the update script to a file.
 * 
 * @author Christoph Sturm
 * @author Koen Aers
 */
public class JbpmSchemaUpdate {

  private Configuration configuration;
  private Settings settings;

  private File outputFile;
  private String delimiter;

  private final List<Exception> exceptions = new ArrayList<Exception>();

  private static final Log log = LogFactory.getLog(JbpmSchemaUpdate.class);

  public JbpmSchemaUpdate(Configuration configuration) {
    this(configuration, configuration.getProperties());
  }

  public JbpmSchemaUpdate(Configuration configuration, Properties properties) {
    Properties copy = (Properties) properties.clone();
    PropertiesHelper.resolvePlaceHolders(copy);
    this.configuration = configuration;
    this.settings = configuration.buildSettings(copy);
  }

  public JbpmSchemaUpdate(Configuration configuration, Settings settings) {
    this.configuration = configuration;
    this.settings = settings;
  }

  /**
   * Set an output file. The generated script will be written to this file.
   */
  public void setOutputFile(File outputFile) {
    this.outputFile = outputFile;
  }

  public void setDelimiter(String delimiter) {
    this.delimiter = delimiter;
  }

  public static void main(String[] args) {
    try {
      Configuration cfg = new Configuration();

      boolean script = true;
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
            Class<?> clazz = ReflectHelper.classForName(args[i].substring(9));
            cfg.setNamingStrategy(clazz.asSubclass(NamingStrategy.class).newInstance());
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
        InputStream inStream = new FileInputStream(propFile);
        try {
          Properties props = new Properties();
          props.load(inStream);
          cfg.addProperties(props);
        }
        finally {
          inStream.close();
        }
      }

      new JbpmSchemaUpdate(cfg).execute(script, doUpdate, out);
    }
    catch (Exception e) {
      log.error("Error running schema update", e);
    }
  }

  /**
   * Executes the schema update tool.
   * 
   * @param printScript print DDL statements to the console
   * @param doUpdate post DDL statements to the database
   * @param outputFile write DDL statements to this file, can be <code>null</code>
   */
  public void execute(boolean printScript, boolean doUpdate, File outputFile) {
    setOutputFile(outputFile);
    execute(printScript, doUpdate);
  }

  /**
   * Executes the schema update tool.
   * 
   * @param printScript print DDL statements to the console
   * @param doUpdate post DDL statements to the database
   */
  public void execute(boolean printScript, boolean doUpdate) {
    log.info("running schema update");
    exceptions.clear();
    try {
      String[] script = generateScript(doUpdate);
      if (printScript) {
        printScript(script);
      }
      if (outputFile != null) {
        writeFile(script);
      }
    }
    catch (SQLException e) {
      exceptions.add(e);
      log.error("database connection failed", e);
    }
    catch (IOException e) {
      exceptions.add(e);
      log.error("could not write file: " + outputFile, e);
    }
  }

  private String[] generateScript(boolean doUpdate) throws SQLException {
    ConnectionProvider connectionProvider = settings.getConnectionProvider();
    try {
      log.info("acquiring a connection");
      Connection connection = connectionProvider.getConnection();
      try {
        if (!connection.getAutoCommit()) {
          connection.commit();
          connection.setAutoCommit(true);
        }
        log.info("fetching database metadata");
        Dialect dialect = settings.getDialect();
        DatabaseMetadata meta = new DatabaseMetadata(connection, dialect);

        log.info("generating schema update script");
        String[] script = configuration.generateSchemaUpdateScript(dialect, meta);

        if (doUpdate) {
          log.info("updating schema");
          doUpdate(script, connection);
          log.info("schema update complete");
        }

        return script;
      }
      finally {
        JDBCExceptionReporter.logAndClearWarnings(connection);
        connectionProvider.closeConnection(connection);
      }
    }
    finally {
      connectionProvider.close();
    }
  }

  private void doUpdate(String[] script, Connection connection) throws SQLException {
    Statement statement = connection.createStatement();
    try {
      for (int i = 0; i < script.length; i++) {
        String sql = script[i];
        log.debug(sql);
        try {
          statement.execute(sql);
          SQLWarning warning = statement.getWarnings();
          if (warning != null) {
            JDBCExceptionReporter.logWarnings(warning);
          }
        }
        catch (SQLException e) {
          exceptions.add(e);
          log.error("unsuccessful: " + sql);
          log.error(e.getMessage());
        }
      }
    }
    finally {
      statement.close();
    }
  }

  private void printScript(String[] script) {
    for (int i = 0; i < script.length; i++) {
      System.out.println(script[i]);
    }
  }

  private void writeFile(String[] script) throws IOException {
    Writer writer = new FileWriter(outputFile);
    String lineSeparator = System.getProperty("line.separator");
    try {
      for (int i = 0; i < script.length; i++) {
        writer.write(script[i]);
        if (delimiter != null)
          writer.write(delimiter);
        writer.write(lineSeparator);
      }
    }
    finally {
      writer.close();
    }
  }

  /**
   * Returns a List of all Exceptions that occurred during the export.
   * 
   * @return the exceptions that occurred during the export
   */
  public List<Exception> getExceptions() {
    return exceptions;
  }
}
