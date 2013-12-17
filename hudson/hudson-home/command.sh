#!/bin/sh
#
# A script that uses Maven to build the project and
# execute its test suite against a given target container 
#
# $Id: command.sh 4170 2009-03-06 15:32:28Z thomas.diesler@jboss.com $

WORKSPACE=`pwd`
JBPMDIR=$WORKSPACE/jbpm
DISTRODIR=$JBPMDIR/modules/distribution/target

case "$CONTAINER" in
  jboss422*)
    JBOSS_BUILD=jboss-4.2.2.GA
  ;;

  jboss423*)
    JBOSS_BUILD=jboss-4.2.3.GA
  ;;

  jboss500*)
    JBOSS_BUILD=jboss-5.0.0.GA
  ;;
esac

#
# Unzip the JBoss build
#
rm -rf $JBOSS_BUILD
unzip -q $HUDSON_HOME/../jboss/$JBOSS_BUILD.zip

JBOSS_HOME=$WORKSPACE/$JBOSS_BUILD
ENVIRONMENT="-Ddatabase=$DATABASE -Djbpm.target.container=$CONTAINER -Djboss.home=$JBOSS_HOME -Djboss.bind.address=$JBOSS_BINDADDR"

#
# Build distro
#
cd $JBPMDIR
MVN_CMD="mvn -U $ENVIRONMENT -Pdistro clean install"
echo $MVN_CMD; $MVN_CMD; MVN_STATUS=$?
if [ $MVN_STATUS -ne 0 ]; then
  echo maven exit status $MVN_STATUS
  exit 1
fi

#
# build the tests
#
MVN_CMD="mvn -o $ENVIRONMENT process-test-classes"
echo $MVN_CMD; $MVN_CMD 2>&1; MVN_STATUS=$?
if [ $MVN_STATUS -ne 0 ]; then
  echo maven exit status $MVN_STATUS
  exit 1
fi

#
# Deploy distro
#
AUTO_INSTALL=modules/distribution/target/resources/auto-install-template.xml; cat $AUTO_INSTALL;
JAVA_CMD="java -jar $DISTRODIR/jbpm-installer-$JBPM_VERSION.jar $AUTO_INSTALL"
echo $JAVA_CMD; $JAVA_CMD 

# FIXME: Autoinstall does not respect conditions
# http://jira.codehaus.org/browse/IZPACK-153
rm $JBOSS_HOME/server/$JBOSS_SERVER/deploy/jbpm/jbpm-*-ds.xml
rm $JBOSS_HOME/server/$JBOSS_SERVER/deploy/jbpm/jbpm-service.sar/hibernate.cfg.xml
cp $JBOSS_HOME/docs/examples/jbpm/jbpm-$DATABASE-ds.xml $JBOSS_HOME/server/$JBOSS_SERVER/deploy/jbpm/jbpm-$DATABASE-ds.xml
cp $JBOSS_HOME/docs/examples/jbpm/hibernate.cfg.$DATABASE.xml $JBOSS_HOME/server/$JBOSS_SERVER/deploy/jbpm/jbpm-service.sar/hibernate.cfg.xml

# FIXME: find out whether jTDS can be made to work with XA data source
# https://jira.jboss.org/jira/browse/JBPM-1818
SYBASE_JDBC_DRIVER=~/.m2/repository/com/sybase/jconnect/6.0.5/jconnect-6.0.5.jar
if [ -f $SYBASE_JDBC_DRIVER ]; then
  echo "cp $SYBASE_JDBC_DRIVER $JBOSS_HOME/server/$JBOSS_SERVER/deploy/jbpm/jbpm-service.sar"
  cp $SYBASE_JDBC_DRIVER $JBOSS_HOME/server/$JBOSS_SERVER/deploy/jbpm/jbpm-service.sar
fi

#
# start jbossas
#
$JBPMDIR/hudson/jboss/bin/jboss.sh $JBOSS_HOME start $JBOSS_BINDADDR

# Was it successfully started?
$JBPMDIR/hudson/jboss/bin/http-spider.sh $JBOSS_BINDADDR:8080 $WORKSPACE
if [ -e $WORKSPACE/spider.failed ]; then
  tail -n 100 $JBOSS_HOME/server/$JBOSS_SERVER/log/server.log
  $JBPMDIR/hudson/jboss/bin/jboss.sh $JBOSS_HOME stop $JBOSS_BINDADDR
  exit 1
fi

#
# log dependency tree
#
MVN_CMD="mvn -o $ENVIRONMENT dependency:tree"
echo $MVN_CMD; $MVN_CMD | tee $WORKSPACE/dependency-tree.txt

#
# execute tests
#
MVN_CMD="mvn -o -fae $ENVIRONMENT test"
echo $MVN_CMD; $MVN_CMD 2>&1 | tee $WORKSPACE/tests.log
cat $WORKSPACE/tests.log | egrep FIXME\|FAILED | sort -u | tee $WORKSPACE/fixme.txt
cat $WORKSPACE/fixme.txt | egrep "\[\S*]" > $WORKSPACE/errata-$CONTAINER.txt || :

#
# stop jbossas
#
$JBPMDIR/hudson/jboss/bin/jboss.sh $JBOSS_HOME stop
cp $JBOSS_HOME/server/$JBOSS_SERVER/log/boot.log $WORKSPACE/jboss-boot.log
cp $JBOSS_HOME/server/$JBOSS_SERVER/log/server.log $WORKSPACE/jboss-server.log
