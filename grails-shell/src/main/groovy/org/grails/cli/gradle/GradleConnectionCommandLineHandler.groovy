package org.grails.cli.gradle

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ModelBuilder
import org.gradle.tooling.ProgressEvent
import org.gradle.tooling.ProgressListener
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.GradleTask
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProjectContext

class GradleConnectionCommandLineHandler implements CommandLineHandler {
    ProjectConnection projectConnection

    public void initialize(File baseDir) {
        SystemOutErrCapturer.doWithCapturer {
            projectConnection = GradleConnector.newConnector().forProjectDirectory(baseDir).connect()
        }
    }

    @Override
    public boolean handleCommand(ExecutionContext context) {
        if(projectConnection==null) {
            initialize(context.baseDir)
        }
        return false;
    }

    @Override
    public List<CommandDescription> listCommands(ProjectContext context) {
        def sysout = System.out
        if(projectConnection==null) {
            initialize(context.getBaseDir())
        }
        SystemOutErrCapturer.doWithCapturer { 
            ModelBuilder<GradleProject> builder = projectConnection.model(GradleProject.class);
            //builder.withArguments("--quiet")
            builder.addProgressListener({ ProgressEvent event ->  
                //sysout.println "event: $event.description"
            } as ProgressListener)
            GradleProject project = builder.get();
            project.getTasks().collect { GradleTask task ->
                new CommandDescription(task.name, task.description, null)
            }
        }
    }
}
