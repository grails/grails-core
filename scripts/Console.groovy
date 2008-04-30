/*
 * Copyright 2004-2005 the original author or authors.
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

/**
 * Gant script that loads the Grails console
 * 
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import groovy.text.SimpleTemplateEngine  
import org.codehaus.groovy.grails.support.*

Ant.property(environment:"env")                             
grailsHome = Ant.antProject.properties."env.GRAILS_HOME"    

includeTargets << new File ( "${grailsHome}/scripts/Bootstrap.groovy" )

target ('default': "Load the Grails interactive Swing console") {
	depends( checkVersion, configureProxy, packageApp, classpath)
	console()
}            

target(console:"The console implementation target") {

    classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], rootLoader)
    Thread.currentThread().setContextClassLoader(classLoader)
	loadApp()
	configureApp()
    createConsole()
    try {
        console.run()
        monitorCallback = {
            println "Exiting console"
            console.exit()
            createConsole()
            println "Restarting console"
            console.run()
        }
        monitorApp()
        //while(true) { sleep(Long.MAX_VALUE) }
    } catch (Exception e) {
        event("StatusFinal", ["Error starting console: ${e.message}"])
    }
}

target(createConsole:"Creates a new console") {
    def b = new Binding()
    b.ctx = appCtx
    b.grailsApplication = grailsApp
    console = new groovy.ui.Console(grailsApp.classLoader, b)
    console.beforeExecution = {
        appCtx.getBeansOfType(PersistenceContextInterceptor).each { k,v ->
            v.init()
        }
    }
    console.afterExecution = {
        appCtx.getBeansOfType(PersistenceContextInterceptor).each { k,v ->
            v.flush()
            v.destroy()
        }
    }

}
