package org.grails.gradle.plugin.watch

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.gradle.api.Named

/**
 *
 * A configuration for watching files for changes
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@ToString
class WatchConfig implements Named {

    WatchConfig(String name) {
        this.name = name
    }

    /**
     * The name of the config
     */
    String name
    /**
     * The directory to watch
     */
    File directory


    /**
     * The file extensions to watch
     */
    List<String> extensions

    /**
     * The tasks to execute
     */
    List<String> tasks = []

    /**
     * The tasks to trigger when a modification event is received
     *
     * @param tasks The tasks
     */
    void tasks(String... tasks) {
        this.tasks.addAll(Arrays.asList(tasks))
    }
}
