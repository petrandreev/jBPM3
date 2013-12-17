package org.jbpm.jbpm1686;

import org.jbpm.AbstractJbpmTestCase;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;

/**
 * NPE when reading a process definition with a decision name which contains the "/" character
 * 
 * https://jira.jboss.org/jira/browse/JBPM-1686
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 12-Feb-2009
 */
public class JBPM1686Test extends AbstractJbpmTestCase
{
  public void testProcessDefinition()
  {
    try
    {
      ProcessDefinition.parseXmlString(
          "<process-definition>" +
          "  <start-state>" +
          "    <transition to='d/e' />" +
          "  </start-state>" +
          "  <decision name='d/e'>" +
          "    <transition name='one' to='a'>" +
          "      <condition>#{a == 1}</condition>" +
          "    </transition>" +
          "    <transition name='three' to='c'>" +
          "      <condition>#{a == 3}</condition>" +
          "    </transition>" +
          "  </decision>" +
          "  <state name='a' />" +
          "  <state name='c' />" +
          "</process-definition>");
      
      fail("JpdlException expected");
    }
    catch (JpdlException ex)
    {
      // expected
    }
  }
}
