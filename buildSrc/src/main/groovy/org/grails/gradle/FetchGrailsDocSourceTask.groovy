/*
* Copyright 2024 The Unity Foundation
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.grails.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task to fetch the Grails documentation source code.
 *
 * @author Puneet Behl
 * @since 7.0.0
 */
class FetchGrailsDocSourceTask extends DefaultTask {

    @Optional @Input String explicitGrailsDocHome = project.findProperty('grails-doc.home') ?: null
    @Optional @Input String grailsDocBranch = System.getenv('TARGET_GRAILS_DOC_BRANCH') ?: 'main'
    @Optional @OutputDirectory checkoutDir = project.findProperty('grails-doc.checkoutDir') ?: project.layout.buildDirectory.dir("checkout")

    @TaskAction
    void fetchGrailsDocSource() {

        if (!explicitGrailsDocHome) {
            ant.mkdir(dir: checkoutDir)

            def zipFile = "${checkoutDir}/grails-doc-src.zip"
            if (grailsDocBranch) {
                ant.get(src: "https://github.com/grails/grails-doc/archive/refs/heads/${grailsDocBranch}.zip", dest: zipFile, verbose: true)
                ant.unzip(src: zipFile, dest: checkoutDir) {
                    mapper(type: 'regexp', from:/(grails-doc-[\w]*\/)(.*)/, to:/grails-docs-src\/\2/)
                }
            }
        }
    }
}
