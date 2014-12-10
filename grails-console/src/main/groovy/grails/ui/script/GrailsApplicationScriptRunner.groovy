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
package grails.ui.script

import grails.ui.support.DevelopmentGrailsApplication
import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext
/**
 * Used to run Grails scripts within the context of a Grails application
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GrailsApplicationScriptRunner extends DevelopmentGrailsApplication {
    File script

    private GrailsApplicationScriptRunner(File script, Object... sources) {
        super(sources)
        this.script = script
    }

    @Override
    ConfigurableApplicationContext run(String... args) {
        ConfigurableApplicationContext ctx
        try {
            ctx = super.run(args)
        } catch (Throwable e) {
            System.err.println("Context failed to load: $e.message")
            System.exit(1)
        }

        def binding = new Binding()
        binding.setVariable("ctx", ctx)
        try {
            new GroovyShell(binding).evaluate(script)
        } catch (Throwable e) {
            System.err.println("Script execution error: $e.message")
            System.exit(1)
        }
        finally {
            try {
                ctx?.close()
            } catch (Throwable e) {
                // ignore
            }
        }
        return ctx
    }
    /**
     * Main method to run an existing Application class
     *
     * @param args The first argument is the Application class name
     */
    public static void main(String[] args) {
        if(args.size() > 1) {
            def applicationClass = Thread.currentThread().contextClassLoader.loadClass(args[0])
            File script = new File(args[1]);
            if(script.exists()) {
                new GrailsApplicationScriptRunner(script, applicationClass).run(args[1..-1] as String[])
            }
            else {
                System.err.println("Specified script [$script] does not exist")
                System.exit(1)
            }
        }
        else {
            System.err.println("Missing application class name and script name arguments")
            System.exit(1)
        }
    }
}
