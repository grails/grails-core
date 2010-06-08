
/**
 * @author Graeme Rocher
 * @since 1.2.3
 */

target(default:"Clears a proxy configuration") {
    def settingsFile = grailsSettings.proxySettingsFile
    config = grailsSettings.proxySettings
    config.remove('currentProxy')

    settingsFile.withWriter { w ->
        config.writeTo(w)
    }

    println "Cleared proxy settings."
}

