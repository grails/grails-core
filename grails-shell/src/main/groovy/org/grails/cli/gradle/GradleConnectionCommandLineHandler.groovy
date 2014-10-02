package org.grails.cli.gradle

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.grails.cli.gradle.FetchAllTaskSelectorsBuildAction.AllTasksModel
import org.grails.cli.profile.CommandDescription
import org.grails.cli.profile.CommandLineHandler
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.ProjectContext

@CompileStatic
class GradleConnectionCommandLineHandler implements CommandLineHandler {
    @Override
    public boolean handleCommand(ExecutionContext context) {
        if(context.commandLine.commandName == 'gradle') {
            
        }
        return false;
    }

    @CompileDynamic
    @Override
    public List<CommandDescription> listCommands(ProjectContext context) {
        [new CommandDescription("gradle", "run gradle build", "usage: gradle [task]")]
    }
    
    private static final Closure<String> taskNameFormatter = { String projectPath, String taskName ->
        if(projectPath == ':') {
            ":$taskName".toString()
        } else {
            "$projectPath:$taskName".toString()
        }
    }
    
    public Set<String> listAllTaskSelectors(ProjectContext context) {
        AllTasksModel allTasksModel = ((AllTasksModel)GradleUtil.withProjectConnection(context.getBaseDir()) { ProjectConnection projectConnection ->
            (AllTasksModel)projectConnection.action(new FetchAllTaskSelectorsBuildAction(context.getBaseDir())).run()
        })
        Set<String> allTaskSelectors=[] as Set
        
        if (allTasksModel.currentProject) {
            allTaskSelectors.addAll(allTasksModel.allTaskSelectors.get(allTasksModel.currentProject))
        }
        
        allTasksModel.projectPaths.each { String projectName, String projectPath ->
            allTasksModel.allTasks.get(projectName).each { String taskName ->
                allTaskSelectors.add(taskNameFormatter(projectPath, taskName))
            }
        }
        
        allTaskSelectors
    }
}
