
/**
 * @author Graeme Rocher
 * @since 1.2.3
 */

includeTargets << grailsScript("_GrailsArgParsing")
target(default:"Removes a proxy configuration") {
    depends(parseArguments)


    if(!argsMap.params) {
        println msg()
        exit 1
    }
    else {
        def settingsFile = grailsSettings.proxySettingsFile
        config = grailsSettings.proxySettings
        def name = argsMap.params[0]
        config.remove(name)

        settingsFile.withWriter { w ->
            config.writeTo(w)
        }

        println "Removed proxy configuration [${name}]."
    }
}

String msg() {
    return '''\
Usage: grails remove-proxy [name]
Example: grails remove-proxy client
'''
}
