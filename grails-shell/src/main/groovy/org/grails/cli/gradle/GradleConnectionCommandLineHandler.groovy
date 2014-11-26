package org.grails.cli.gradle

import groovy.transform.CompileStatic
import jline.console.completer.*

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.grails.cli.gradle.FetchAllTaskSelectorsBuildAction.AllTasksModel
import org.grails.cli.profile.*

@CompileStatic
class GradleConnectionCommandLineHandler implements CommandLineHandler, CompleterFactory {
    boolean backgroundInitialize = true

    @Override
    public boolean handle(ExecutionContext context) {
        if(context.commandLine.commandName == 'gradle') {
            GradleUtil.runBuildWithConsoleOutput(context) { BuildLauncher buildLauncher ->
                def args = context.commandLine.remainingArgsString?.trim()
                if(args) {
                    buildLauncher.withArguments(args)
                }
            }
            return true
        }
        return false
    }

    

    @groovy.transform.CompileDynamic
    @Override
    public List<CommandDescription> listCommands(ProjectContext context) {
        [new CommandDescription("gradle", "Runs the gradle build", "usage: gradle [task]")]
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

    public Completer createCompleter(ProjectContext context) {
        new ArgumentCompleter(new StringsCompleter("gradle"), new ClosureCompleter({ listAllTaskSelectors(context) }))
    }
    
    private static class ClosureCompleter implements Completer {
        private Closure<Set<String>> closure
        private Completer completer
        
        public ClosureCompleter(Closure<Set<String>> closure) {
            this.closure = closure
        }
        
        Completer getCompleter() {
            if(completer == null) {
                completer = new StringsCompleter(closure.call())
            } 
            completer
        }
        
        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            getCompleter().complete(buffer, cursor, candidates)
        }
    }
}
