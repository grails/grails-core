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
package grails.ui.console

import grails.boot.GrailsApp
import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ResourceLoader


/**
 * The Grails console runs Grails embedded within a Swing console instead of within a container like Tomcat
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsSwingConsole extends GrailsApp {

    static {
        System.setProperty("java.awt.headless", "false");
    }

    GrailsSwingConsole(Object... sources) {
        super(sources)
        setApplicationContextClass(GroovyConsoleApplicationContext)
    }

    GrailsSwingConsole(ResourceLoader resourceLoader, Object... sources) {
        super(resourceLoader, sources)
        setApplicationContextClass(GroovyConsoleApplicationContext)
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object source, String... args) {
        return run([ source ] as Object[], args);
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Object[] sources, String[] args) {
        return new GrailsSwingConsole(sources).run(args);
    }
}
