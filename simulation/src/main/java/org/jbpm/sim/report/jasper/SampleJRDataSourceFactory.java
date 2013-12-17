package org.jbpm.sim.report.jasper;

import org.jbpm.sim.report.dto.QueueStatisticsResult;
import org.jbpm.sim.report.dto.TimeSeriesResult;
import org.jbpm.sim.report.dto.TimedValue;
import org.jbpm.sim.report.dto.UtilizationStatisticsResult;
import org.jbpm.sim.report.dto.ValueStatisticResult;


/**
 * class to provide sample data source to be used in Report Editor
 * to test the reports
 * 
 * @author bernd.ruecker@camunda.com
 */
public class SampleJRDataSourceFactory {
//
//  public static JRBeanArrayDataSource createTaskDistributionDS() {
//    Object[] row1 = new Object[] {"Task 1", Double.valueOf(373.0)};
//    Object[] row2 = new Object[] {"Task 2", Double.valueOf(100.0)};
//    Object[] row3 = new Object[] {"Task 3", Double.valueOf(450.0)};
//    
//    return new JRBeanArrayDataSource( new Object[] {row1, row2, row3} );
//  }
  
  public static ValueStatisticResult[] createTaskDistributionArray() {
    ValueStatisticResult row1 = new ValueStatisticResult("Task 1", "Scenario 1", 373.0, 0, 0, 0, 0);
    ValueStatisticResult row5 = new ValueStatisticResult("Task 1", "Scenario 3", 353.0, 0, 0, 0, 0);
    ValueStatisticResult row6 = new ValueStatisticResult("Task 1", "Scenario 2", 400.0, 0, 0, 0, 0);
    ValueStatisticResult row7 = new ValueStatisticResult("Task 1", "Scenario 4", 320.0, 0, 0, 0, 0);

    ValueStatisticResult row2 = new ValueStatisticResult("Task 2", "Scenario 1", 100.0, 0, 0, 0, 0);
    ValueStatisticResult row3 = new ValueStatisticResult("Task 3", "Scenario 3", 470.0, 0, 0, 0, 0);
    ValueStatisticResult row4 = new ValueStatisticResult("Task 4", "Scenario 4", 470.0, 0, 0, 0, 0);
   
    return new ValueStatisticResult[] {row1, row2, row3, row4, row5, row6, row7};
  }
  
  public static QueueStatisticsResult[] createQueueStaticsArray() {
    QueueStatisticsResult row1 = new QueueStatisticsResult("Clerk",   "Scenario 1", "FIFO", 0, 0, 0, 0,  20,  3.6, 0,  20,   4.8, 0, 0);
    QueueStatisticsResult row2 = new QueueStatisticsResult("Tester",  "Scenario 1", "FIFO", 0, 0, 0, 0, 100, 23.5, 0, 500, 178.0, 0, 0);
    QueueStatisticsResult row3 = new QueueStatisticsResult("Manager", "Scenario 1", "FIFO", 1, 1, 1, 1,  120, 30.5, 0, 250, 144.0, 1, 1);
    QueueStatisticsResult row4 = new QueueStatisticsResult("Manager", "Scenario 2", "FIFO", 0, 0, 0, 0,   80, 40.5, 0, 300, 120.0, 0, 0);
   
    return new QueueStatisticsResult[] {row1, row2, row3, row4};
  }  
  
  public static TimedValue[] createTimedValueArray() {
    TimedValue row1 = new TimedValue(1,20);
    TimedValue row2 = new TimedValue(2,0);
    TimedValue row3 = new TimedValue(3,5);
    TimedValue row4 = new TimedValue(4,21);
   
    return new TimedValue[] {row1, row2, row3, row4};
  }  
  
  public static TimeSeriesResult[] createTimeSeriesArray() {
    TimeSeriesResult row1 = new TimeSeriesResult("Manager", "Scenario 1", new double[] {1, 2, 3, 4, 5, 8}, new double[] {0, 0, 10, 10, 12, 5});
    TimeSeriesResult row2 = new TimeSeriesResult("Clerk", "Scenario 1", new double[] {1, 2, 3, 4, 5, 6}, new double[] {3, 20, 18, 12, 21, 20});
    TimeSeriesResult row3 = new TimeSeriesResult("Clerk", "Scenario 2", new double[] {1, 2, 3, 4, 5, 8}, new double[] {0, 0, 10, 10, 12, 5});
   
    return new TimeSeriesResult[] {row1, row2, row3};
  } 
  
  public static UtilizationStatisticsResult[] createUtilizationStatatisticsArray() {
    UtilizationStatisticsResult row1 = new UtilizationStatisticsResult("Manager", "Scenario 1", 0, 3,  0, 3, 2, 1, 10, 8, 0, 0.02);
    UtilizationStatisticsResult row2 = new UtilizationStatisticsResult("Manager", "Scenario 2", 0, 3,  0, 3, 2, 1, 10, 8, 0,0.02);
    UtilizationStatisticsResult row3 = new UtilizationStatisticsResult("Clerk",   "Scenario 1", 0, 10, 0, 10, 1, 0.3, 3, 30, 0,0.02);
   
    return new UtilizationStatisticsResult[] {row1, row2, row3};
  }     
}
