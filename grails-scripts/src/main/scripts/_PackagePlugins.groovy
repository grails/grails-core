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

import org.codehaus.groovy.grails.project.packaging.GrailsProjectPackager

/**
 * Gant script that handles the packaging of Grails plug-ins.
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

projectPackager = new GrailsProjectPackager(projectCompiler, eventListener, configFile, false)
projectPackager.servletVersion = servletVersion

packageFiles = { String from ->
    projectPackager.packageConfigFiles(from)
}

target(packagePlugins : "Packages any Grails plugins that are installed for this project") {
    depends(classpath, resolveDependencies)
    projectPackager.packagePlugins()
}

packagePluginsForWar = { targetDir ->
    try {
        projectPackager.packagePluginsForWar(targetDir)
    }
    catch(e) {
        grailsConsole.error e.message, e
        exit 1
    }
}
