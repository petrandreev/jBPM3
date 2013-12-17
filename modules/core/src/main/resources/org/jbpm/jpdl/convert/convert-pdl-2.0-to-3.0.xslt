<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes" />	
	<xsl:template match="/">	   
	   <xsl:comment>Converted from jPDL 2.0 to jPDL 3.0 by the jBPM Converter</xsl:comment> 
		<xsl:apply-templates/>
	</xsl:template>
	<xsl:template match="process-definition">
	  <xsl:element name="process-definition" namespace="http://jbpm.org/3/jpdl">		  
	    <xsl:copy-of select="@name"/>	
	    <xsl:apply-templates select="description"/>
	    <xsl:element name="start-state" namespace="http://jbpm.org/3/jpdl">
        <xsl:copy-of select="start-state/@name"/><xsl:copy-of select="start-state/@swimlane"/>
        <xsl:apply-templates select="start-state/description"/>
        <xsl:apply-templates select="start-state/transition"/>
      </xsl:element>    
	    <xsl:apply-templates/>
	  </xsl:element>		 			
	</xsl:template>
	<xsl:template match="start-state">
        
  </xsl:template>	
  	<xsl:template match="swimlane">	  
	  <xsl:element name="swimlane" namespace="http://jbpm.org/3/jpdl">
	    <xsl:copy-of select="@name"/>
	    <xsl:apply-templates select="description"/>
	    <xsl:if test="delegation"><xsl:call-template name="assignment"/></xsl:if>
	  </xsl:element>
	</xsl:template>
	<xsl:template name="assignment">
	    <xsl:element name="assignment" namespace="http://jbpm.org/3/jpdl">
	      <xsl:copy-of select="delegation/@class"/>
	    </xsl:element>
	</xsl:template>
	<xsl:template match="type">	  
	  <xsl:comment>WARNING:  types are not present in jPDL 3.0</xsl:comment>
  </xsl:template>  
  <xsl:template match="state">
    <xsl:choose>
        <xsl:when test="assignment">
          <xsl:element name="task-node" namespace="http://jbpm.org/3/jpdl">
            <xsl:copy-of select="@name"/>
            <xsl:attribute name="signal">last</xsl:attribute>
            <xsl:attribute name="create-tasks">true</xsl:attribute>
            <xsl:apply-templates select="description"/>
            <xsl:element name="task" namespace="http://jbpm.org/3/jpdl">
              <xsl:attribute name="swimlane"><xsl:value-of select="assignment/@swimlane"/></xsl:attribute> 
              <xsl:if test="assignment/@assignment"><xsl:comment>WARNING:  assignment = <xsl:value-of select="assignment/@assignment"/> is not supported in jPDL 3.0</xsl:comment></xsl:if>
              <xsl:if test="assignment/@authentication"><xsl:comment>WARNING:  authentication = <xsl:value-of select="assignment/@authentication"/> is not supported in jPDL 3.0</xsl:comment></xsl:if>
              <xsl:apply-templates select="action[@event-type='state-after-assignment']"/>
            </xsl:element>             
            <xsl:apply-templates select="transition"/>
            <xsl:apply-templates select="action[@event-type!='state-after-assignment']"/>
          </xsl:element>
        </xsl:when>
		<xsl:otherwise>
		  <xsl:call-template name="node"/>		     
		</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template match="milestone">
    <!--<xsl:call-template name="node"/>-->
    <xsl:comment>WARNING:  Milestone not supported yet in jPDL 3.0</xsl:comment>
  </xsl:template>
  <xsl:template match="process-state | decision | fork | join">
    <xsl:call-template name="node"/>
  </xsl:template>
  <xsl:template match="end-state">
    <xsl:element name="end-state" namespace="http://jbpm.org/3/jpdl">
      <xsl:copy-of select="@name"/>
    </xsl:element>
  </xsl:template>
  <xsl:template name="node">
    <xsl:element name="{local-name()}" namespace="http://jbpm.org/3/jpdl">
      <xsl:copy-of select="@name"/>
       <xsl:apply-templates select="description"/>
       <xsl:if test="local-name() = 'process-state'">
         <xsl:element name="sub-process" namespace="http://jbpm.org/3/jpdl">
           <xsl:attribute name="name"><xsl:value-of select="@name"/></xsl:attribute>
         </xsl:element>
         <xsl:apply-templates select="transition"/>
         <xsl:element name="event" namespace="http://jbpm.org/3/jpdl">
           <xsl:attribute name="type">subprocess-created</xsl:attribute>
           <xsl:element name="action" namespace="http://jbpm.org/3/jpdl">
             <xsl:attribute name="class"><xsl:value-of select="delegation/@class"/></xsl:attribute>
           </xsl:element>
         </xsl:element>
       </xsl:if>
       <xsl:if test="local-name() != 'process-state'">
       	<xsl:apply-templates select="transition"/>
       </xsl:if>
       <xsl:apply-templates select="action"/> 
    </xsl:element>
  </xsl:template>
  <xsl:template match="description">
	<xsl:comment><xsl:value-of select="."/></xsl:comment>
  </xsl:template>   
  <xsl:template match="transition">
    <xsl:element name="transition" namespace="http://jbpm.org/3/jpdl">
      <xsl:copy-of select="@name" /><xsl:copy-of select="@to"/>
      <xsl:apply-templates select="action"/>
    </xsl:element>
  </xsl:template>
  <xsl:template match="transition/action">
    <xsl:element name="action" namespace="http://jbpm.org/3/jpdl">
      <xsl:attribute name="class"><xsl:value-of select="delegation/@class"/></xsl:attribute>
    </xsl:element>
  </xsl:template>  
  <xsl:template match="action">
    <xsl:element name="event" namespace="http://jbpm.org/3/jpdl">
      <xsl:attribute name="type">
        <xsl:choose>
			<xsl:when test="@event-type='state-enter'">node-enter</xsl:when>
			<xsl:when test="@event-type='state-leave'">node-leave</xsl:when>
			<xsl:when test="@event-type='decision-enter'">node-enter</xsl:when>
			<xsl:when test="@event-type='decision-leave'">node-leave</xsl:when>
			<xsl:when test="@event-type='milestone-enter'">node-enter</xsl:when>
			<xsl:when test="@event-type='milestone-leave'">node-leave</xsl:when>
			<xsl:when test="@event-type='fork-enter'">node-enter</xsl:when>
			<xsl:when test="@event-type='fork-every-leave'">node-leave</xsl:when>
			<xsl:when test="@event-type='join-every-enter'">node-enter</xsl:when>
			<xsl:when test="@event-type='join-leave'">node-leave</xsl:when>
			<xsl:when test="@event-type='process-state-enter'">subprocess-created</xsl:when>
			<xsl:when test="@event-type='process-state-leave'">subprocess-end</xsl:when>
			<xsl:when test="@event-type='state-after-assignment'">task-assign</xsl:when>
			<xsl:otherwise><xsl:value-of select="@event-type"/></xsl:otherwise>
		</xsl:choose>      
      </xsl:attribute>
      <xsl:element name="action" namespace="http://jbpm.org/3/jpdl">
        <xsl:attribute name="class"><xsl:value-of select="delegation/@class"/></xsl:attribute>
      </xsl:element>
    </xsl:element>
  </xsl:template>
  
</xsl:stylesheet>
