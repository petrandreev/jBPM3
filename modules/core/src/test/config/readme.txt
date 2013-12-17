This directory contains a template for the database test 
configuration properties.  

To get started, copy this directory to ${user.home}/jbpm/dbtests  
so all the database dirs are then located in ${user.home}/jbpm/dbtests/config

Usage is as follows:

1) With ant's -D or in your ${user.home}/jbpm/build.properties specify a config 
property ${custom.db.config} and give it a value that corresponds with 
one of these database directories.

2) If this property is set, all the contents of that directory will overwrite
the default configurations as in jpdl/jar/src/main/config for the test run

3) Put the driver jar files for the database in 
${user.home}/jbpm/dbtests/config/${custom.db.config}/*.jar  So e.g. the oracle driver jar 
file could have the following location: ${user.home}/jbpm/dbtests/config/oracle/classes12.jar 