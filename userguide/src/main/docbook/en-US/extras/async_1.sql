update JBPM_JOB job
set job.version = 2
    job.lockOwner = '192.168.1.3:2'
where 
    job.version = 1