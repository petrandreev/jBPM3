package org.jbpm.signal;

import org.jbpm.graph.def.GraphElement;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.svc.Service;

/**
 * An extension to the jBPM event architecture 
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Dec-2008
 */
public interface EventService extends Service
{
  static String SERVICE_NAME = "event";

  void fireEvent(String eventType, GraphElement graphElement, ExecutionContext executionContext);
}
