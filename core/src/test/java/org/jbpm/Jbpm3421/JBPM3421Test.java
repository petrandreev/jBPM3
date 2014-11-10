package org.jbpm.Jbpm3421;

import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.JpdlException;
import org.jbpm.jpdl.xml.Problem;

@SuppressWarnings({
  "rawtypes"
})
public class JBPM3421Test extends TestCase {

  private static final Log log = LogFactory.getLog(JBPM3421Test.class);

  private static final String[] jpdlNamespaces = {
    "urn:jbpm.org:jpdl-3.0", "urn:jbpm.org:jpdl-3.1", "urn:jbpm.org:jpdl-3.2",
  };

  public void testCancelEventTimerAttributeIsvalid() {
    for (int i = 0; i < 3; ++i) {
      try {
        ProcessDefinition.parseXmlString("<process-definition xmlns='" + jpdlNamespaces[i]
          + "' name='pd'>" + "  <task-node name='a'>" + "    <task name='clean ceiling'>"
          + "      <timer duedate='2 business minutes' cancel-event='task-end'>"
          + "        <action class='org.jbpm.taskmgmt.exe.TaskEventExecutionTest$PlusPlus' />"
          + "      </timer>" + "    </task>" + "  </task-node>" + "</process-definition>");
      }
      catch (JpdlException je) {
        Iterator iter = je.getProblems().iterator();
        while (iter.hasNext()) {
          Problem xmlProb = (Problem) iter.next();
          log.error("jpdl 3." + i + ": " + getTypeDescription(xmlProb.getLevel()) + ": "
            + xmlProb.getDescription());
        }
        fail("Exception thrown with jpdl 3." + i + ": " + je.getMessage());
      }
    }
  }

  static String getTypeDescription(int level) {
    switch (level) {
    case Problem.LEVEL_FATAL:
      return "FATAL";
    case Problem.LEVEL_ERROR:
      return "ERROR";
    case Problem.LEVEL_WARNING:
      return "WARNING";
    case Problem.LEVEL_INFO:
      return "INFO";
    }
    return null;
  }

}
