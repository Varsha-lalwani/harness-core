${failureStrategies}
<#list phases as phase>
<#if phase_index=0>
${canarySnippet
?replace("<+start>", 0)
?replace("<+end>", phase)
?replace("<+unit>", unitType)
?replace("<+phase>", (phase_index+1))
?replace("<+setup_runtime_paths_script>", setupRuntimePathsScript)
?replace("<+process_stop_script>", processStopScript)
?replace("<+port_cleared_script>", portClearedScript)
?replace("<+process_run_script>", processRunScript)
?replace("<+port_listening_script>", portListeningScript)}
<#assign prevPhase = phase>
<#else>
${canarySnippet
?replace("spec:\n  execution:\n    steps:\n", "")
?replace("<+start>", prevPhase)
?replace("<+end>", phase)
?replace("<+unit>", unitType)
?replace("<+phase>", (phase_index+1))
?replace("<+setup_runtime_paths_script>", setupRuntimePathsScript)
?replace("<+process_stop_script>", processStopScript)
?replace("<+port_cleared_script>", portClearedScript)
?replace("<+process_run_script>", processRunScript)
?replace("<+port_listening_script>", portListeningScript)}
<#assign prevPhase = phase>
</#if>
</#list>