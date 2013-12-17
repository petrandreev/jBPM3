package org.jbpm.mail;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration.Configs;
import org.jbpm.JbpmException;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.jpdl.el.ELException;
import org.jbpm.jpdl.el.VariableResolver;
import org.jbpm.jpdl.el.impl.JbpmExpressionEvaluator;
import org.jbpm.util.ClassLoaderUtil;

public class Mail implements ActionHandler {

  private static final Log log = LogFactory.getLog(Mail.class);
  
  private String template;
  private String to;
  private String actors;
  private String cc;
  private String ccActors;
  private String bcc;
  private String bccActors;
  private String subject;
  private String text;

  private transient ExecutionContext executionContext;

  private static final long serialVersionUID = 2L;

  public Mail() {
  }

  public Mail(String template, String actors, String to, String subject, String text) {
    this.template = template;
    this.actors = actors;
    this.to = to;
    this.subject = subject;
    this.text = text;
  }

  public Mail(String template, String actors, String to, String bccActors, String bcc,
    String subject, String text) {
    this.template = template;
    this.actors = actors;
    this.to = to;
    this.bccActors = bccActors;
    this.bcc = bcc;
    this.subject = subject;
    this.text = text;
  }

  public void execute(ExecutionContext executionContext) {
    this.executionContext = executionContext;
    send();
  }
  
  public List getRecipients() {
    Collection recipientsCollection = collectRecipients(actors, to);
    List recipientsList = null;
    if( recipientsCollection != null ) { 
      recipientsList =  new ArrayList(recipientsCollection);
    }
    return recipientsList;
  }

  public List getCcRecipients() {
    Collection ccRecipientsCollection = collectRecipients(ccActors, cc);
    List ccRecipientsList = null;
    if( ccRecipientsCollection != null ) { 
      ccRecipientsList =  new ArrayList(ccRecipientsCollection);
    }
    return ccRecipientsList;
  }

  public List getBccRecipients() {
    Collection recipients = collectRecipients(bccActors, bcc);
    if (Configs.hasObject("jbpm.mail.bcc.address")) {
      if (!(recipients instanceof ArrayList)) recipients = new ArrayList(recipients);
      recipients.addAll(tokenize(Configs.getString("jbpm.mail.bcc.address")));
    }
    List bccRecipientsList = null;
    if( recipients != null ) { 
      bccRecipientsList =  new ArrayList(recipients);
    }
    return bccRecipientsList;
  }

  private Collection collectRecipients(String actors, String addresses) {
    if (actors != null) {
      if (addresses != null) {
        Collection recipients = new ArrayList(evaluateActors(actors));
        recipients.addAll(evaluateAddresses(addresses));
        return recipients;
      }
      return evaluateActors(actors);
    }
    else if (addresses != null) {
      return evaluateAddresses(addresses);
    }
    return null;
  }

  private Collection evaluateActors(String expression) {
    Object value = evaluate(expression, Object.class);
    Collection actorIds;
    if (value instanceof String) {
      actorIds = tokenize((String) value);
    }
    else if (value instanceof Collection) {
      actorIds = (Collection) value;
    }
    else if (value instanceof String[]) {
      actorIds = Arrays.asList((String[]) value);
    }
    else {
      throw new JbpmException(expression + " returned " + value
        + " instead of comma-separated string, string array or collection");
    }
    return resolveAddresses(actorIds);
  }

  protected Collection resolveAddresses(Collection actorIds) {
    AddressResolver addressResolver = (AddressResolver) Configs.getObject("jbpm.mail.address.resolver");

    Collection addresses = new ArrayList();
    for (Iterator iter = actorIds.iterator(); iter.hasNext();) {
      String actorId = (String) iter.next();
      Object result = addressResolver.resolveAddress(actorId);

      if (result instanceof String) {
        addresses.add(result);
      }
      else if (result instanceof Collection) {
        addresses.addAll((Collection) result);
      }
      else if (result instanceof String[]) {
        addresses.addAll(Arrays.asList((String[]) result));
      }
      else if (result == null) {
        // no such actor or actor has no address
      }
      else {
        throw new JbpmException(addressResolver + " returned " + result
          + " instead of single string, string array or collection");
      }
    }
    return addresses;
  }

  private Collection evaluateAddresses(String expression) {
    Object value = evaluate(expression, Object.class);
    if (value instanceof String) return tokenize((String) value);
    if (value instanceof Collection) return (Collection) value;
    if (value instanceof String[]) return Arrays.asList((String[]) value);
    // give up
    throw new JbpmException(expression + " returned " + value
      + " instead of comma-separated string, string array or collection");
  }

  protected List tokenize(String text) {
    return text != null ? Arrays.asList(text.split("[,;:]+")) : null;
  }

  private Object evaluate(String expression, Class expectedType) {
    VariableResolver variableResolver = new MailVariableResolver(MailTemplates.getTemplateVariables(), JbpmExpressionEvaluator.getVariableResolver());
    return JbpmExpressionEvaluator.evaluate(expression, executionContext, expectedType, variableResolver, JbpmExpressionEvaluator.getFunctionMapper());
  }

  public String getSubject() {
    return subject != null ? (String) evaluate(subject, String.class) : null;
  }

  public String getText() {
    return text != null ? (String) evaluate(text, String.class) : null;
  }

  public String getFromAddress() {
    return Configs.hasObject("jbpm.mail.from.address") ?
      Configs.getString("jbpm.mail.from.address") : null;
  }

  public void send() {
    if (template != null) {
      Properties templateProperties = MailTemplates.getTemplateProperties(template);

      if (actors == null) actors = templateProperties.getProperty("actors");
      if (to == null) to = templateProperties.getProperty("to");
      if (cc == null) cc = templateProperties.getProperty("cc");
      if (ccActors == null) ccActors = templateProperties.getProperty("cc-actors");
      if (bcc == null) bcc = templateProperties.getProperty("bcc");
      if (bccActors == null) bccActors = templateProperties.getProperty("bcc-actors");
      if (subject == null) subject = templateProperties.getProperty("subject");
      if (text == null) text = templateProperties.getProperty("text");
    }

    MessageInfo msgInfo = new MessageInfo();
    msgInfo.sender = getFromAddress();
    msgInfo.recipients = getRecipients();
    msgInfo.ccRecipients = getCcRecipients();
    msgInfo.bccRecipients = getBccRecipients();
    if (nullOrEmpty(msgInfo.recipients) && nullOrEmpty(msgInfo.ccRecipients) && nullOrEmpty(msgInfo.bccRecipients))
      return;

    msgInfo.subject = getSubject();
    msgInfo.text = getText();

    if (log.isDebugEnabled()) {
      StringBuffer detail = new StringBuffer("sending email");
      if (!nullOrEmpty(msgInfo.recipients)) detail.append(" to ").append(msgInfo.recipients);
      if (!nullOrEmpty(msgInfo.ccRecipients)) detail.append(" cc ").append(msgInfo.ccRecipients);
      if (!nullOrEmpty(msgInfo.bccRecipients)) detail.append(" bcc ").append(msgInfo.bccRecipients);
      if (subject != null) detail.append(" about '").append(subject).append('\'');
      log.debug(detail.toString());
    }

    Properties sessionProperties = getServerProperties();
    msgInfo.session = getSession(sessionProperties);
    
    for (int retries = 4; retries >= 0; retries--) {
      try {
        sendInternal(msgInfo);
        break;
      }
      catch (MessagingException me) {
        if (retries == 0) throw new JbpmException("failed to send email", me);
        log.warn("failed to send email (" + retries + " retries left): " + me.getMessage());
      }
    }
  }

  /**
   * Kept for backwards compatibility
   * @deprecated
   */
  public static void send(Properties serverProperties, String sender, List recipients, String subject, String text) {
    send(serverProperties,sender,(Collection) recipients,subject,text);
  }
  
  public static void send(Properties serverProperties, String sender, Collection recipients, String subject, String text) {
    MessageInfo msgInfo = new MessageInfo(null, sender, recipients, null, null, subject, text);
    send(serverProperties, msgInfo);
  }

  /**
   * Kept for backwards compatibility
   * @deprecated
   */
  public static void send(Properties serverProperties, String sender, List recipients, List bccRecipients, String subject, String text) {
    send(serverProperties,sender,(Collection) recipients, (Collection) bccRecipients, subject,text);
  }
  
  public static void send(Properties serverProperties, String sender, Collection recipients, Collection bccRecipients, String subject, String text) {
    MessageInfo msgInfo = new MessageInfo(null, sender, recipients, null, bccRecipients, subject, text);
    send(serverProperties, msgInfo); 
  } 
  
  private static void send(Properties serverProperties, MessageInfo msgInfo) { 
    if (nullOrEmpty(msgInfo.recipients) && nullOrEmpty(msgInfo.bccRecipients)) return;

    if (log.isDebugEnabled()) {
      StringBuffer detail = new StringBuffer("sending email to ");
      detail.append(msgInfo.recipients);
      if (msgInfo.bccRecipients != null) detail.append(" bcc ").append(msgInfo.bccRecipients);
      if (msgInfo.subject != null) detail.append(" about '").append(msgInfo.subject).append('\'');
      log.debug(detail.toString());
    }

    msgInfo.session = getSession(serverProperties);
    
    for (int retries = 4; retries >= 0; retries--) {
      try {
        sendInternal(msgInfo);
        break;
      }
      catch (MessagingException me) {
        if (retries == 0) throw new JbpmException("failed to send email", me);
        log.warn("failed to send email (" + retries + " retries left): " + me.getMessage());
      }
    }
  }

  private static boolean nullOrEmpty(Collection col) {
    return col == null || col.isEmpty();
  }

  private static void sendInternal(MessageInfo msgInfo) throws MessagingException {
    
    Message message = fillMessage(msgInfo);
    
    // send the message
    Transport.send(message);
  }

  private static Message fillMessage(MessageInfo msgInfo) throws MessagingException { 
    MimeMessage message = new MimeMessage(msgInfo.session);
    // from
    if (msgInfo.sender != null) {
      message.setFrom(new InternetAddress(msgInfo.sender));
    }
    else {
      // read sender from session property "mail.from"
      message.setFrom();
    }
    // to
    if (msgInfo.recipients != null) {
      for (Iterator iter = msgInfo.recipients.iterator(); iter.hasNext();) {
        InternetAddress recipient = new InternetAddress((String) iter.next());
        message.addRecipient(Message.RecipientType.TO, recipient);
      }
    }
    // cc
    if (msgInfo.ccRecipients != null) {
      for (Iterator iter = msgInfo.ccRecipients.iterator(); iter.hasNext();) {
        InternetAddress recipient = new InternetAddress((String) iter.next());
        message.addRecipient(Message.RecipientType.CC, recipient);
      }
    }
    // bcc
    if (msgInfo.bccRecipients != null) {
      for (Iterator iter = msgInfo.bccRecipients.iterator(); iter.hasNext();) {
        InternetAddress recipient = new InternetAddress((String) iter.next());
        message.addRecipient(Message.RecipientType.BCC, recipient);
      }
    }
    // subject
    if (msgInfo.subject != null) message.setSubject(msgInfo.subject);
    // text
    if (msgInfo.text != null) message.setText(msgInfo.text);
    
    return message;
  }
  
  private static Session getSession(Properties properties) {
    String userName = (String) properties.remove(JBPM_MAIL_USER);
    String password = (String) properties.remove(JBPM_MAIL_PASSWORD);

    Session session;
    if( userName != null ) { 
        properties.setProperty("mail.smtp.submitter", userName );
        Authenticator authenticator = new Authenticator(userName, password);
        session = Session.getInstance(properties, authenticator);
        if( password != null ) { 
          properties.setProperty("mail.smtp.auth", "true");
        }
    }
    else { 
      properties.remove("mail.smtp.auth");
      properties.remove("mail.smtp.starttls.enable");
      session = Session.getInstance(properties);
    }
   
    // set debug prop if available
    String debugStr = (String) properties.remove(JBPM_MAIL_DEBUG);
    boolean debug = false;
    if( debugStr != null ) { 
      debug = Boolean.getBoolean(debugStr);
    }
    session.setDebug(debug);

    return session;
  }

  private static class Authenticator extends javax.mail.Authenticator implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient PasswordAuthentication authentication;

    public Authenticator(String username, String password) {
      authentication = new PasswordAuthentication(username, password);
    }

    protected PasswordAuthentication getPasswordAuthentication() {
      return authentication;
    }
  }
  
  private static final Map serverPropertiesByResource = new HashMap();

  private static final String JBPM_MAIL_USER = "jbpm.mail.user";
  private static final String JBPM_MAIL_PASSWORD = "jbpm.mail.password";
  private static final String JBPM_MAIL_DEBUG = "jbpm.mail.debug";
  
  private Properties getServerProperties() {
    Properties serverProperties;

    if (Configs.hasObject("resource.mail.properties")) {
      String resource = Configs.getString("resource.mail.properties");
      synchronized (serverPropertiesByResource) {
        // look in server properties cache
        serverProperties = (Properties) serverPropertiesByResource.get(resource);
        if (serverProperties == null) {
          // load server properties and put them in the cache
          serverProperties = ClassLoaderUtil.getProperties(resource);
          serverPropertiesByResource.put(resource, serverProperties);
        }
      }
    }
    else {
      serverProperties = new Properties();
      // host
      String jbpmProperty = "jbpm.mail.smtp.host";
      if (Configs.hasObject(jbpmProperty)) {
        String smtpHost = Configs.getString(jbpmProperty);
        serverProperties.setProperty("mail.smtp.host", smtpHost);
      }
      // port
      jbpmProperty = "jbpm.mail.smtp.port";
      if (Configs.hasObject(jbpmProperty)) {
        int port = Configs.getInt(jbpmProperty);
        serverProperties.setProperty("mail.smtp.port", Integer.toString(port));
      }
      // start TLS
      jbpmProperty = "jbpm.mail.smtp.starttls";
      if (Configs.hasObject(jbpmProperty)) {
        boolean enableTLS = Configs.getBoolean(jbpmProperty);
        serverProperties.setProperty("mail.smtp.starttls.enable", Boolean.toString(enableTLS) );
      }
      // SMTP authentication
      jbpmProperty = "jbpm.mail.smtp.auth";
      if (Configs.hasObject(jbpmProperty)) {
        boolean useAuth = Configs.getBoolean(jbpmProperty);
        serverProperties.setProperty("mail.smtp.auth", Boolean.toString(useAuth) );
      }
      // user, password
      String [] propNameStrings = { JBPM_MAIL_USER, JBPM_MAIL_PASSWORD };
      for( int i = 0; i < propNameStrings.length; ++i ) { 
        if (Configs.hasObject(propNameStrings[i])) {
          String propVal = Configs.getString(propNameStrings[i]);
          serverProperties.setProperty(propNameStrings[i], propVal);
        }
      }
      // debug
      jbpmProperty = JBPM_MAIL_DEBUG;
      if (Configs.hasObject(jbpmProperty)) {
        boolean debug = Configs.getBoolean(jbpmProperty);
        serverProperties.setProperty(JBPM_MAIL_DEBUG, Boolean.toString(debug) );
      }
    }
    
    return serverProperties;
  }

  static class MailVariableResolver implements VariableResolver, Serializable {

    private Map templateVariables;
    private VariableResolver variableResolver;

    private static final long serialVersionUID = 1L;

    MailVariableResolver(Map templateVariables, VariableResolver variableResolver) {
      this.templateVariables = templateVariables;
      this.variableResolver = variableResolver;
    }

    public Object resolveVariable(String pName) throws ELException {
      if (templateVariables != null && templateVariables.containsKey(pName)) {
        return templateVariables.get(pName);
      }
      return variableResolver != null ? variableResolver.resolveVariable(pName) : null;
    }
  }

  private static class MessageInfo implements Serializable { 
    
    private static final long serialVersionUID = -4252493407344235335L;
    
    public transient Session session;
    public String sender; 
    public Collection recipients;
    public Collection ccRecipients; 
    public Collection bccRecipients; 
    public String subject; 
    public String text;
    
    public MessageInfo() { } 
    public MessageInfo(Session session, String sender, 
      Collection recipients, Collection ccRecipients, Collection bccRecipients, 
      String subject, String text) { 
      this.session = session;
      this.sender = sender;
      this.recipients = recipients;
      this.ccRecipients = ccRecipients;
      this.bccRecipients = bccRecipients;
      this.subject = subject;
      this.text = text;
    } 

  }
  
}
