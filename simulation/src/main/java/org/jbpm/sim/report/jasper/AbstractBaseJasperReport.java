package org.jbpm.sim.report.jasper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.PrintServiceAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.PrinterName;

import org.jbpm.JbpmException;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanArrayDataSource;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporterParameter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Helper class to instantiate, fill, show and print the basic JasperReports
 * report shipping with the simulation framework.
 * 
 * Code basically copied from the jasper commands in the jbop framework (a
 * development from camunda, unfortunately closed source).
 * 
 * @author bernd.ruecker@camunda.com
 */
public abstract class AbstractBaseJasperReport {

  private boolean initialized = false;

  private JasperReport mainReport;

  /**
   * Map<String, JasperReport>
   */
  private Map subReports = new HashMap();

  private JasperPrint jasperPrint;

  public void show() {
    JasperViewer.viewReport(getJasperPrint(), false);
  }

  public void printOnDefaultPrinter(boolean withPrintDialog) {
    try {
      JasperPrint jp = getJasperPrint();
      JasperPrintManager.printPages(jp, 0, jp.getPages().size() - 1, withPrintDialog);
    }
    catch (JRException ex) {
      throw new JbpmException("could not print report", ex);
    }
  }

  public void print(String printerName) {
    PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
    printRequestAttributeSet.add(MediaSizeName.ISO_A4);

    PrintServiceAttributeSet printServiceAttributeSet = new HashPrintServiceAttributeSet();
    printServiceAttributeSet.add(new PrinterName(printerName, null));
    // printServiceAttributeSet.add(new PrinterName("HP LaserJet 4P", null));

    JRPrintServiceExporter exporter = new JRPrintServiceExporter();

    exporter.setParameter(JRExporterParameter.JASPER_PRINT, getJasperPrint());
    exporter.setParameter(JRPrintServiceExporterParameter.PRINT_REQUEST_ATTRIBUTE_SET, printRequestAttributeSet);
    exporter.setParameter(JRPrintServiceExporterParameter.PRINT_SERVICE_ATTRIBUTE_SET, printServiceAttributeSet);
    exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PAGE_DIALOG, Boolean.FALSE);
    exporter.setParameter(JRPrintServiceExporterParameter.DISPLAY_PRINT_DIALOG, Boolean.FALSE);

    try {
      exporter.exportReport();
    }
    catch (JRException ex) {
      throw new JbpmException("could not print report", ex);
    }
  }

  public OutputStream getAsPDF() {
    try {
      OutputStream result = new ByteArrayOutputStream();
      JasperExportManager.exportReportToPdfStream(getJasperPrint(), result);
      return result;
    }
    catch (JRException ex) {
      throw new JbpmException("could not export report to pdf", ex);
    }
  }

  private void initReports() {
    if (initialized) {
      return;
    }
    try {
      String resource = getReportPath();
      InputStream reportStream = getClass().getResourceAsStream(resource);
      if (reportStream == null) {
        throw new JbpmException("report resource not found: " + resource);
      }
      mainReport = (JasperReport) JRLoader.loadObject(reportStream);

      Map subreports = getSubreportPaths();
      if (subreports != null) {
        for (Iterator iter = subreports.keySet().iterator(); iter.hasNext();) {
          String subreport = (String) iter.next();
          resource = (String) subreports.get(subreport);
          reportStream = getClass().getResourceAsStream(resource);
          if (reportStream == null) {
            throw new JbpmException("report resource not found: " + subreports.get(subreport));
          }
          subReports.put(subreport, JRLoader.loadObject(reportStream));
        }
      }
      initialized = true;
    }
    catch (JRException ex) {
      throw new JbpmException("could not load report", ex);
    }
  }

  public JasperPrint getJasperPrint() {
    if (jasperPrint == null) {
      initReports();
      try {
        Map params = getReportParameters();
        if (params == null) params = new HashMap();

        params.putAll(subReports);

        jasperPrint = JasperFillManager.fillReport(mainReport, params, new JRBeanArrayDataSource(
          getContent()));
      }
      catch (JRException ex) {
        throw new JbpmException("could not fill report", ex);
      }
    }
    return jasperPrint;
  }

  public abstract String getReportPath();

  /**
   * Map<String, String>
   */
  public abstract Map getSubreportPaths();

  /**
   * Map<String, Object>
   */
  public abstract Map getReportParameters();

  public abstract Object[] getContent();
}
