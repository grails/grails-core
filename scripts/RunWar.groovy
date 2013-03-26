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

import grails.util.BuildScope

/**
 * Gant script that executes creates WAR file and runs Grails using an embedded Jetty server
 * against the WAR, without reloading
 *
 * @author Graeme Rocher
 *
 * @since 0.4
 */

scriptScope = BuildScope.WAR
includeTargets << grailsScript("_GrailsWar")
includeTargets << grailsScript("_GrailsRun")

target ('default': "Runs a Grails application's WAR in an embedded web server") {
    depends(checkVersion, configureProxy, parseArguments)

    if (argsMap.restart) {
        configureServerContextPath()
        configureWarName()
    }
    else {
        war()
    }

    if (argsMap.https) {
        runWarHttps()
    }
    else {
        runWar()
    }
    keepServerAlive()
}
