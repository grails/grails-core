/*
 * Copyright 2011 the original author or authors.
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

import groovy.io.FileType

includeTargets << grailsScript("_GrailsInit")

USAGE = """
    wrapper [--wrapperDir=dir] [--distributionUrl=url]

where
    --wrapperDir = Directory where wrapper support files are installed relative to project root
    --distributationUrl = URL to the directory where the release may be downloaded from if necessary

examples
    grails wrapper --wrapperDir=grailsWrapper
    grails wrapper --wrapperDir=grailsWrapper --distributionUrl=http://dist.springframework.org.s3.amazonaws.com/milestone/GRAILS/

optional argument default values
    wrapperDir = 'wrapper'
    distributionUrl = 'http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/'

"""

target (generateWrapper: "Generates the Grails wrapper") {
    depends(checkVersion, parseArguments)
    event 'InstallWrapperStart', [ 'Installing Wrapper...' ]

    grailsDistUrl =  argsMap.distributionUrl ?: 'http://dist.springframework.org.s3.amazonaws.com/release/GRAILS/'
    grailsWrapperDir = argsMap.wrapperDir ?: 'wrapper'

    targetDir = "${basedir}/${grailsWrapperDir}"

    supportFiles = []
    new File(grailsHome, "dist").eachFileMatch( FileType.FILES, { it ==~ /grails-wrapper-support.*\.jar/ && !it.contains('-sources.jar') && !it.contains('-javadoc.jar')}) {
        supportFiles << it
    }
    if (supportFiles.size() != 1) {
        if (supportFiles.size() == 0) {
            event("StatusError", ["An error occurred locating the grails-wrapper-support jar file"])
        } else {
            event("StatusError", ["Multiple grails-wrapper-support jar files were found ${supportFiles.absolutePath}"])
        }
        exit 1
    }
    supportFile = supportFiles[0]
    ant.unjar(dest: targetDir, src: supportFile.absolutePath, overwrite: true) {
        patternset {
            exclude(name: "META-INF/**")
        }
    }
    ant.move(todir: basedir) {
        fileset(dir: targetDir) {
            include(name: 'grailsw*')
        }
    }
    ant.replace(dir: targetDir, includes: '*.properties', token: '@distributationUrl@', value: grailsDistUrl)
    ant.replace(dir: basedir, includes: 'grailsw*', token: '@wrapperDir@', value: grailsWrapperDir)
    ant.chmod(file: "${basedir}/grailsw", perm: 'u+x')

    springloadedFiles = []
    new File(grailsHome, "lib/org.springsource.springloaded/springloaded-core/jars/").eachFileMatch( FileType.FILES, { it ==~ /springloaded-core-.*/ }) {
        springloadedFiles << it
    }
    springloadedFiles = springloadedFiles.findAll { !it.name.contains('sources') &&  !it.name.contains('javadoc')}

    if (springloadedFiles.size() != 1) {
        if (springloadedFiles.size() == 0) {
            event("StatusError", ["An error occurred locating the springloaded-core jar file"])
        } else {
            event("StatusError", ["Multiple springloaded-core jar files were found ${springloadedFiles.absolutePath}"])
        }
        exit 1
    }

    springloadedFile = springloadedFiles[0]

    ant.copy(todir: targetDir, file: springloadedFile.absolutePath, overwrite: true)

    event("StatusUpdate", [ "Wrapper installed successfully"])
    event 'InstallWrapperEnd', [ 'Finished Installing Wrapper.' ]
}
