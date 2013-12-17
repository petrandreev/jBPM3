package org.jbpm.sim.def;

import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.util.Clock;

import desmoj.core.simulator.SimTime;

/**
 * This SimulationClock can be configured to be the clock for jbpm
 * <b>and</b> observe the real simulation clock of DESMOJ.
 * 
 * So it always returns the current simulation time, but converted in a Date object
 * (simulation time 1 = new Date(1))
 * 
 * @author bernd.ruecker@camunda.com
 */
public class JbpmSimulationClock implements Clock.DateGenerator, Observer {
  
  /**
   * the clock always reflects the current simulation time as a <code>java.util.Date</code>
   */
  public static Date currentTime = new Date(0);
  
  /**
   * For the moment, we think of the smallest amount of model time to be seconds
   * but the representation as double is in hours.
   * 
   * Maybe not very realistic by the way, but sufficient for the moment. 
   * See ApiDoc on getAsDouble for the weakness of conversation for more
   * information.
   */
  public static long timeScaleFactor = 86400;
  
  private static Log log = LogFactory.getLog(JbpmSimulationClock.class);

  public Date getCurrentTime() {
    return currentTime;
  }

  public void update(Observable o, Object arg) {
    SimTime currentSimTime = (SimTime) arg;
    
    long converterValue = getAsLong(currentSimTime.getTimeValue());
    log.warn("conversion of date from double to long is dangerous: " + currentSimTime.getTimeValue() + " --> " + converterValue );
    
    currentTime = new Date( converterValue );
  }

  /**
   * Currently we have a problem of converting the simulation time (represented as double)
   * to a representation as long, needed for the jbpm log.
   * 
   * We would need to know the exact scale and steps of the simulation time, to calculate the long value.
   * And currently I am not sure about statistical effects when just multiplying 
   * the double and truncate the result to long.
   * 
   * But at the moment I ignore this problem, because it doesn't affect the simulation run,
   * it only affects the time seen in jBPM, and this time is only used for writing
   * logs. It may get a problem if we start to use Timers in the simulation, which must be 
   * triggered to some simulation time.
   * 
   * So for the moment only log this problem and be careful with judging jbpm log times.
   * 
   * TODO: Improve the time conversion double -> long
   */
  public static double getAsDouble(long l) {
    return l / timeScaleFactor;
  }

  public static long getAsLong(double d) {
    return new Double(d * timeScaleFactor ).longValue();
  }

}
