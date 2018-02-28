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
import grails.ui.console.support.GroovyConsoleApplicationContext
import grails.ui.console.support.GroovyConsoleWebApplicationContext
import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ResourceLoader
import org.springframework.util.ClassUtils


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

    GrailsSwingConsole(Class<?>... sources) {
        super(sources)
        configureApplicationContextClass()
    }

    GrailsSwingConsole(ResourceLoader resourceLoader, Class<?>... sources) {
        super(resourceLoader, sources)
        configureApplicationContextClass()
    }

    public configureApplicationContextClass() {
        if(ClassUtils.isPresent("javax.servlet.ServletContext", Thread.currentThread().contextClassLoader)) {
            setApplicationContextClass(GroovyConsoleWebApplicationContext)
        }
        else {
            setApplicationContextClass(GroovyConsoleApplicationContext)
        }
    }


    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified source using default settings.
     * @param source the source to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class<?> source, String... args) {
        return run([ source ] as Class[], args);
    }

    /**
     * Static helper that can be used to run a {@link GrailsApp} from the
     * specified sources using default settings and user supplied arguments.
     * @param sources the sources to load
     * @param args the application arguments (usually passed from a Java main method)
     * @return the running {@link org.springframework.context.ApplicationContext}
     */
    public static ConfigurableApplicationContext run(Class<?>[] sources, String[] args) {
        return new GrailsSwingConsole(sources).run(args);
    }

    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Application class name
     */
    public static void main(String[] args) {
        if(args) {
            def applicationClass = Thread.currentThread().contextClassLoader.loadClass(args[0])
            new GrailsSwingConsole(applicationClass).run(args)
        }
        else {
            System.err.println("Missing application class name argument")
        }
    }
}
