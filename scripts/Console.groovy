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

import org.codehaus.groovy.grails.support.*

includeTargets << grailsScript("_GrailsBootstrap")

target ('default': "Load the Grails interactive Swing console") {
	depends( checkVersion, configureProxy, packageApp, classpath)
	console()
}            

target(console:"The console implementation target") {

//    classLoader = new URLClassLoader([classesDir.toURI().toURL()] as URL[], rootLoader)
//    Thread.currentThread().setContextClassLoader(classLoader)
	loadApp()
	configureApp()

    try {
        def console = createConsole()
        console.run()

        // On each monitor check, determine whether the console window
        // is still open. If not, we set the monitor flag so that its
        // thread ends and the script completes.
        monitorCheckCallback = {
            if (!console.frame.visible) keepMonitoring = false
        }

        // If the app is recompiled, we close the console and start it
        // up again with the new classes.
        monitorRecompileCallback = {
            println "Exiting console"
            console.exit()
            String scriptText = console.inputArea.text
            createConsole()
            println "Restarting console"
            console.run()
            console.inputArea.addFocusListener(new ConsoleFocusListener(text:scriptText))
        }

        // Start the monitor thread.
        monitorApp()
        //while(true) { sleep(Long.MAX_VALUE) }
    } catch (Exception e) {
        event("StatusFinal", ["Error starting console: ${e.message}"])
    }
}

createConsole = {
    def b = new Binding()
    b.ctx = appCtx
    b.grailsApplication = grailsApp

    def console = new groovy.ui.Console(grailsApp.classLoader, b)
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

    return console
}

class ConsoleFocusListener implements java.awt.event.FocusListener{
	String text
	void focusGained(java.awt.event.FocusEvent e){
		e.source.text = text
		e.source.removeFocusListener(this)
	}
	void focusLost(java.awt.event.FocusEvent e){}	
}
