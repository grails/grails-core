package org.grails.gradle.plugin.agent

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.JavaExec
import org.grails.gradle.plugin.core.GrailsExtension

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

        GrailsExtension.Agent agentConfig = project.extensions.findByType(GrailsExtension)?.agent ?: new GrailsExtension.Agent()
        if(agentConfig.enabled) {
            exec.jvmArgs "-javaagent:${agentConfig.path?.absolutePath ?: agent.absolutePath}"

            for(arg in agentConfig.jvmArgs) {
                exec.jvmArgs arg
            }
            for(entry in agentConfig.systemProperties) {
                exec.systemProperty(entry.key, entry.value)
            }

            Map<String, String> agentArgs= [
                    inclusions: agentConfig.inclusions,
                    synchronize: String.valueOf( agentConfig.synchronize ),
                    allowSplitPackages: String.valueOf( agentConfig.allowSplitPackages ),
                    cacheDir: agentConfig.cacheDir ? project.mkdir(agentConfig.cacheDir) : project.mkdir("build/springloaded")
            ]
            if(agentConfig.logging != null) {
                agentArgs.put("logging", String.valueOf(agentConfig.logging))
            }
            if(agentConfig.exclusions) {
                agentArgs.put('exclusions', agentConfig.exclusions)
            }
            exec.systemProperty('springloaded', agentArgs.collect { entry -> "$entry.key=$entry.value"}.join(';'))
        }

    }
}
