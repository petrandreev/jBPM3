package org.jbpm.sim;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.graph.def.Event;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.instantiation.UserCodeInterceptorConfig;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.logging.log.ProcessLog;
import org.jbpm.sim.action.StartTaskAndPlanCompletion;
import org.jbpm.sim.def.JbpmSimulationClock;
import org.jbpm.sim.jpdl.SimulationDefinition;
import org.jbpm.util.Clock;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * @deprecated
 * @author bernd.ruecker@camunda.com
 */
public abstract class SimulationTestCase extends TestCase {

  private static Log log = LogFactory.getLog(SimulationTestCase.class);
  
  static DateFormat dateFormat = new SimpleDateFormat("HH:mm");
//  static JbpmSimulationClock simulationClock = new JbpmSimulationClock();
  static JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance("org/jbpm/sim/simulation.cfg.xml");
  static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
  
  static {
//    Clock.dateGenerator = simulationClock;
//    UserCodeInterceptorConfig.userCodeInterceptor = new SimulationUserCodeInterceptor();
  }
  
  public static void setCurrentTime(String timeText) {
    try {
      JbpmSimulationClock.currentTime = dateFormat.parse(timeText);
    } catch (ParseException e) {
      throw new RuntimeException("invalid timeText: "+timeText, e);
    }
  }

  public void produceReports(ProcessInstance processInstance) {
    List logs = processInstance.getLoggingInstance().getLogs();
    Iterator iter = logs.iterator();
    while (iter.hasNext()) {
      ProcessLog processLog = (ProcessLog) iter.next();
      log.info(dateFormat.format(processLog.getDate())+" | "+processLog.toString());
    }
  }
}
