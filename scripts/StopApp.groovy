/*
 * Copyright 2012 the original author or authors.
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
 * Stops the forked Grails application if it is running.
 *
 * @author Graeme Rocher
 *
 * @since 2.2
 */

includeTargets << grailsScript("_GrailsRun")

target('default': "Stops a forked Grails application") {
    depends(checkVersion, configureProxy)

    try {
        grailsConsole.updateStatus "Stopping Grails Server..."
        def url = "http://${serverHost ?: 'localhost'}:${serverPort+1}"
        grailsConsole.verbose "URL to stop server is $url"
        new URL(url).getText(connectTimeout: 10000, readTimeout: 10000)
        grailsConsole.updateStatus "Server Stopped"
    }
    catch(e) {
        grailsConsole.updateStatus "Server Stopped"
    }
}
