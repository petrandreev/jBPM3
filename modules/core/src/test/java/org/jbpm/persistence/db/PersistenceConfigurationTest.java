package org.jbpm.persistence.db;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;

public class PersistenceConfigurationTest extends AbstractJbpmTestCase {

  public void testDbPersistenceConfigurationDefault() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context>" +
      "    <service name='persistence' factory='"+DbPersistenceServiceFactory.class.getName()+"' />" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory("persistence");
    assertNull(dbPersistenceServiceFactory.dataSourceJndiName);
    assertFalse(dbPersistenceServiceFactory.isCurrentSessionEnabled);
    assertTrue(dbPersistenceServiceFactory.isTransactionEnabled);
  }

  public void testDbPersistenceConfigurationProvidedValues() {
    JbpmConfiguration jbpmConfiguration = JbpmConfiguration.parseXmlString(
      "<jbpm-configuration>" +
      "  <jbpm-context>" +
      "    <service name='persistence'>" +
      "      <factory>" +
      "        <bean class='"+DbPersistenceServiceFactory.class.getName()+"'>" +
      "          <field name='dataSourceJndiName'><string value='java:/myDataSource'/></field> " +
      "          <field name='isCurrentSessionEnabled'><true /></field> " +
      "          <field name='isTransactionEnabled'><false /></field> " +
      "        </bean>" +
      "      </factory>" +
      "    </service>" +
      "  </jbpm-context>" +
      "</jbpm-configuration>"
    );
    DbPersistenceServiceFactory dbPersistenceServiceFactory = (DbPersistenceServiceFactory) jbpmConfiguration.getServiceFactory("persistence");
    assertEquals("java:/myDataSource", dbPersistenceServiceFactory.dataSourceJndiName);
    assertTrue(dbPersistenceServiceFactory.isCurrentSessionEnabled);
    assertFalse(dbPersistenceServiceFactory.isTransactionEnabled);
  }
}
