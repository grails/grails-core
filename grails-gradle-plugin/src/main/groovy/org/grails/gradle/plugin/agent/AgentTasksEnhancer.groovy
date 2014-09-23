package org.grails.gradle.plugin.agent

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
        def agentJars = project.getConfigurations().getByName("agent").resolvedConfiguration.resolvedArtifacts

        if(agentJars) {
            def agentJar = agentJars.iterator().next().file
            for (Task task : project.getTasks()) {
                if (task instanceof JavaExec) {
                    addAgent(project, (JavaExec) task, agentJar);
                }
            }
        }
    }

    private void addAgent(Project project, JavaExec exec, File agent) {
        exec.jvmArgs "-javaagent:$agent.absolutePath"
        exec.jvmArgs "-noverify"
    }
}
