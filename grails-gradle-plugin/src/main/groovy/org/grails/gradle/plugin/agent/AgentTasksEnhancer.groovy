package org.grails.gradle.plugin.agent

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec

/**
 * Sets up the reloading agent based on the agent configuration after the project has been configured
 *
 * @author Graeme Rocher
 */
class AgentTasksEnhancer implements Action<Project> {
    @Override
    void execute(Project project) {
        try {
            def agentJars = project.getConfigurations().getByName("agent").resolvedConfiguration.resolvedArtifacts

            if(agentJars) {
                def agentJar = agentJars.iterator().next().file
                for (Task task : project.getTasks()) {
                    if (task instanceof JavaExec) {
                        addAgent(project, (JavaExec) task, agentJar);
                    }
                }
            }
        } catch (Throwable e) {
            project.logger.warn("Cannot resolve reloading agent JAR: ${e.message}")
        }
    }

    private void addAgent(Project project, JavaExec exec, File agent) {
        exec.jvmArgs "-javaagent:$agent.absolutePath"
        exec.jvmArgs "-Xverify:none"
        exec.jvmArgs "-Dspringloaded=inclusions=grails.plugins..*"
    }
}
