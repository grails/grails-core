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

import grails.ui.console.GrailsSwingConsole

import org.codehaus.groovy.grails.cli.interactive.InteractiveMode
import org.codehaus.groovy.grails.project.ui.GrailsProjectConsole

includeTargets << grailsScript("_GrailsBootstrap")

projectConsole = new GrailsProjectConsole(projectLoader)

target(console: "Load the Grails interactive Swing console") {
    depends(checkVersion, configureProxy, enableExpandoMetaClass, classpath)

    def forkSettings = grailsSettings.forkSettings
    def forkConfig = forkSettings?.console


    packageApp()
    if (forkConfig == false || forkConfig == 'false') {
        try {
            
            projectConsole.run()
        } catch (Exception e) {
            grailsConsole.error "Error starting console: ${e.message}", e
        }
    }
    else {
        def forkedConsole = new GrailsSwingConsole(grailsSettings)
        if (forkConfig instanceof Map) {
            forkedConsole.configure(forkConfig)
        }
        if (InteractiveMode.active) {
            grailsConsole.addStatus "Running Grails Console..."
            Thread.start {
                forkedConsole.fork()
            }
        }
        else {
            forkedConsole.fork()
        }
    }
}

createConsole = {
    projectConsole.createConsole()
}

setDefaultTarget 'console'
