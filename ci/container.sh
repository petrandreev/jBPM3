#!/bin/sh
#
# Build the project with Maven and
# run the enterprise test suite inside a container
#
# Exported shell variables required:
# WORKSPACE      work directory
# JAVA_HOME      JDK location
# JAVA_15_HOME   JDK 1.5 location; required if JAVA_HOME points to JDK 1.4
# DATABASE       one of db2, hsqldb, mssql, mysql, oracle, postgresql
# CONTAINER      one of jboss405, jboss423, jboss501, jboss510
# MYTESTIP_1     bind address
# MCAST_ADDR     multicast address
# SOURCE_REPO    JBoss distributions directory
# MAVEN_SETTINGS Maven settings file; optional, defaults to ci/settings-qa.xml

PROGNAME=`basename $0`

#
# Helper to puke
#
die() {
  echo "$PROGNAME: $*"
  exit 1
}

# Determine JBoss AS version
case $CONTAINER in
  jboss405)
    JBOSS_VERSION=jboss-4.0.5.GA
    ;;
  jboss423)
    JBOSS_VERSION=jboss-4.2.3.GA
    ;;
  jboss501)
    JBOSS_VERSION=jboss-5.0.1.GA
    ;;
  jboss510)
    JBOSS_VERSION=jboss-5.1.0.GA
    ;;
  *)
    die "Invalid container: $CONTAINER"
    ;;
esac
JBOSS_HOME=$WORKSPACE/$JBOSS_VERSION
JBOSS_SERVER=all

# Determine Java platform version
JAVA=$JAVA_HOME/bin/java
JAVA_VERSION=`$JAVA -version 2>&1 | grep version | awk '{ print substr($3, 2, 3); }'`

# Set up Maven
export MAVEN_OPTS="-Ddatabase=$DATABASE -Xms16m -Xmx256m"
[ -z $MAVEN_SETTINGS ] && MAVEN_SETTINGS=ci/settings-qa.xml

if [ $JAVA_VERSION = 1.4 ]; then
  # Tell Maven to use JDK 1.4 for running tests 
  export MAVEN_OPTS="$MAVEN_OPTS -Djvm=$JAVA"

  # Switch to JDK 1.5 for Maven builds and jBPM installation
  JAVA_14=$JAVA
  JAVA_14_HOME=$JAVA_HOME

  JAVA=$JAVA_15_HOME/bin/java
  export JAVA_HOME=$JAVA_15_HOME
fi

# Display Maven version
mvn -s $MAVEN_SETTINGS -v

# Determine jBPM version
mvn -s $MAVEN_SETTINGS -Dexpression=project.version help:evaluate > $WORKSPACE/version.txt
JBPM_VERSION=`grep '^3\.' $WORKSPACE/version.txt`

# Build jBPM
mvn -U -s $MAVEN_SETTINGS -Pdistro -Djbpm.home=$WORKSPACE/jbpm-$JBPM_VERSION \
  -Dcontainer=$CONTAINER -Djboss.home=$JBOSS_HOME -Djboss.server=$JBOSS_SERVER clean install
if [ $? -ne 0 ]; then
  die "Failed to build distro"
fi

# Install JBoss AS 
rm -rf $JBOSS_HOME
unzip -q -d $WORKSPACE $SOURCE_REPO/jboss/$JBOSS_VERSION.zip

# Install jBPM
$JAVA -jar distribution/target/jbpm-distribution-$JBPM_VERSION-installer.jar \
  distribution/target/classes/auto-install-template.xml
if [ $? -ne 0 ]; then
  die "Failed to install distro"
fi

# Fetch proprietary JDBC drivers
mvn -s $MAVEN_SETTINGS -N -DoutputFile=$WORKSPACE/dependencies.txt dependency:resolve

# DB2 driver
if [ "$DATABASE" = "db2" ]; then
  DB2JCC_VERSION=`grep com.ibm:db2jcc:jar $WORKSPACE/dependencies.txt | awk -F : '{ print $4 }'`
  DB2JCC_JAR=~/.m2/repository/com/ibm/db2jcc/$DB2JCC_VERSION/db2jcc-$DB2JCC_VERSION.jar
  if [ -f $DB2JCC_JAR ]; then
  	ln -s $DB2JCC_JAR $JBOSS_HOME/server/$JBOSS_SERVER/lib
  else
    die "DB2 driver not found: $DB2JCC_JAR"
  fi
  DB2JCC_LICENSE=~/.m2/repository/com/ibm/db2jcc_license_cu/$DB2JCC_VERSION/db2jcc_license_cu-$DB2JCC_VERSION.jar
  if [ -f $DB2JCC_LICENSE ]; then
  	ln -s $DB2JCC_LICENSE $JBOSS_HOME/server/$JBOSS_SERVER/lib
  else
    die "DB2 driver license not found: $DB2JCC_LICENSE"
  fi
fi

# MSSQL driver
if [ "$DATABASE" = "mssql" ]; then
  MSJDBC_VERSION=`grep com.microsoft.sqlserver:msjdbc $WORKSPACE/dependencies.txt | awk -F : '{ print $4 }'`
  MSJDBC_JAR=~/.m2/repository/com/microsoft/sqlserver/msjdbc/$MSJDBC_VERSION/msjdbc-$MSJDBC_VERSION.jar
  if [ -f $MSJDBC_JAR ]; then
  	ln -s $MSJDBC_JAR $JBOSS_HOME/server/$JBOSS_SERVER/lib
  else
    die "SQL Server driver not found: $MSJDBC_JAR"
  fi
fi

# Oracle driver
if [ "$DATABASE" = "oracle" ]; then
  OJDBC_VERSION=`grep com.oracle:ojdbc14 $WORKSPACE/dependencies.txt | awk -F : '{ print $4 }'`
  OJDBC_JAR=~/.m2/repository/com/oracle/ojdbc14/$OJDBC_VERSION/ojdbc14-$OJDBC_VERSION.jar
  if [ -f $OJDBC_JAR ]; then
    ln -s $OJDBC_JAR $JBOSS_HOME/server/$JBOSS_SERVER/lib
  else
    die "Oracle driver not found: $OJDBC_JAR"
  fi
fi

# Sybase driver
if [ "$DATABASE" = "sybase" ]; then
  JCONNECT_VERSION=`grep com.sybase:jconnect $WORKSPACE/dependencies.txt | awk -F : '{ print $4 }'`
  JCONNECT_JAR=~/.m2/repository/com/sybase/jconnect/$JCONNECT_VERSION/jconnect-$JCONNECT_VERSION.jar
  if [ -f $JCONNECT_JAR ]; then
    ln -s $JCONNECT_JAR $JBOSS_HOME/server/$JBOSS_SERVER/lib
  else
    die "Sybase driver not found: $JCONNECT_JAR"
  fi
fi

if [ $JAVA_VERSION = 1.4 ]; then
  # Switch back to JDK 1.4 for JBoss AS launch
  JAVA=$JAVA_14
fi

# BEGIN section taken from run.sh

# Read an optional running configuration file
if [ -z "$RUN_CONF" ]; then
  RUN_CONF="$JBOSS_HOME/bin/run.conf"
fi
if [ -r "$RUN_CONF" ]; then
  . "$RUN_CONF"
fi

# Force IPv4 on Linux systems since IPv6 doesn't work correctly with jdk5 and lower
JAVA_OPTS="$JAVA_OPTS -Djava.net.preferIPv4Stack=true"

# Setup the classpath
runjar="$JBOSS_HOME/bin/run.jar"
if [ ! -f "$runjar" ]; then
	die "Missing required file: $runjar"
fi
JBOSS_BOOT_CLASSPATH="$runjar"

if [ -z "$JBOSS_CLASSPATH" ]; then
	JBOSS_CLASSPATH="$JBOSS_BOOT_CLASSPATH"
else
  JBOSS_CLASSPATH="$JBOSS_CLASSPATH:$JBOSS_BOOT_CLASSPATH"
fi

# Setup JBoss specific properties
JAVA_OPTS="-Dprogram.name=$PROGNAME $JAVA_OPTS"

# Execute the JVM in the background
$JAVA $JAVA_OPTS \
  -Djava.endorsed.dirs="$JBOSS_HOME/lib/endorsed" \
  -classpath "$JBOSS_CLASSPATH" \
  org.jboss.Main -b $MYTESTIP_1 -c $JBOSS_SERVER \
  -g jbpm3-$CONTAINER-$DATABASE -u $MCAST_ADDR &> /dev/null &
JBOSS_PID=$!

# END section adapted from run.sh

# Symlink server log file
ln -sf $JBOSS_HOME/server/$JBOSS_SERVER/log/server.log $WORKSPACE

# Was it successfully started?
for (( TRY=1 ; ; TRY++ )); do
  curl --head --connect-timeout 15 --fail http://$MYTESTIP_1:8080/jbpm-console
  if [ $? -eq 0 ]; then
    break
  elif [ $TRY -lt 5 ]; then
    sleep 30
  else
    tail -n 100 $WORKSPACE/server.log
    kill $JBOSS_PID
    die "JBoss AS failed to start"
  fi
done

# Run enterprise test suite
mvn -f enterprise/pom.xml -s $MAVEN_SETTINGS -Djboss.bind.address=$MYTESTIP_1 \
  integration-test | tee $WORKSPACE/tests.log

# Stop JBoss AS
$JBOSS_HOME/bin/shutdown.sh -s jnp://$MYTESTIP_1:1099 -S
