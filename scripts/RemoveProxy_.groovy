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
 * @author Graeme Rocher
 * @since 1.2.3
 */

target(default: "Removes a proxy configuration") {

    if (!argsMap.params) {
        println msg()
        exit 1
    }

    def settingsFile = grailsSettings.proxySettingsFile
    config = grailsSettings.proxySettings
    def name = argsMap.params[0]
    config.remove(name)

    settingsFile.withWriter('UTF-8') { w -> config.writeTo(w) }

    println "Removed proxy configuration [${name}]."
}

String msg() {
    return '''\
Usage: grails remove-proxy [name]
Example: grails remove-proxy client
'''
}
