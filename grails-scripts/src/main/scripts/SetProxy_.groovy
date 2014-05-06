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

import grails.util.BuildSettings

/**
 * Gant script for setting HTTP proxy-settings.
 *
 * @author Graeme Rocher
 *
 * @since 1.2.3
 */

target ("default" : "Sets HTTP proxy configuration for Grails") {
    if (argsMap.params) {
        def name = argsMap.params[0]

        BuildSettings settings = grailsSettings
        ConfigObject proxySettings = settings.proxySettings
        if (proxySettings[name]) {
            proxySettings.currentProxy = name
            settings.proxySettingsFile.withWriter('UTF-8') { w ->
                proxySettings.writeTo(w)
            }
            grailsConsole.updateStatus "Proxy set to [$name]"
        }
        else {
            grailsConsole.error "No proxy configuration found for name: $name. Please add a proxy with add-proxy first."
            exit 1
        }
    }
    else {
        grailsConsole.error '''\
Usage: grails set-proxy [name]

Sets a named proxy. Use "grails add-proxy" to add a named proxy.
'''
        exit 1
    }
}
