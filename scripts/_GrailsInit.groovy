
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
* Gant script that handles general initialization of a Grails applications
*
* @author Graeme Rocher
*
* @since 0.4
*/

import org.springframework.core.io.FileSystemResource
import grails.util.GrailsNameUtils
import groovy.grape.Grape
import grails.util.Metadata

// No point doing this stuff more than once.
if (getBinding().variables.containsKey("_init_called")) return
_init_called = true

Grape.enableAutoDownload = true

// add includes
includeTargets << grailsScript("_GrailsArgParsing")
includeTargets << grailsScript("_PluginDependencies")


// Generates Eclipse .classpath entries for all the Grails dependencies,
// i.e. a string containing a "<classpath entry ..>" element for each
// of Grails' library JARs. This only works if $GRAILS_HOME is set.
eclipseClasspathLibs = {
    def result = ''
    if (grailsHome) {
        (new File("${grailsHome}/lib")).eachFileMatch(~/.*\.jar/) {file ->
            if (!file.name.startsWith("gant-")) {
                result += "<classpathentry kind=\"var\" path=\"GRAILS_HOME/lib/${file.name}\" />\n\n"
            }
        }
    }
    result
}

intellijClasspathLibs = {
    def builder = new StringBuilder()
    if (grailsHome) {
        (new File("${grailsHome}/lib")).eachFileMatch(~/.*\.jar/) {file ->
            if (!file.name.startsWith("gant-")) {
                builder << "<root url=\"jar://${grailsHome}/lib/${file.name}!/\" />\n\n"
            }
        }
        (new File("${grailsHome}/dist")).eachFileMatch(~/^grails-.*\.jar/) {file ->
            builder << "<root url=\"jar://${grailsHome}/dist/${file.name}!/\" />\n\n"
        }

    }

    return builder.toString()
}


// Generates Eclipse .classpath entries for the Grails distribution
// JARs. This only works if $GRAILS_HOME is set.
eclipseClasspathGrailsJars = {args ->
    result = ''
    if (grailsHome) {
        (new File("${grailsHome}/dist")).eachFileMatch(~/^grails-.*\.jar/) {file ->
            result += "<classpathentry kind=\"var\" path=\"GRAILS_HOME/dist/${file.name}\" />\n\n"
        }
    }
    result
}

target(createStructure: "Creates the application directory structure") {
    ant.sequential {
        mkdir(dir: "${basedir}/src")
        mkdir(dir: "${basedir}/src/java")
        mkdir(dir: "${basedir}/src/groovy")
        mkdir(dir: "${basedir}/grails-app")
        mkdir(dir: "${basedir}/grails-app/controllers")
        mkdir(dir: "${basedir}/grails-app/services")
        mkdir(dir: "${basedir}/grails-app/domain")
        mkdir(dir: "${basedir}/grails-app/taglib")
        mkdir(dir: "${basedir}/grails-app/utils")
        mkdir(dir: "${basedir}/grails-app/views")
        mkdir(dir: "${basedir}/grails-app/views/layouts")
        mkdir(dir: "${basedir}/grails-app/i18n")
        mkdir(dir: "${basedir}/grails-app/conf")
        mkdir(dir: "${basedir}/test")
        mkdir(dir: "${basedir}/test/unit")
        mkdir(dir: "${basedir}/test/integration")
        mkdir(dir: "${basedir}/scripts")
        mkdir(dir: "${basedir}/web-app")
        mkdir(dir: "${basedir}/web-app/js")
        mkdir(dir: "${basedir}/web-app/css")
        mkdir(dir: "${basedir}/web-app/images")
        mkdir(dir: "${basedir}/web-app/META-INF")
        mkdir(dir: "${basedir}/lib")
        mkdir(dir: "${basedir}/grails-app/conf/spring")
        mkdir(dir: "${basedir}/grails-app/conf/hibernate")
    }
}

target(checkVersion: "Stops build if app expects different Grails version") {
    if (metadataFile.exists()) {
        if (appGrailsVersion != grailsVersion) {
            event("StatusFinal", ["Application expects grails version [$appGrailsVersion], but GRAILS_HOME is version " +
                    "[$grailsVersion] - use the correct Grails version or run 'grails upgrade' if this Grails " +
                    "version is newer than the version your application expects."])
            exit(1)
        }
    } else {
        // We know this is pre-0.5 application
        event("StatusFinal", ["Application is pre-Grails 0.5, please run: grails upgrade"])
        exit(1)
    }
}

target(updateAppProperties: "Updates default application.properties") {
    def entries = [ "app.name": "$grailsAppName", "app.grails.version": "$grailsVersion" ]
    if (grailsAppVersion) {
        entries["app.version"] = "$grailsAppVersion"
    }
    updateMetadata(entries)

    // Make sure if this is a new project that we update the var to include version
    appGrailsVersion = grailsVersion
}

target( launderIDESupportFiles: "Updates the IDE support files (Eclipse, TextMate etc.), changing file names and replacing tokens in files where appropriate.") {
    ant.move(file: "${basedir}/.launch", tofile: "${basedir}/${grailsAppName}.launch", overwrite: true)
    ant.move(file: "${basedir}/test.launch", tofile: "${basedir}/${grailsAppName}-test.launch", overwrite: true)
    ant.move(file: "${basedir}/project.tmproj", tofile: "${basedir}/${grailsAppName}.tmproj", overwrite: true)


    ant.move(file: "${basedir}/ideaGrailsProject.iml", tofile: "${basedir}/${grailsAppName}.iml", overwrite: true)
    ant.move(file: "${basedir}/ideaGrailsProject.ipr", tofile: "${basedir}/${grailsAppName}.ipr", overwrite: true)
    ant.move(file: "${basedir}/ideaGrailsProject.iws", tofile: "${basedir}/${grailsAppName}.iws", overwrite: true)

    

    def appKey = grailsAppName.replaceAll( /\s/, '.' ).toLowerCase()
    ant.replace(dir:"${basedir}", includes:"*.*") {
        replacefilter(token:"@grails.intellij.libs@", value: intellijClasspathLibs())
        replacefilter(token: "@grails.libs@", value: eclipseClasspathLibs())
        replacefilter(token: "@grails.jar@", value: eclipseClasspathGrailsJars())
        replacefilter(token: "@grails.version@", value: grailsVersion)
        replacefilter(token: "@grails.project.name@", value: grailsAppName)
        replacefilter(token: "@grails.project.key@", value: appKey)
    }
}

target(init: "main init target") {
    depends(createStructure, updateAppProperties)

    grailsUnpack(dest: basedir, src: "grails-shared-files.jar")
    grailsUnpack(dest: basedir, src: "grails-app-files.jar")
    launderIDESupportFiles()

    classpath()

    // Create a message bundle to get the user started.
    touch(file: "${basedir}/grails-app/i18n/messages.properties")

	// Set the default version number for the application
    updateMetadata(
            "app.version": grailsAppVersion ?: "0.1",
            "app.servlet.version": servletVersion)
}
