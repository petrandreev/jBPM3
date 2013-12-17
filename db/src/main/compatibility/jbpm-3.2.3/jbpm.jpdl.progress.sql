alter table JBPM_ACTION drop constraint FK_ACTION_REFACT
alter table JBPM_ACTION drop constraint FK_CRTETIMERACT_TA
alter table JBPM_ACTION drop constraint FK_ACTION_PROCDEF
alter table JBPM_ACTION drop constraint FK_ACTION_EVENT
alter table JBPM_ACTION drop constraint FK_ACTION_ACTNDEL
alter table JBPM_ACTION drop constraint FK_ACTION_EXPTHDL
alter table JBPM_BYTEARRAY drop constraint FK_BYTEARR_FILDEF
alter table JBPM_BYTEBLOCK drop constraint FK_BYTEBLOCK_FILE
alter table JBPM_COMMENT drop constraint FK_COMMENT_TOKEN
alter table JBPM_COMMENT drop constraint FK_COMMENT_TSK
alter table JBPM_DECISIONCONDITIONS drop constraint FK_DECCOND_DEC
alter table JBPM_DELEGATION drop constraint FK_DELEGATION_PRCD
alter table JBPM_EVENT drop constraint FK_EVENT_PROCDEF
alter table JBPM_EVENT drop constraint FK_EVENT_TRANS
alter table JBPM_EVENT drop constraint FK_EVENT_NODE
alter table JBPM_EVENT drop constraint FK_EVENT_TASK
alter table JBPM_ID_GROUP drop constraint FK_ID_GRP_PARENT
alter table JBPM_ID_MEMBERSHIP drop constraint FK_ID_MEMSHIP_GRP
alter table JBPM_ID_MEMBERSHIP drop constraint FK_ID_MEMSHIP_USR
alter table JBPM_JOB drop constraint FK_JOB_PRINST
alter table JBPM_JOB drop constraint FK_JOB_ACTION
alter table JBPM_JOB drop constraint FK_JOB_TOKEN
alter table JBPM_JOB drop constraint FK_JOB_NODE
alter table JBPM_JOB drop constraint FK_JOB_TSKINST
alter table JBPM_LOG drop constraint FK_LOG_SOURCENODE
alter table JBPM_LOG drop constraint FK_LOG_DESTNODE
alter table JBPM_LOG drop constraint FK_LOG_TOKEN
alter table JBPM_LOG drop constraint FK_LOG_TRANSITION
alter table JBPM_LOG drop constraint FK_LOG_TASKINST
alter table JBPM_LOG drop constraint FK_LOG_CHILDTOKEN
alter table JBPM_LOG drop constraint FK_LOG_OLDBYTES
alter table JBPM_LOG drop constraint FK_LOG_SWIMINST
alter table JBPM_LOG drop constraint FK_LOG_NEWBYTES
alter table JBPM_LOG drop constraint FK_LOG_ACTION
alter table JBPM_LOG drop constraint FK_LOG_VARINST
alter table JBPM_LOG drop constraint FK_LOG_NODE
alter table JBPM_LOG drop constraint FK_LOG_PARENT
alter table JBPM_MODULEDEFINITION drop constraint FK_MODDEF_PROCDEF
alter table JBPM_MODULEDEFINITION drop constraint FK_TSKDEF_START
alter table JBPM_MODULEINSTANCE drop constraint FK_MODINST_PRCINST
alter table JBPM_MODULEINSTANCE drop constraint FK_TASKMGTINST_TMD
alter table JBPM_NODE drop constraint FK_DECISION_DELEG
alter table JBPM_NODE drop constraint FK_NODE_PROCDEF
alter table JBPM_NODE drop constraint FK_NODE_ACTION
alter table JBPM_NODE drop constraint FK_PROCST_SBPRCDEF
alter table JBPM_NODE drop constraint FK_NODE_SCRIPT
alter table JBPM_NODE drop constraint FK_NODE_SUPERSTATE
alter table JBPM_POOLEDACTOR drop constraint FK_POOLEDACTOR_SLI
alter table JBPM_PROCESSDEFINITION drop constraint FK_PROCDEF_STRTSTA
alter table JBPM_PROCESSINSTANCE drop constraint FK_PROCIN_PROCDEF
alter table JBPM_PROCESSINSTANCE drop constraint FK_PROCIN_ROOTTKN
alter table JBPM_PROCESSINSTANCE drop constraint FK_PROCIN_SPROCTKN
alter table JBPM_RUNTIMEACTION drop constraint FK_RTACTN_PROCINST
alter table JBPM_RUNTIMEACTION drop constraint FK_RTACTN_ACTION
alter table JBPM_SWIMLANE drop constraint FK_SWL_ASSDEL
alter table JBPM_SWIMLANE drop constraint FK_SWL_TSKMGMTDEF
alter table JBPM_SWIMLANEINSTANCE drop constraint FK_SWIMLANEINST_TM
alter table JBPM_SWIMLANEINSTANCE drop constraint FK_SWIMLANEINST_SL
alter table JBPM_TASK drop constraint FK_TASK_STARTST
alter table JBPM_TASK drop constraint FK_TASK_PROCDEF
alter table JBPM_TASK drop constraint FK_TASK_ASSDEL
alter table JBPM_TASK drop constraint FK_TASK_SWIMLANE
alter table JBPM_TASK drop constraint FK_TASK_TASKNODE
alter table JBPM_TASK drop constraint FK_TASK_TASKMGTDEF
alter table JBPM_TASK drop constraint FK_TSK_TSKCTRL
alter table JBPM_TASKACTORPOOL drop constraint FK_TASKACTPL_TSKI
alter table JBPM_TASKACTORPOOL drop constraint FK_TSKACTPOL_PLACT
alter table JBPM_TASKCONTROLLER drop constraint FK_TSKCTRL_DELEG
alter table JBPM_TASKINSTANCE drop constraint FK_TSKINS_PRCINS
alter table JBPM_TASKINSTANCE drop constraint FK_TASKINST_TMINST
alter table JBPM_TASKINSTANCE drop constraint FK_TASKINST_TOKEN
alter table JBPM_TASKINSTANCE drop constraint FK_TASKINST_SLINST
alter table JBPM_TASKINSTANCE drop constraint FK_TASKINST_TASK
alter table JBPM_TOKEN drop constraint FK_TOKEN_SUBPI
alter table JBPM_TOKEN drop constraint FK_TOKEN_PROCINST
alter table JBPM_TOKEN drop constraint FK_TOKEN_NODE
alter table JBPM_TOKEN drop constraint FK_TOKEN_PARENT
alter table JBPM_TOKENVARIABLEMAP drop constraint FK_TKVARMAP_TOKEN
alter table JBPM_TOKENVARIABLEMAP drop constraint FK_TKVARMAP_CTXT
alter table JBPM_TRANSITION drop constraint FK_TRANSITION_FROM
alter table JBPM_TRANSITION drop constraint FK_TRANS_PROCDEF
alter table JBPM_TRANSITION drop constraint FK_TRANSITION_TO
alter table JBPM_VARIABLEACCESS drop constraint FK_VARACC_PROCST
alter table JBPM_VARIABLEACCESS drop constraint FK_VARACC_SCRIPT
alter table JBPM_VARIABLEACCESS drop constraint FK_VARACC_TSKCTRL
alter table JBPM_VARIABLEINSTANCE drop constraint FK_VARINST_PRCINST
alter table JBPM_VARIABLEINSTANCE drop constraint FK_VARINST_TKVARMP
alter table JBPM_VARIABLEINSTANCE drop constraint FK_VARINST_TK
alter table JBPM_VARIABLEINSTANCE drop constraint FK_BYTEINST_ARRAY
alter table JBPM_VARIABLEINSTANCE drop constraint FK_VAR_TSKINST
drop table JBPM_ACTION
drop table JBPM_BYTEARRAY
drop table JBPM_BYTEBLOCK
drop table JBPM_COMMENT
drop table JBPM_DECISIONCONDITIONS
drop table JBPM_DELEGATION
drop table JBPM_EVENT
drop table JBPM_EXCEPTIONHANDLER
drop table JBPM_ID_GROUP
drop table JBPM_ID_MEMBERSHIP
drop table JBPM_ID_PERMISSIONS
drop table JBPM_ID_USER
drop table JBPM_JOB
drop table JBPM_LOG
drop table JBPM_MODULEDEFINITION
drop table JBPM_MODULEINSTANCE
drop table JBPM_NODE
drop table JBPM_POOLEDACTOR
drop table JBPM_PROCESSDEFINITION
drop table JBPM_PROCESSINSTANCE
drop table JBPM_RUNTIMEACTION
drop table JBPM_SWIMLANE
drop table JBPM_SWIMLANEINSTANCE
drop table JBPM_TASK
drop table JBPM_TASKACTORPOOL
drop table JBPM_TASKCONTROLLER
drop table JBPM_TASKINSTANCE
drop table JBPM_TOKEN
drop table JBPM_TOKENVARIABLEMAP
drop table JBPM_TRANSITION
drop table JBPM_VARIABLEACCESS
drop table JBPM_VARIABLEINSTANCE
drop table hibernate_unique_key
create table JBPM_ACTION (ID_ numeric not null, class character(1) not null, NAME_ varchar(255), ISPROPAGATIONALLOWED_ bit, ACTIONEXPRESSION_ varchar(255), ISASYNC_ bit, REFERENCEDACTION_ numeric, ACTIONDELEGATION_ numeric, EVENT_ numeric, PROCESSDEFINITION_ numeric, EXPRESSION_ varchar(4000), TIMERNAME_ varchar(255), DUEDATE_ varchar(255), REPEAT_ varchar(255), TRANSITIONNAME_ varchar(255), TIMERACTION_ numeric, EVENTINDEX_ integer, EXCEPTIONHANDLER_ numeric, EXCEPTIONHANDLERINDEX_ integer, primary key (ID_))
create table JBPM_BYTEARRAY (ID_ numeric not null, NAME_ varchar(255), FILEDEFINITION_ numeric, primary key (ID_))
create table JBPM_BYTEBLOCK (PROCESSFILE_ numeric not null, BYTES_ varbinary(1024), INDEX_ integer not null, primary key (PROCESSFILE_, INDEX_))
create table JBPM_COMMENT (ID_ numeric not null, VERSION_ integer not null, ACTORID_ varchar(255), TIME_ timestamp, MESSAGE_ varchar(4000), TOKEN_ numeric, TASKINSTANCE_ numeric, TOKENINDEX_ integer, TASKINSTANCEINDEX_ integer, primary key (ID_))
create table JBPM_DECISIONCONDITIONS (DECISION_ numeric not null, TRANSITIONNAME_ varchar(255), EXPRESSION_ varchar(255), INDEX_ integer not null, primary key (DECISION_, INDEX_))
create table JBPM_DELEGATION (ID_ numeric not null, CLASSNAME_ varchar(4000), CONFIGURATION_ varchar(4000), CONFIGTYPE_ varchar(255), PROCESSDEFINITION_ numeric, primary key (ID_))
create table JBPM_EVENT (ID_ numeric not null, EVENTTYPE_ varchar(255), TYPE_ character(1), GRAPHELEMENT_ numeric, PROCESSDEFINITION_ numeric, NODE_ numeric, TRANSITION_ numeric, TASK_ numeric, primary key (ID_))
create table JBPM_EXCEPTIONHANDLER (ID_ numeric not null, EXCEPTIONCLASSNAME_ varchar(4000), TYPE_ character(1), GRAPHELEMENT_ numeric, PROCESSDEFINITION_ numeric, GRAPHELEMENTINDEX_ integer, NODE_ numeric, TRANSITION_ numeric, TASK_ numeric, primary key (ID_))
create table JBPM_ID_GROUP (ID_ numeric not null, CLASS_ character(1) not null, NAME_ varchar(255), TYPE_ varchar(255), PARENT_ numeric, primary key (ID_))
create table JBPM_ID_MEMBERSHIP (ID_ numeric not null, CLASS_ character(1) not null, NAME_ varchar(255), ROLE_ varchar(255), USER_ numeric, GROUP_ numeric, primary key (ID_))
create table JBPM_ID_PERMISSIONS (ENTITY_ numeric not null, CLASS_ varchar(255), NAME_ varchar(255), ACTION_ varchar(255))
create table JBPM_ID_USER (ID_ numeric not null, CLASS_ character(1) not null, NAME_ varchar(255), EMAIL_ varchar(255), PASSWORD_ varchar(255), primary key (ID_))
create table JBPM_JOB (ID_ numeric not null, CLASS_ character(1) not null, VERSION_ integer not null, DUEDATE_ timestamp, PROCESSINSTANCE_ numeric, TOKEN_ numeric, TASKINSTANCE_ numeric, ISSUSPENDED_ bit, ISEXCLUSIVE_ bit, LOCKOWNER_ varchar(255), LOCKTIME_ timestamp, EXCEPTION_ varchar(4000), RETRIES_ integer, NAME_ varchar(255), REPEAT_ varchar(255), TRANSITIONNAME_ varchar(255), ACTION_ numeric, GRAPHELEMENTTYPE_ varchar(255), GRAPHELEMENT_ numeric, NODE_ numeric, primary key (ID_))
create table JBPM_LOG (ID_ numeric not null, CLASS_ character(1) not null, INDEX_ integer, DATE_ timestamp, TOKEN_ numeric, PARENT_ numeric, MESSAGE_ varchar(4000), EXCEPTION_ varchar(4000), ACTION_ numeric, NODE_ numeric, ENTER_ timestamp, LEAVE_ timestamp, DURATION_ numeric, NEWLONGVALUE_ numeric, TRANSITION_ numeric, CHILD_ numeric, SOURCENODE_ numeric, DESTINATIONNODE_ numeric, VARIABLEINSTANCE_ numeric, OLDBYTEARRAY_ numeric, NEWBYTEARRAY_ numeric, OLDDATEVALUE_ timestamp, NEWDATEVALUE_ timestamp, OLDDOUBLEVALUE_ double precision, NEWDOUBLEVALUE_ double precision, OLDLONGIDCLASS_ varchar(255), OLDLONGIDVALUE_ numeric, NEWLONGIDCLASS_ varchar(255), NEWLONGIDVALUE_ numeric, OLDSTRINGIDCLASS_ varchar(255), OLDSTRINGIDVALUE_ varchar(255), NEWSTRINGIDCLASS_ varchar(255), NEWSTRINGIDVALUE_ varchar(255), OLDLONGVALUE_ numeric, OLDSTRINGVALUE_ varchar(4000), NEWSTRINGVALUE_ varchar(4000), TASKINSTANCE_ numeric, TASKACTORID_ varchar(255), TASKOLDACTORID_ varchar(255), SWIMLANEINSTANCE_ numeric, primary key (ID_))
create table JBPM_MODULEDEFINITION (ID_ numeric not null, CLASS_ character(1) not null, NAME_ varchar(4000), PROCESSDEFINITION_ numeric, STARTTASK_ numeric, primary key (ID_))
create table JBPM_MODULEINSTANCE (ID_ numeric not null, CLASS_ character(1) not null, VERSION_ integer not null, PROCESSINSTANCE_ numeric, TASKMGMTDEFINITION_ numeric, NAME_ varchar(255), primary key (ID_))
create table JBPM_NODE (ID_ numeric not null, CLASS_ character(1) not null, NAME_ varchar(255), DESCRIPTION_ varchar(4000), PROCESSDEFINITION_ numeric, ISASYNC_ bit, ISASYNCEXCL_ bit, ACTION_ numeric, SUPERSTATE_ numeric, SUBPROCNAME_ varchar(255), SUBPROCESSDEFINITION_ numeric, DECISIONEXPRESSION_ varchar(255), DECISIONDELEGATION numeric, SCRIPT_ numeric, SIGNAL_ integer, CREATETASKS_ bit, ENDTASKS_ bit, NODECOLLECTIONINDEX_ integer, primary key (ID_))
create table JBPM_POOLEDACTOR (ID_ numeric not null, VERSION_ integer not null, ACTORID_ varchar(255), SWIMLANEINSTANCE_ numeric, primary key (ID_))
create table JBPM_PROCESSDEFINITION (ID_ numeric not null, CLASS_ character(1) not null, NAME_ varchar(255), DESCRIPTION_ varchar(4000), VERSION_ integer, ISTERMINATIONIMPLICIT_ bit, STARTSTATE_ numeric, primary key (ID_))
create table JBPM_PROCESSINSTANCE (ID_ numeric not null, VERSION_ integer not null, KEY_ varchar(255), START_ timestamp, END_ timestamp, ISSUSPENDED_ bit, PROCESSDEFINITION_ numeric, ROOTTOKEN_ numeric, SUPERPROCESSTOKEN_ numeric, primary key (ID_))
create table JBPM_RUNTIMEACTION (ID_ numeric not null, VERSION_ integer not null, EVENTTYPE_ varchar(255), TYPE_ character(1), GRAPHELEMENT_ numeric, PROCESSINSTANCE_ numeric, ACTION_ numeric, PROCESSINSTANCEINDEX_ integer, primary key (ID_))
create table JBPM_SWIMLANE (ID_ numeric not null, NAME_ varchar(255), ACTORIDEXPRESSION_ varchar(255), POOLEDACTORSEXPRESSION_ varchar(255), ASSIGNMENTDELEGATION_ numeric, TASKMGMTDEFINITION_ numeric, primary key (ID_))
create table JBPM_SWIMLANEINSTANCE (ID_ numeric not null, VERSION_ integer not null, NAME_ varchar(255), ACTORID_ varchar(255), SWIMLANE_ numeric, TASKMGMTINSTANCE_ numeric, primary key (ID_))
create table JBPM_TASK (ID_ numeric not null, NAME_ varchar(255), DESCRIPTION_ varchar(4000), PROCESSDEFINITION_ numeric, ISBLOCKING_ bit, ISSIGNALLING_ bit, CONDITION_ varchar(255), DUEDATE_ varchar(255), PRIORITY_ integer, ACTORIDEXPRESSION_ varchar(255), POOLEDACTORSEXPRESSION_ varchar(255), TASKMGMTDEFINITION_ numeric, TASKNODE_ numeric, STARTSTATE_ numeric, ASSIGNMENTDELEGATION_ numeric, SWIMLANE_ numeric, TASKCONTROLLER_ numeric, primary key (ID_))
create table JBPM_TASKACTORPOOL (TASKINSTANCE_ numeric not null, POOLEDACTOR_ numeric not null, primary key (TASKINSTANCE_, POOLEDACTOR_))
create table JBPM_TASKCONTROLLER (ID_ numeric not null, TASKCONTROLLERDELEGATION_ numeric, primary key (ID_))
create table JBPM_TASKINSTANCE (ID_ numeric not null, CLASS_ character(1) not null, VERSION_ integer not null, NAME_ varchar(255), DESCRIPTION_ varchar(4000), ACTORID_ varchar(255), CREATE_ timestamp, START_ timestamp, END_ timestamp, DUEDATE_ timestamp, PRIORITY_ integer, ISCANCELLED_ bit, ISSUSPENDED_ bit, ISOPEN_ bit, ISSIGNALLING_ bit, ISBLOCKING_ bit, TASK_ numeric, TOKEN_ numeric, PROCINST_ numeric, SWIMLANINSTANCE_ numeric, TASKMGMTINSTANCE_ numeric, primary key (ID_))
create table JBPM_TOKEN (ID_ numeric not null, VERSION_ integer not null, NAME_ varchar(255), START_ timestamp, END_ timestamp, NODEENTER_ timestamp, NEXTLOGINDEX_ integer, ISABLETOREACTIVATEPARENT_ bit, ISTERMINATIONIMPLICIT_ bit, ISSUSPENDED_ bit, LOCK_ varchar(255), NODE_ numeric, PROCESSINSTANCE_ numeric, PARENT_ numeric, SUBPROCESSINSTANCE_ numeric, primary key (ID_))
create table JBPM_TOKENVARIABLEMAP (ID_ numeric not null, VERSION_ integer not null, TOKEN_ numeric, CONTEXTINSTANCE_ numeric, primary key (ID_))
create table JBPM_TRANSITION (ID_ numeric not null, NAME_ varchar(255), DESCRIPTION_ varchar(4000), PROCESSDEFINITION_ numeric, FROM_ numeric, TO_ numeric, CONDITION_ varchar(255), FROMINDEX_ integer, primary key (ID_))
create table JBPM_VARIABLEACCESS (ID_ numeric not null, VARIABLENAME_ varchar(255), ACCESS_ varchar(255), MAPPEDNAME_ varchar(255), SCRIPT_ numeric, PROCESSSTATE_ numeric, TASKCONTROLLER_ numeric, INDEX_ integer, primary key (ID_))
create table JBPM_VARIABLEINSTANCE (ID_ numeric not null, CLASS_ character(1) not null, VERSION_ integer not null, NAME_ varchar(255), CONVERTER_ character(1), TOKEN_ numeric, TOKENVARIABLEMAP_ numeric, PROCESSINSTANCE_ numeric, BYTEARRAYVALUE_ numeric, DATEVALUE_ timestamp, DOUBLEVALUE_ double precision, LONGIDCLASS_ varchar(255), LONGVALUE_ numeric, STRINGIDCLASS_ varchar(255), STRINGVALUE_ varchar(4000), TASKINSTANCE_ numeric, primary key (ID_))
create index IDX_ACTION_ACTNDL on JBPM_ACTION (ACTIONDELEGATION_)
create index IDX_ACTION_PROCDF on JBPM_ACTION (PROCESSDEFINITION_)
create index IDX_ACTION_EVENT on JBPM_ACTION (EVENT_)
create index IDX_COMMENT_TSK on JBPM_COMMENT (TASKINSTANCE_)
create index IDX_COMMENT_TOKEN on JBPM_COMMENT (TOKEN_)
create index IDX_DELEG_PRCD on JBPM_DELEGATION (PROCESSDEFINITION_)
create index IDX_JOB_TSKINST on JBPM_JOB (TASKINSTANCE_)
create index IDX_JOB_TOKEN on JBPM_JOB (TOKEN_)
create index IDX_JOB_PRINST on JBPM_JOB (PROCESSINSTANCE_)
create index IDX_MODDEF_PROCDF on JBPM_MODULEDEFINITION (PROCESSDEFINITION_)
create index IDX_MODINST_PRINST on JBPM_MODULEINSTANCE (PROCESSINSTANCE_)
create index IDX_PSTATE_SBPRCDEF on JBPM_NODE (SUBPROCESSDEFINITION_)
create index IDX_NODE_PROCDEF on JBPM_NODE (PROCESSDEFINITION_)
create index IDX_NODE_ACTION on JBPM_NODE (ACTION_)
create index IDX_NODE_SUPRSTATE on JBPM_NODE (SUPERSTATE_)
create index IDX_TSKINST_SWLANE on JBPM_POOLEDACTOR (SWIMLANEINSTANCE_)
create index IDX_PLDACTR_ACTID on JBPM_POOLEDACTOR (ACTORID_)
create index IDX_PROCDEF_STRTST on JBPM_PROCESSDEFINITION (STARTSTATE_)
create index IDX_PROCIN_SPROCTK on JBPM_PROCESSINSTANCE (SUPERPROCESSTOKEN_)
create index IDX_PROCIN_ROOTTK on JBPM_PROCESSINSTANCE (ROOTTOKEN_)
create index IDX_PROCIN_PROCDEF on JBPM_PROCESSINSTANCE (PROCESSDEFINITION_)
create index IDX_PROCIN_KEY on JBPM_PROCESSINSTANCE (KEY_)
create index IDX_RTACTN_ACTION on JBPM_RUNTIMEACTION (ACTION_)
create index IDX_RTACTN_PRCINST on JBPM_RUNTIMEACTION (PROCESSINSTANCE_)
create index IDX_SWIMLINST_SL on JBPM_SWIMLANEINSTANCE (SWIMLANE_)
create index IDX_TASK_PROCDEF on JBPM_TASK (PROCESSDEFINITION_)
create index IDX_TASK_TSKNODE on JBPM_TASK (TASKNODE_)
create index IDX_TASK_TASKMGTDF on JBPM_TASK (TASKMGMTDEFINITION_)
create index IDX_TSKINST_TMINST on JBPM_TASKINSTANCE (TASKMGMTINSTANCE_)
create index IDX_TSKINST_SLINST on JBPM_TASKINSTANCE (SWIMLANINSTANCE_)
create index IDX_TASKINST_TOKN on JBPM_TASKINSTANCE (TOKEN_)
create index IDX_TASK_ACTORID on JBPM_TASKINSTANCE (ACTORID_)
create index IDX_TASKINST_TSK on JBPM_TASKINSTANCE (TASK_, PROCINST_)
create index IDX_TOKEN_PARENT on JBPM_TOKEN (PARENT_)
create index IDX_TOKEN_PROCIN on JBPM_TOKEN (PROCESSINSTANCE_)
create index IDX_TOKEN_NODE on JBPM_TOKEN (NODE_)
create index IDX_TOKEN_SUBPI on JBPM_TOKEN (SUBPROCESSINSTANCE_)
create index IDX_TKVVARMP_TOKEN on JBPM_TOKENVARIABLEMAP (TOKEN_)
create index IDX_TKVARMAP_CTXT on JBPM_TOKENVARIABLEMAP (CONTEXTINSTANCE_)
create index IDX_TRANS_PROCDEF on JBPM_TRANSITION (PROCESSDEFINITION_)
create index IDX_TRANSIT_FROM on JBPM_TRANSITION (FROM_)
create index IDX_TRANSIT_TO on JBPM_TRANSITION (TO_)
create index IDX_VARINST_TK on JBPM_VARIABLEINSTANCE (TOKEN_)
create index IDX_VARINST_TKVARMP on JBPM_VARIABLEINSTANCE (TOKENVARIABLEMAP_)
create index IDX_VARINST_PRCINS on JBPM_VARIABLEINSTANCE (PROCESSINSTANCE_)
create table hibernate_unique_key ( next_hi integer )
insert into hibernate_unique_key values ( 0 )
