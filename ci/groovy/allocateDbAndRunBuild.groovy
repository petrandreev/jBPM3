// hudson.model.Result for setting build result
import hudson.model.*
// JDBCLoader class (from config_repository)
import hudson.util.*
// Builder class to get maven info
import hudson.tasks.*
// Simple date format
import java.text.*
// Template for writing maven settings file
import groovy.text.SimpleTemplateEngine
// java.nio.* classes for dealing with NFS
import java.nio.channels.*

/**
 * For documentation over this script, scroll past the following two classes. 
 */ 

class ScriptException extends Exception { 
  ScriptException(String message) { 
    super(message)
  }
}

/**
 * This is a helper class to fetch JDBC drivers from the shared JBoss QE 
 * repository.
 *
 * Usage: In constructor, provide product, its version, the requested database
 * and JDBC version. Then use method load() to get the driver(s) - the one
 * attribute of the method is the target directory in which to store the files. 
 *
 * If the "rename" attribute has been set, the downloaded driver file will be 
 * named "jdbc_driver.jar".  If there is more than one driver file necessary for
 * that database, the others will be named jdbc_driver_${X}.jar, where ${X} is 
 * a number larger than 1. Otherwise, the file will be named as it was 
 * originally named. 
 *
 * The class has very descriptive exceptions, so when something goes wrong,
 * it should be immediately clear, what actually happened.
 *
 * @author lpetrovi
 */

class JDBCLoader {

	private URL = new URL("http://www.qa.jboss.com/jdbc-drivers-products/");
	private product;
	private productVersion;
	private database;
	private JDBCVersion;
	
	def JDBCLoader(String product, String productVersion, String database, Integer JDBCVersion) {
		this.product = product;
		this.productVersion = productVersion;
		this.database = database;
		this.JDBCVersion = JDBCVersion;
	}
	
	def verify() throws IllegalStateException {
		// verify product URL
		def productURL = new URL(this.URL, this.product + "/");
		def connection = null;
		try {
			connection = productURL.openStream();
		} catch (Exception ex) {
			throw new IllegalStateException("Invalid product name: " + this.product, ex); 
		} finally {
			connection.close();
		}
		// verify product version
		def productVersionURL = new URL(productURL, this.productVersion + "/");
		try {
			connection = productVersionURL.openStream();
		} catch (Exception ex) {
			throw new IllegalStateException("Invalid " + this.product +" version: " + this.productVersion, ex); 
		} finally {
			connection.close();
		}
		// verify database version
		def databaseURL = new URL(productVersionURL, this.database + "/");
		try {
			connection = databaseURL.openStream();
		} catch (Exception ex) {
			throw new IllegalStateException("Invalid " + this.product + " " + this.productVersion + " database: " + this.database, ex); 
		} finally {
			connection.close();
		}
		if (this.JDBCVersion != 3 && this.JDBCVersion != 4) {
			throw new IllegalStateException("Invalid JDBC version: " + this.JDBCVersion);
		}
	}
	
	def private downloadFile(URL f, File target, String name, boolean ignoreIfExists) throws IOException {
		def targetFile = new File(target, name);
		if (targetFile.exists() && ignoreIfExists) {
			System.out.println("Skipping " + f + " to " + targetFile.getAbsolutePath() + " because it already exists.");
		} else {
			System.out.println("Downloading " + f + " to " + targetFile.getAbsolutePath() + ".");
			def ins = new BufferedInputStream(f.openStream());
			def fos = new FileOutputStream(targetFile);
			def outs = new BufferedOutputStream(fos);
			outs << ins
		} 
		return targetFile;
	}
	
	def load(File target) throws IllegalStateException, IllegalArgumentException {
		return load(target, false, true);
	}
	
	def load(File target, boolean rename, boolean ignoreIfExists) throws IllegalStateException, IllegalArgumentException {
		verify();
		if (!target.isDirectory() || !target.canWrite()) {
			throw new IllegalArgumentException("Supplied file is not a writable directory: " + target.getAbsolutePath()); 
		}
		// get a list of files to fetch
		def rootURL = new URL(this.URL, this.product + "/" + this.productVersion + "/" + this.database + "/jdbc" + this.JDBCVersion + "/");
		def filesToDownload = new LinkedList<URL>();
		try {
			def metaInfURL = new URL(rootURL, "meta-inf.txt");
			def r = new BufferedReader(new InputStreamReader(metaInfURL.openStream()));
			String line = "";
			while ((line = r.readLine()) != null) {
				filesToDownload.add(new URL(rootURL, line));
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed reading ${metaInfURL}!", ex);
		}
		def i = 1;
		Set<File> files = new HashSet<File>();
		for (URL u: filesToDownload) {
			def name = "";
			if (rename) {
				name = "jdbc_driver.jar";
				if (i > 1) {
					name = "jdbc_driver_" + i + ".jar";
				}
				i++;
			} else {
				name = new File(u.getFile()).getName();
			}
			try {
				files.add(downloadFile(u, target, name, ignoreIfExists));
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException("Failed downloading " + u, ex);
			} 
		}
		return files;
	}
	
  def void listLabels() { 
    def connection = null;
		def indexURL = new URL(this.URL, this.product + "/" + this.productVersion + "/" )
    try {
      def r = new BufferedReader(new InputStreamReader(indexURL.openStream()));
      String line = "";
      while ((line = r.readLine()) != null) {
        (line =~ /a href="[a-z][^"]+"/).each { 
          match -> System.out.println " - " + match.replaceAll("\"", "").replace("a href=", "").replaceAll( "/","")
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Failed reading ${indexURL}!", ex);
    }
  }

}

/**
 * - DBAllocation class
 *   : this a modified copy of the following file, imported for self-sufficiency: 
 *      https://svn.devel.redhat.com/repos/jboss-qa/hudson/config_repository/scripts/groovy.classes/hudson/util/DBAllocation.groovy
 *   : instead of using AntBuilder, we use the "normal" java download methods 
 *      (URL.openStream(), *Stream objects)
 * 
 * Many thanks to mvecera!
 */

class DBAllocation {

  private retries = 30
  private UUID = ""
  private out

  private tmpFile = File.createTempFile("dbAlloc-", ".out")

  def dbConfigFile
  private dbInstallPath = tmpFile.parent

  private static String baseUrl = "http://dballocator.mw.lab.eng.bos.redhat.com:8080/Allocator/AllocatorServlet?"

  def DBAllocation(dbInstallPath, out, outPropsFileName = "allocated.db.properties") {
    this.out = out
    this.dbInstallPath = dbInstallPath
    dbConfigFile = new File(dbInstallPath, outPropsFileName)
  }
  
  def getProperties() {
    def props = new Properties();
    props.load(new FileInputStream(dbConfigFile));
    return props;
  }

  def allocate(label, requestee, expiry) {
    if (dbConfigFile.exists()) {
      dbConfigFile.delete()
    }

    def i = 0
    int sleepSeconds = 60
    while (!(dbConfigFile.exists() && dbConfigFile.length() > 0)) {
      if (i >= retries) {
        throw new ScriptException('Database unavailable')
      }
      if (i > 0) {
        out.println "Waiting " + sleepSeconds + " seconds before retrying DB allocation."
        Thread.sleep(sleepSeconds * 1000)
      }
      out.println 'Allocating DB [requestee: ' + requestee + ']'
      def allocatorUrl = baseUrl + "operation=alloc&label=$label&requestee=$requestee&expiry=$expiry"
      download( "Allocate", allocatorUrl, dbConfigFile )
      i++
    }

    def dbProps = getProperties();
    this.UUID = dbProps['uuid']

    return this.UUID
  }
  
  def release() {
    release(this.UUID)
  }

  def release(UUID) {
    out.println 'Deallocating DB [' + this.UUID + ']'
    def allocatorUrl = baseUrl + "operation=dealloc&uuid=$UUID"
    download( "Release", allocatorUrl, tmpFile )
  }

  def clean() {
    clean(this.UUID);
  }

  def clean(UUID) {
    out.println 'Cleaning DB [' + this.UUID + ']'
    def allocatorUrl = baseUrl + "operation=erase&uuid=$UUID"
    download( "Clean", allocatorUrl, tmpFile )
  }

  def reallocate(newExpiry) {
    reallocate(this.UUID, newExpiry)
  }

  def reallocate(UUID, newExpiry) {
    out.println 'Reallocating DB [' + this.UUID + ']'
    def allocatorUrl = baseUrl + "operation=realloc&uuid=$UUID&expiry=$newExpiry"
    download( "Reallocate", allocatorUrl, tmpFile )
  }

  def download(action, address, toFile) { 
    def file = new FileOutputStream(toFile);
    def outStream = new BufferedOutputStream(file)
    action += ": "
    outStream.write(action.toString().getBytes())
    try { 
      outStream << new URL(address).openStream()
    }
    finally { 
      if( outStream != null ) { 
        try { 
          outStream.close()
        }
        catch( Throwable t ) { 
          // do nothing.. 
        }
      }
    }
  }

  def clear() {
    if( tmpFile != null && tmpFile.exists() ) { 
      tmpFile.delete()
    }
    if( dbConfigFile != null && dbConfigFile.exists() ) { 
      dbConfigFile.delete()
    }
  }
}

/**
 * This script requires the following in order to run:. 
 * 
 * String variables set by the build:
 * - database:     The type of database the tests should be run against 
 *                 (This is usually a "user-defined axis" in a matrix job, see
 *                   https://wiki.jenkins-ci.org/display/JENKINS/Aboutncysa)
 * - DB_EXPIRY:    How long the database should be reserved for. This
 *                 needs to be long enough to run all the tests, obviously.
 * 
 * This script does the following: 
 * - retrieve variables that it needs.  
 * - allocate the database (via DBAllocator)
 * - download the correct jdbc driver
 * - write the database settings to a maven settings file for maven build to use
 * - free the database 
 * ! if any part of the script fails, the database is freed and the settings file is deleted
 *
 * This script is organized as follows: 
 * "VARIABLES": retrieving and setting variables
                at the end, all critical variables are printed 
 * "DATABASE VAR": database dependent variables configuration
                   (for example, hibernate dialects, XA datasource classes, etc.)
 * "SETTINGS XML": text/template definition for the settings.xml file that the 
 *                  following maven build will use
 * "PREPARE": Where everything is prepared
 *         - database allocation
 *         - JDBC driver jar download
 *         - create and write the settings.xml file
 * "MAVEN BUILD" : where the maven build is fired off (using a ProcessBuilder).
 * "CLEAN UP" : exception/error handling and database de-allocation
 */

boolean debug = false
Properties localDbProps = new Properties()
localDbProps.setProperty("uuid", "mrietvel-local-postgresql-db")
localDbProps.setProperty("db.primary_label", "postgresql84")
localDbProps.setProperty("dballoc.db_type", "standard")

localDbProps.setProperty("db.minpoolsize", "15")
localDbProps.setProperty("db.maxpoolsize", "50")
localDbProps.setProperty("db.jdbc_class", "org.postgresql.Driver")

localDbProps.setProperty("db.name", "jbpm5")
localDbProps.setProperty("db.hostname", "localhost" )
localDbProps.setProperty("db.port", "5432")
localDbProps.setProperty("db.username", "jbpm5")
localDbProps.setProperty("db.password", "jbpm5")
localDbProps.setProperty("db.jdbc_url", "jdbc:postgresql://localhost:5432/jbpm5")

localDbProps.setProperty("hibernate.connection.password", "jbpm5")
localDbProps.setProperty("hibernate.connection.username", "jbpm5")
localDbProps.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
localDbProps.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
localDbProps.setProperty("hibernate.connection.schema", "jbpm5")

/**
 * VARIABLES
 */

def config = new HashMap()
def bindings = getBinding()
config.putAll(bindings.getVariables())

def out = config['out']

// System environmental variables

def sysEnv = System.getenv()
config.putAll(sysEnv)

if( config['DEBUG'] != null || debug ) { 
  debug = true;
  out.println( "*** DEBUG MODE ***" )
}

/**
 * Check setup
 */

out.println( "Checking environmental variables." );

def acceptedDatabases = new HashMap();
acceptedDatabases.put("hsqldb", true)
acceptedDatabases.put("db2-91", true)
acceptedDatabases.put("db2-97", true)
acceptedDatabases.put("mssql2005", true)
acceptedDatabases.put("mssql2008R1", true)
acceptedDatabases.put("mssql2008R2", true)
acceptedDatabases.put("mysql50", true)
acceptedDatabases.put("mysql51", true)
acceptedDatabases.put("mysql55", true)
acceptedDatabases.put("oracle10g", true)
acceptedDatabases.put("oracle11gR1", true)
acceptedDatabases.put("oracle11gR2", true)
acceptedDatabases.put("postgresql82", true)
acceptedDatabases.put("postgresql83", true)
acceptedDatabases.put("postgresql84", true)
acceptedDatabases.put("postgresql91", true)
acceptedDatabases.put("sybase150", true)
acceptedDatabases.put("sybase155", true)
acceptedDatabases.put("sybase157", true)

if( acceptedDatabases.get(config['database']) == null ) { 
  String message = "Database type [" + config['database'] + "] is unknown!";
  throw new ScriptException(message)
}

def envVarNames = [ 
  "COMMON_TOOLS", 
  "JOB_NAME", 
  "BUILD_NUMBER", 
  "DB_EXPIRY", 
  "WORKSPACE" 
]

envVarNames.each() { 
  if( config[it] == null ) { 
    String message = "Please set the ${it} environmental variable!";
    throw new ScriptException(message)
  }
};

/**
 * VARIABLES
 */

// Set maven variables 
def mvn2Path = config['COMMON_TOOLS'] + File.separator + "maven-2.2.1"

config.remove('M3_HOME')
config.put('MAVEN_HOME', mvn2Path )
config.put('M2_HOME', mvn2Path )

// Get the relative target dir

// set/save variables
def jobName = config['JOB_NAME'] 
def buildTag = '' 
(jobName =~ /^[\.A-Za-z0-9\-]+/).each {
    match -> buildTag = match
}
buildTag += "-" + config['BUILD_NUMBER'] 

def database = config['database']
def dbExpiry = config['DB_EXPIRY'] 
def workspace = config['WORKSPACE']

def profileName = ''
(database =~ /^[a-z]+/).each { 
  match -> profileName = match
}
if( database =~ /^db2/ ) { 
  profileName = "db2";
}

if( profileName.length() < 1 ) { 
  String message = "UNABLE TO DETERMINE DATABASE TYPE: " + database
  out.println message
  throw new ScriptException(message)
}

def settingsXmlFileName = "settings-db.xml"
if( config['settingsXmlFileName'] != null ) { 
  settingsXmlFileName = config['settingsXmlFileName'] 
}

// Log variables

out.println "-" * 80

out.println "build name:         " + buildTag
out.println "-"
out.println "profile name:       " + profileName
out.println "settings xml file:  " + settingsXmlFileName
out.println "workspace:          " + workspace
out.println "-"
out.println "database:           " + database
out.println "profile:            " + profileName
out.println "db expiry:          " + dbExpiry
out.println "-"
out.println "maven home:         " + config.get('MAVEN_HOME')

out.println "-" * 80

/**
 * DATABASE VAR: database dependent info
 */

// Hibernate dialects
def hibernateDialect = [
"h2":"org.hibernate.dialect.H2Dialect",
"hsqldb":"org.hibernate.dialect.HSQLDialect",
"db2":"org.hibernate.dialect.DB2Dialect",
"postgresql":"org.hibernate.dialect.PostgreSQLDialect",
"oracle10g":"org.hibernate.dialect.Oracle10gDialect",
"oracle11g":"org.hibernate.dialect.Oracle10gDialect",
"oracle11gR2":"org.hibernate.dialect.Oracle10gDialect",
"mssql":"org.hibernate.dialect.SQLServerDialect",
"mysql":"org.hibernate.dialect.MySQL5InnoDBDialect",
"sybase":"org.hibernate.dialect.SybaseDialect",
// other.. 
"db2as/400":"org.hibernate.dialect.DB2400Dialect",
"db2os390":"org.hibernate.dialect.DB2390Dialect",
"informix":"org.hibernate.dialect.InformixDialect",
"HypersonicSQL":"org.hibernate.dialect.HSQLDialect",
"ingres":"org.hibernate.dialect.IngresDialect",
"progress":"org.hibernate.dialect.ProgressDialect",
"interbase":"org.hibernate.dialect.InterbaseDialect",
"pointbase":"org.hibernate.dialect.PointbaseDialect",
"frontbase":"org.hibernate.dialect.FrontbaseDialect",
"firebird":"org.hibernate.dialect.FirebirdDialect",
"sapdb":"org.hibernate.dialect.SAPDBDialect"
]

def jdbcDriverDependency = [
"hsqldb":       'hsqldb:hsqldb:1.8.0.7:',

"db2-91":       'com.ibm:db2jcc:3.8.47:',
"db2-97":       'com.ibm:db2jcc4:3.58.82:',

"mssql2005":    'com.microsoft.sqlserver:msjdbc:4.0.unknown:',
"mssql2008R1":  'com.microsoft.sqlserver:msjdbc:4.0.unknown:',
"mssql2008R2":  'com.microsoft.sqlserver:msjdbc:4.0.unknown:',
// "mssql2005":    'net.sourceforge.jtds:jtds:1.2.4:',
// "mssql2008R1":  'net.sourceforge.jtds:jtds:1.2.4:',
// "mssql2008R2":  'net.sourceforge.jtds:jtds:1.2.4:',

"mysql50":      'mysql:mysql-connector-java:5.0.8:',
"mysql51":      'mysql:mysql-connector-java:5.1.19:',
"mysql55":      'mysql:mysql-connector-java:5.1.19:',

"oracle10g":    'com.oracle:ojdbc14:10.2.0.4:',
"oracle11gR1":  'com.oracle:ojdbc6:11.1.0.7.0:',
"oracle11gR2":  'com.oracle:ojdbc6:11.2.0.1.0:',

"postgresql82": 'postgresql:postgresql:8.2-507.jdbc4:',
"postgresql83": 'postgresql:postgresql:8.3-606.jdbc4:',
"postgresql84": 'postgresql:postgresql:8.4-702.jdbc4:',

"sybase150":    'com.sybase:jconnect:6.0.5_26564:',
"sybase155":    'com.sybase:jconnect:7.0_26502-4:',
"sybase157":    'com.sybase:jconnect:7.0_26666-4:'
// "sybase150":    'net.sourceforge.jtds:jtds:1.2.4:',
// "sybase155":    'net.sourceforge.jtds:jtds:1.2.4:',
// "sybase157":    'net.sourceforge.jtds:jtds:1.2.4:'
]

def dependencyString = jdbcDriverDependency.get(database);
def dependency = new ArrayList<String>(3);

(dependencyString =~ /([^:]+):/).each { match ->
  dependency.add(match[1]);
}

/**
 * SETTINGS XML
 */ 

def settingsXmlFilePath = workspace + File.separator + settingsXmlFileName

def settingsXmlHeader = 
  '<?xml version="1.0" encoding="UTF-8"?>\n\n' +
  '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"\n' + 
  '  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n' + 
  '  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\n' + 
  '  <profiles>\n' + 
  '    <profile>\n' + 
  '      <id>' + profileName + '</id>\n\n'

def repositoryXml = 
  '\n' +
  '      <repositories>\n' + 
  '        <repository>\n' + 
  '          <id>qa-jdbc-drivers</id>\n' + 
  '          <url>http://www.qa.jboss.com/jdbc-drivers/maven2</url>\n' + 
  '        </repository>\n' + 
  '      </repositories>\n'
  
def propertiesXmlTemplate =
  '      <properties>\n' +
  '        <jdbc.' + profileName + '.server>${dbServer}</jdbc.' + profileName + '.server>\n' + 
  '        <jdbc.' + profileName + '.port>${dbPort}</jdbc.' + profileName + '.port>\n' + 
  '        <jdbc.' + profileName + '.database>${dbName}</jdbc.' + profileName + '.database>\n' + 
  '        <jdbc.' + profileName + '.url>${jdbcUrl}</jdbc.' + profileName + '.url>\n' + 
  '        <jdbc.' + profileName + '.username>${dbUsername}</jdbc.' + profileName + '.username>\n' + 
  '        <jdbc.' + profileName + '.password>${dbPassword}</jdbc.' + profileName + '.password>\n' + 
  '        <jdbc.' + profileName + '.driver>${jdbcDriver}</jdbc.' + profileName + '.driver>\n' + 
  '\n' + 
  '        <jdbc.' + profileName + '.dialect>${hibernateDialect}</jdbc.' + profileName + '.dialect>\n' + 
  '        <jdbc.' + profileName + '.schema>${dbSchema}</jdbc.' + profileName + '.schema>\n' + 
  '\n' + 
  '        <jdbc.driver.group>${driverGroupId}</jdbc.driver.group>\n' + 
  '        <jdbc.driver.artifact>${driverArtifactId}</jdbc.driver.artifact>\n' + 
  '        <jdbc.driver.version>${driverVersion}</jdbc.driver.version>\n' + 
  '\n' + 
  '      </properties>\n'

def settingsXmlFooter =
  '\n' + 
  '    </profile>\n' + 
  '  </profiles>\n' + 
  '\n' + 
  '  <activeProfiles>\n' + 
  '    <activeProfile>' + profileName + '</activeProfile>\n' + 
  '  </activeProfiles>\n' + 
  '</settings>\n'

def mustDownloadDriverJar = [
"db2-91":       "SOA:5.0:4:",
"db2-97":       "SOA:5.3.0:4:",

"mssql2005":    "SOA:5.2.0:4:",
"mssql2008R1":  "SOA:5.3.0:4:",
"mssql2008R2":  "SOA:5.3.0:4:",

// "oracle10g":    "SOA:5.2.0:4:"
// "oracle11gR1":  "SOA:5.3.0:4:"
// "oracle11gR2":  "SOA:5.3.0:4:"

"sybase150":    "SOA:5.2.0:4:",
"sybase155":    "SOA:5.3.0:4:",
"sybase157":    "EAP:6.0.0:4:",
]

/**
 * PREPARE: allocate DB, download driver, save settings
 */

// logging info
def dbInfo = database + ", " + buildTag + ", " + dbExpiry + " minutes."

def sdf = new SimpleDateFormat( "HH:mm:ss [z]" )

int dbAllocated = 0
int jdbcDriverDownloaded = 0

def allocation = null
File settingsXml = null
Process mvnProc = null

try {
  /**
   * allocate the database
   */
  def dbProps = null
  if( debug ) { 
    out.println "Using local database: " + dbInfo
    dbProps = localDbProps
  } else if( profileName.equals("hsqldb") ) { 
    dbProps = new Properties(); 
    dbProps.setProperty('db.jdbc_class', "org.hsqldb.jdbcDriver" );
    dbProps.setProperty('db.name', "jbpm3" );
    dbProps.setProperty('db.port', "" );
    dbProps.setProperty('db.hostname', "" );
    dbProps.setProperty('db.username', "sa" );
    dbProps.setProperty('db.password', "" );
    dbProps.setProperty('db.jdbc_url', "jdbc:hsqldb:mem:jbpm3" );
  } else { 
    /**
     * allocate the database
     */
    allocation = new DBAllocation(workspace, out)
     
    def allocationUUID = allocation.allocate(database, buildTag, dbExpiry)
    dbAllocated = 1
    try { 
      allocation.clean()
    }
    catch( IOException ioe ) { 
      // If it can't be cleaned, it don't matter
      String message = ioe.message.replaceAll( "for URL.*", "" )
      out.println( "Unable to clean database: " + message )
    }

    dbAllocated = 2

    GregorianCalendar expiryTime = new GregorianCalendar()
    expiryTime.add(Calendar.MINUTE, Integer.parseInt(dbExpiry))
    out.print sdf.format(new Date()) + ": " 
    out.println "Allocated " + database + " will expire at " + sdf.format(expiryTime.getTime())
    out.println "-" * 80
  
    // print properties
    dbProps = allocation.getProperties()
  }

  /**
   * Extra setup
   */
  def dbSchema = dbProps.getProperty('hibernate.connection.schema');
  if( database =~ /^postgresql/ ) { 
    dbSchema = "public";
  } else if( database =~ /^oracle/ ) { 
    dbSchema = dbProps.getProperty('db.username');
  } else if( database =~ /^db2/ ) { 
    dbSchema = dbProps.getProperty('db.username');
  } else if( database =~ /^mssql/ ) { 
    dbSchema = "";
  }

  String processDir = workspace

  File workspaceDir = new File(processDir)

  /**
   * Download and install the jdbc driver jar
   */

  loaderInputString = mustDownloadDriverJar.get(database); 
  if( loaderInputString != null ) { 

    /** 
     * Download the jar
     */
    def loaderInput = new ArrayList<String>(3);
    (loaderInputString =~ /([^:]+):/).each { match ->
      loaderInput.add(match[1]);
    }
    if( loaderInput.size() < 3 ) { 
      String message = "JDBC Driver loader input has only " + loaderInput.size() + " arguments!";
      out.println message
      throw new ScriptException(message)
    }
    out.println "Retrieving JDBC driver [" + loaderInput.get(0) + ", " + loaderInput.get(1) + ", " + database + ", " + loaderInput.get(2) + "]"
  
    def destDir = workspace + File.separator + "lib"
    destDir = destDir instanceof File ? destDir : destDir as File
    if( ! destDir.exists() ) { 
      if( ! destDir.mkdirs() ) { 
        String message = "Unable to make directory [" + destDir.absolutePath + "]" 
        out.println( message )
        throw new ScriptException(message)
      }
    }
  
    def jdbcLoader = new JDBCLoader( loaderInput.get(0), loaderInput.get(1), database, Integer.parseInt(loaderInput.get(2)) )
    jdbcDriverDownloaded = 1
    def driverFiles = jdbcLoader.load(destDir)
    File driverFile = driverFiles.toArray()[0]
    jdbcDriverDownloaded = 2

    out.println "JDBC driver downloaded to " + driverFile.absolutePath

    /** 
     * Install the jar in the maven repo
     */
    String [] installCmd  = new String[7];

    int i = 0;
    installCmd[i++] = config.get('MAVEN_HOME').toString() + File.separator + "bin" + File.separator  + "mvn" 
    installCmd[i++] = "install:install-file"
    installCmd[i++] = "-Dfile=" + driverFile.absolutePath
    installCmd[i++] = "-DgroupId=" + dependency.get(0)
    installCmd[i++] = "-DartifactId=" + dependency.get(1)
    installCmd[i++] = "-Dversion=" + dependency.get(2)
    installCmd[i++] = "-Dpackaging=jar"

    out.println "Installing downloaded jdbc jar: " 
    out.println "[cmd]: " + installCmd
    out.println "-" * 80
  
    ProcessBuilder mvnProcBuilder = new ProcessBuilder(installCmd);
    mvnProcBuilder.redirectErrorStream(true);

    Map<String, String> env = mvnProcBuilder.environment()
    env.clear()

    config.keySet().each { key -> 
      try {  
        env.put(key, (String) config.get(key))
      }
      catch(ClassCastException cce) { 
        print "Unable to convert '" + key + "' to String: " 
        println cce.message
        // do nothing else..
      }
    } 

    mvnProcBuilder.directory(workspaceDir)

    mvnProc = mvnProcBuilder.start();

    // Just to make sure
    mvnProc.waitFor()
  
    if( mvnProc.exitValue() != 0 ) { 
      String message = "Maven install failed: ["
      installCmd.each { arg -> message += arg + " " } 
      message += "]"
      throw new ScriptException(message)
    }

  }

  /**
   * write the settings.xml file
   */
  def templateBinding = [
  "dbServer":         dbProps.getProperty('db.hostname'),
  "dbPort":           dbProps.getProperty('db.port'),
  "dbName":           dbProps.getProperty('db.name'),
  "jdbcUrl":          dbProps.getProperty('db.jdbc_url'),
  "dbUsername":       dbProps.getProperty('db.username'),
  "dbPassword":       dbProps.getProperty('db.password'),
  "jdbcDriver":       dbProps.getProperty('db.jdbc_class'),

  "hibernateDialect": hibernateDialect.get(profileName),  // ocram
  "dbSchema":         dbSchema,

  "driverGroupId":    dependency.get(0),
  "driverArtifactId": dependency.get(1),
  "driverVersion":    dependency.get(2)
  ]

  def templateEngine = new SimpleTemplateEngine()
  propertiesXml = templateEngine.createTemplate(propertiesXmlTemplate).make(templateBinding)

  settingsXml = new File(settingsXmlFilePath)
  settingsXml.write(settingsXmlHeader + repositoryXml + propertiesXml + settingsXmlFooter)
  out.println "Database settings written to " + settingsXml.absolutePath

  out.println "-" * 80
  out.println "hibernateDialect   " + templateBinding['hibernateDialect']
  out.println "jdbcDriver:        " + templateBinding['jdbcDriver']
  out.println "dbName:            " + templateBinding['dbName']
  out.println "dbPort:            " + templateBinding['dbPort']
  out.println "dbServer:          " + templateBinding['dbServer']
  out.println "dbUsername:        " + templateBinding['dbUsername']
  out.println "dbPassword:        " + templateBinding['dbPassword']
  out.println "jdbcUrl:           " + templateBinding['jdbcUrl']
  out.println "dbSchema:          " + templateBinding['dbSchema']
  out.println "-"
  out.println "driverGroupId:     " + templateBinding['driverGroupId']
  out.println "driverArtifactId:  " + templateBinding['driverArtifactId']
  out.println "driverVersion:     " + templateBinding['driverVersion']
  out.println "-" * 80

/**
 * MAVEN BUILD
 */

  String mvn2EnvOpts = "-Ddatabase=" + profileName + " -Xms16m -Xmx256m";
  config.put('MAVEN_OPTS', mvn2EnvOpts )

  String [] mvn2Cmd  = new String[10]
  int m = 0;
  mvn2Cmd[m++] = config.get('MAVEN_HOME').toString() + File.separator + "bin" + File.separator  + "mvn" 
  mvn2Cmd[m++] = "-s" + settingsXmlFileName 
  mvn2Cmd[m++] = "-P" + profileName
  mvn2Cmd[m++] = "-U"
  mvn2Cmd[m++] = "-Dsurefire.jvm.args=-Xms16m -Xmx256m"
  mvn2Cmd[m++] = "-Dmaven.test.failure.ignore=true"
  mvn2Cmd[m++] = "clean"
  mvn2Cmd[m++] = "test"
  mvn2Cmd[m++] = "-pl"
  mvn2Cmd[m++] = "core,enterprise,enterprise-jee5,examples,identity,simulation,tomcat"
  
  ProcessBuilder mvnProcBuilder = new ProcessBuilder(mvn2Cmd);
  mvnProcBuilder.redirectErrorStream(true);

  Map<String, String> env = mvnProcBuilder.environment()
  env.clear()

  config.keySet().each { key -> 
    try {  
      env.put(key, (String) config.get(key))
    }
    catch(ClassCastException cce) { 
      print "Unable to convert '" + key + "' to String: " 
      println cce.message
      // do nothing else..
    }
  } 

  mvnProcBuilder.directory(workspaceDir)

  out.println "Starting maven build: " 
  out.println "[cmd]: " + mvn2Cmd
  out.println "[opt]: " + mvn2EnvOpts
  out.println "[dir]: " + workspace
  out.println "-" * 80
  
  mvnProc = mvnProcBuilder.start();

  InputStream procOutput = mvnProc.getInputStream();
  BufferedReader outputReader = new BufferedReader(new InputStreamReader(procOutput))

  String line = null;
  while((line = outputReader.readLine()) != null ) { 
    out.println( line )
  }

  // Just to make sure
  mvnProc.waitFor()

  if( mvnProc.exitValue() != 0 ) { 
    String message = "Maven build failed: ["
    mvn2Cmd.each { arg -> message += arg + " " } 
    message += "]"
    throw new ScriptException(message)
  }

} 
catch( Exception e ) {
  out.println "-" * 80
  if( dbAllocated == 1 ) { 
    out.println "(Did not or) unable to allocate database [" + dbInfo + "]"
  }
  if( jdbcDriverDownloaded == 1 ) { 
    out.println "Unable to download jdbc driver [" + jdbcInfo + "]"
  }

  if( out != null ) { 
    out.println e.class.name + ": " + e.message
    if( !(e instanceof ScriptException) ) { 
      PrintStream outPrint = new PrintStream(out)
      e.printStackTrace(out)
    }
  }
  else { 
    println e.class.name + ": " + e.message
    if( !(e instanceof ScriptException) ) { 
      e.printStackTrace()
    }
  }

  throw e
}
finally { 
  out.println "-" * 80
  if (allocation != null) {
    out.print sdf.format(new Date()) + ": " 
    out.println "Releasing database: " + dbInfo
    allocation.release()
    allocation.clear()
  }
  if( settingsXml != null && false ) { 
    // Delete the settingsXml so that other jobs/maven builds
    //  don't try to access a database that they no longer should
    settingsXml.delete()
  }
}

