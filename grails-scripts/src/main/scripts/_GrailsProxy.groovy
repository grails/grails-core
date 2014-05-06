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
 * Contains a target for configuring an HTTP proxy.
 *
 * @author Peter Ledbrook
 *
 * @since 1.1
 */

target(configureProxy: "The implementation target") {
    def scriptFile = new File("${userHome}/.grails/scripts/ProxyConfig.groovy")
    if (!scriptFile.exists()) {
        return
    }

    includeTargets << scriptFile.getText("UTF-8")

    if (!proxyConfig.proxyHost) {
        return
    }

    // Let's configure proxy...
    def proxyHost = proxyConfig.proxyHost
    def proxyPort = proxyConfig.proxyPort ? proxyConfig.proxyPort : '80'
    def proxyUser = proxyConfig.proxyUser ? proxyConfig.proxyUser : ''
    def proxyPassword = proxyConfig.proxyPassword ? proxyConfig.proxyPassword : ''
    grailsConsole.updateStatus "Configured HTTP proxy: ${proxyHost}:${proxyPort}${proxyConfig.proxyUser ? '(' + proxyUser + ')' : ''}"
    // ... for ant. We can remove this line with ant 1.7.0 as it uses system properties.
    ant.setproxy(proxyhost: proxyHost, proxyport: proxyPort, proxyuser: proxyUser, proxypassword: proxyPassword)
    // ... for all other code
    System.properties.putAll(["http.proxyHost": proxyHost, "http.proxyPort": proxyPort,
                              "http.proxyUserName": proxyUser, "http.proxyPassword": proxyPassword])
}
