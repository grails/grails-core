/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.cli.gradle

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jline.console.completer.*

import org.gradle.tooling.BuildLauncher
import org.gradle.tooling.ProjectConnection
import org.grails.cli.gradle.FetchAllTaskSelectorsBuildAction.AllTasksModel
import org.grails.cli.gradle.cache.ListReadingCachedGradleOperation
import org.grails.cli.profile.*

/**
 * A {@link CommandLineHandler} for launching Gradle tasks
 *
 * @author Lari Hotari
 * @author Graeme Rocher
 *
 * @since 3.0
 */
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
    

    

    public Completer createCompleter(ProjectContext context) {
        def readTasks = new ReadTasks(context, ".gradle-tasks")
        new ArgumentCompleter(new StringsCompleter("gradle"), new ClosureCompleter((Closure<Collection<String>>){ readTasks.call() }))
    }
    
    private static class ClosureCompleter implements Completer {
        private Closure<Collection<String>> closure
        private Completer completer
        
        public ClosureCompleter(Closure<Collection<String>> closure) {
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

    @InheritConstructors
    static class ReadTasks extends ListReadingCachedGradleOperation<String> {

        private static final Closure<String> taskNameFormatter = { String projectPath, String taskName ->
            if(projectPath == ':') {
                ":$taskName".toString()
            } else {
                "$projectPath:$taskName".toString()
            }
        }

        @Override
        protected String createListEntry(String str) { str }

        @Override
        List<String> readFromGradle(ProjectConnection connection) {
            AllTasksModel allTasksModel = (AllTasksModel)connection.action(new FetchAllTaskSelectorsBuildAction(projectContext.getBaseDir())).run()
            Collection<String> allTaskSelectors=[]

            if (allTasksModel.currentProject) {
                allTaskSelectors.addAll(allTasksModel.allTaskSelectors.get(allTasksModel.currentProject))
            }

            allTasksModel.projectPaths.each { String projectName, String projectPath ->
                allTasksModel.allTasks.get(projectName).each { String taskName ->
                    allTaskSelectors.add(taskNameFormatter(projectPath, taskName))
                }
            }

            allTaskSelectors.unique().toList()
        }
    }
}
