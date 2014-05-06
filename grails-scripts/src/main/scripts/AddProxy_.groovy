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

target(default:"Adds a proxy configuration") {
    if (!argsMap.params) {
        grailsConsole.error msg()
        exit 1
    }

    if (argsMap.host && argsMap.port) {
        def settingsFile = grailsSettings.proxySettingsFile
        config = grailsSettings.proxySettings

        config[argsMap.params[0]] = ['http.proxyHost':argsMap.host,
                                     'http.proxyPort':argsMap.port,
                                     "http.proxyUser": argsMap.username?: '',
                                     "http.proxyPassword": argsMap.password?: '',
                                     'http.nonProxyHosts': argsMap.noproxy?: '']

        settingsFile.withWriter('UTF-8') { w -> config.writeTo(w) }

        grailsConsole.updateStatus "Added proxy ${argsMap.params[0]} to ${settingsFile}"
    }
    else {
        println msg()
        exit 1
    }
}

String msg() {
    return '''\
Usage: grails add-proxy [name] --host=[server] --port=[port] --username=[username]* --password=[password]* --noproxy=[no|proxy|hosts]*
Example: grails add-proxy client --host=proxy-server --port=4300 --username=guest --password=guest --noproxy=somehost|*.intra.net

* Optional
'''
}
