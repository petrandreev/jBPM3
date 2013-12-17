new ChangeProcessInstanceVersionCommand()
  .processName("commute")
  .nodeNameMappingAdd("drive to destination", "ride bike to destination")
  .execute(jbpmContext);