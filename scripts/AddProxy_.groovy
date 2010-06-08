
/**
 * @author Graeme Rocher
 * @since 1.2.3
 */

includeTargets << grailsScript("_GrailsArgParsing")
target(default:"Adds a proxy configuration") {
    depends(parseArguments)


    if(!argsMap.params) {
        println msg()
        exit 1
    }
    else {
        if(argsMap.host && argsMap.port) {
            def settingsFile = grailsSettings.proxySettingsFile 
            config = grailsSettings.proxySettings

            config[argsMap.params[0]] = ['http.proxyHost':argsMap.host,
                                         'http.proxyPort':argsMap.port,
                                         "http.proxyUserName": argsMap.username?: '',
                                         "http.proxyPassword": argsMap.password?: '']

            settingsFile.withWriter { w ->
                config.writeTo(w)
            }

            println "Added proxy ${argsMap.params[0]} to ${settingsFile}"
        }
        else {
            println msg()
            exit 1
        }
    }
}

String msg() {
    return '''\
Usage: grails add-proxy [name] --host=[server] --port=[port] --username=[username]* --password=[password]*
Example: grails add-proxy client --host=proxy-server --port=4300 --username=guest --password=guest

* Optional
'''
}
