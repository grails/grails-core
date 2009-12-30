import grails.util.GrailsNameUtils

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
 * Command to enable integration of Grails with external IDEs and build systems
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 1.2
 */
includeTargets << grailsScript("_GrailsInit")

integrationFiles = new File("${projectWorkDir}/integration-files")
target(integrateWith:"Integrates ") {
    depends(parseArguments)


    def keys = argsMap.keySet()
    try {
        event("IntegrateWithInit", keys.toList())
        for(key in keys) {
            if(key == 'params') continue
            try {
                def name = GrailsNameUtils.getClassNameRepresentation(key)
                "integrate${name}"()
            }
            catch (e) {
                println "Error: failed to integrate [${key}] with Grails: ${e.message}"
                exit 1
            }
        }
    }
    finally {
        ant.delete(dir:integrationFiles, failonerror:false)
    }


}

target(integrateAnt:"Integrates Ant with Grails") {
   depends unpackSupportFiles
   ant.copy(todir:basedir) {
       fileset(dir:integrationFiles, includes:"*.xml")
   }
   replaceTokens()
   println "Created Ant and Ivy builds files."

}
target(integrateTextmate:"Integrates Textmate with Grails") {
   depends unpackSupportFiles
   ant.copy(todir:basedir) {
       fileset(dir:"${integrationFiles}/textmate")
   }

   ant.move(file: "${basedir}/project.tmproj", tofile: "${basedir}/${grailsAppName}.tmproj", overwrite: true)

   replaceTokens()
   println "Created Textmate project files."
}
target(integrateEclipse:"Integrates Eclipse STS with Grails") {
    depends unpackSupportFiles

    ant.copy(todir:basedir) {
        fileset(dir:"${integrationFiles}/eclipse")
    }

    replaceTokens()
    println "Created Eclipse project files."
}
target(integrateIntellij:"Integrates Intellij with Grails") {
   depends unpackSupportFiles

   ant.copy(todir:basedir) {
       fileset(dir:"${integrationFiles}/intellij")
   }
    ant.move(file: "${basedir}/ideaGrailsProject.iml", tofile: "${basedir}/${grailsAppName}.iml", overwrite: true)
    ant.move(file: "${basedir}/ideaGrailsProject.ipr", tofile: "${basedir}/${grailsAppName}.ipr", overwrite: true)
    ant.move(file: "${basedir}/ideaGrailsProject.iws", tofile: "${basedir}/${grailsAppName}.iws", overwrite: true)

    replaceTokens()
    println "Created IntelliJ project files."
}

target(replaceTokens:"Replaces any tokens in the files") {
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

target(unpackSupportFiles:"Unpacks the support files") {

    if(!integrationFiles.exists()) {
        grailsUnpack(dest: integrationFiles.path, src: "grails-integration-files.jar")
    }
}

setDefaultTarget("integrateWith")

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
