package org.grails.gradle.plugin.watch

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.tooling.*
import org.grails.io.support.DevNullPrintStream
import org.grails.io.watch.DirectoryWatcher

/**
 * A plugin that allows watching for file changes and triggering recompilation of the Gradle build
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class GrailsWatchPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.watch = project.container(WatchConfig) { name ->
            project.extensions.create(name, WatchConfig, name)
        }

        def watchTask = project.task("watch") << {
            NamedDomainObjectContainer<WatchConfig> watchConfigs = project.extensions.getByName("watch")
            DirectoryWatcher directoryWatcher = new DirectoryWatcher()

            ProjectConnection connection = GradleConnector.newConnector()
                                               .useInstallation( project.gradle.gradleHomeDir )
                                               .forProjectDirectory( project.projectDir )
                                               .connect()

            Thread.start {
                sleep 6000
                // initialise the build in a background thread so as to make it quicker to run the first time
                PrintStream previousOut = System.out
                try {
                    PrintStream sysOut = new DevNullPrintStream()
                    System.out = sysOut
                    connection.newBuild()
                              .setStandardOutput(sysOut)
                              .setStandardError( new DevNullPrintStream() )
                              .withArguments('-q').run()
                } finally {
                    System.out = previousOut
                }
            }

            List<String> tasks = []
            for(WatchConfig wc in watchConfigs) {
                if(wc.directory && wc.extensions) {
                    directoryWatcher.addWatchDirectory(wc.directory, wc.extensions)
                    tasks.addAll( wc.tasks )
                }
            }

            tasks = tasks.unique()

            directoryWatcher.addListener(new DirectoryWatcher.FileChangeListener() {
                @Override
                void onChange(File file) {
                    connection
                            .newBuild()
                            .forTasks( tasks as String[] )
                            .run()
                }

                @Override
                void onNew(File file) {

                }
            })
            directoryWatcher.start()
        }

        Task runTasks = project.tasks.findByName('run')
        if(runTasks) {
            runTasks.dependsOn( watchTask )
        }
    }
}
