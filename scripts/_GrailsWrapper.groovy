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

    releaseType = 'release'
    if(grailsVersion ==~ /.*\.RC[0-9]+/ || grailsVersion ==~ /.*\.M[0-9]+/) {
        releaseType = 'milestone'
    } else if(grailsVersion.endsWith('BUILD-SNAPSHOT')) {
        releaseType = 'snapshot'
    }

    grailsDistUrl =  argsMap.distributionUrl ?: "http://dist.springframework.org.s3.amazonaws.com/${releaseType}/GRAILS/"
    grailsWrapperDir = argsMap.wrapperDir ?: 'wrapper'

    def targetDir = "${basedir}/${grailsWrapperDir}"

    grailsUnpack(dest: targetDir, src: "grails-wrapper-support.jar")
    ant.move(todir: basedir) {
        fileset(dir: targetDir) {
            include(name: 'grailsw*')
        }
    }
    ant.replace(dir: targetDir, encoding:'UTF-8', includes: '*.properties', token: '@distributationUrl@', value: grailsDistUrl)
    ant.replace(dir: basedir, encoding:'UTF-8', includes: 'grailsw*', token: '@wrapperDir@', value: grailsWrapperDir)
    ant.chmod(file: "${basedir}/grailsw", perm: 'u+x')

    event("StatusUpdate", [ "Wrapper installed successfully"])
    event 'InstallWrapperEnd', [ 'Finished Installing Wrapper.' ]
}
