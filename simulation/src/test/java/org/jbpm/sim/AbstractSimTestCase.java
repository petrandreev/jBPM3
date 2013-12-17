package org.jbpm.sim;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;

public abstract class AbstractSimTestCase extends AbstractJbpmTestCase {

  private JbpmContext jbpmContext;

  protected void setUp() throws Exception {
    super.setUp();
    jbpmContext = JbpmConfiguration.getInstance("org/jbpm/sim/simulation.cfg.xml").createJbpmContext();
  }

  protected void tearDown() throws Exception {
    jbpmContext.close();
    super.tearDown();
  }

}