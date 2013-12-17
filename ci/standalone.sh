#!/bin/sh
#
# Build the project with Maven and run the standalone test suite 
#
# Environment variables
# JAVA_HOME      JDK location
# DATABASE       one of db2, hsqldb, mssql, mysql, oracle, postgresql
# MAVEN_SETTINGS Maven settings file; optional, defaults to ci/settings-qa.xml

# Set up Maven
[ -z $MAVEN_SETTINGS ] && MAVEN_SETTINGS=ci/settings-qa.xml
export MAVEN_OPTS="-Ddatabase=$DATABASE -Xms16m -Xmx256m"

# Display Maven version
mvn -s $MAVEN_SETTINGS -v

# Run standalone test suite
mvn -s $MAVEN_SETTINGS -U '-Dsurefire.jvm.args=-Xms16m -Xmx256m' \
  -Dmaven.test.failure.ignore=true clean install
