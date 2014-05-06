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
 * Gant script that creates a ZIP file creating just the artifacts from a project, for attaching to a JIRA issue
 *
 * @author Marc Palmer
 * @since 0.4
 */

import java.text.SimpleDateFormat

includeTargets << grailsScript("_GrailsInit")

target(bugReport: "Creates a ZIP containing source artifacts for reporting bugs") {
    depends(checkVersion)

    String fileName = new File(basedir).name
    String date = new SimpleDateFormat("ddMMyyyy").format(new Date())
    String zipName = "$basedir/${fileName}-bug-report-${date}.zip"

    ant.zip(destfile: zipName, filesonly: true) {
        fileset(dir: basedir) {
            include name: 'grails-app/**'
            include name: 'src/**'
            include name: 'test/**'
            include name: 'scripts/**'
            include name: '*GrailsPlugin.groovy'
        }
        fileset file: "$basedir/application.properties"
    }

    event("StatusFinal", ["Created bug-report ZIP at $zipName"])
}

setDefaultTarget 'bugReport'
