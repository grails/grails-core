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
 * Gant script that loads the Grails console.
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

import java.awt.event.FocusEvent
import java.awt.event.FocusListener

import org.codehaus.groovy.grails.compiler.GrailsProjectWatcher
import org.codehaus.groovy.grails.support.*

includeTargets << grailsScript("_GrailsBootstrap")

target ('default': "Load the Grails interactive Swing console") {
    depends(checkVersion, configureProxy, packageApp, classpath)
    console()
}

target(console:"The console implementation target") {
    depends(loadApp, configureApp)

    try {
        createConsole().run()
        def watcher = new GrailsProjectWatcher(projectCompiler, pluginManager)
        watcher.start()
        // keep the console running
        while (true) {
            sleep(Integer.MAX_VALUE)
        }
    } catch (Exception e) {
        event("StatusFinal", ["Error starting console: ${e.message}"])
    }
}

createConsole = {
    def b = new Binding(ctx: appCtx, grailsApplication: grailsApp)

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

class ConsoleFocusListener implements FocusListener {
    String text
    void focusGained(FocusEvent e) {
        e.source.text = text
        e.source.removeFocusListener(this)
    }
    void focusLost(FocusEvent e) {}
}
