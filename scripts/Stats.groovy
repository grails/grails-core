/*
 * Copyright 2004-2009 the original author or authors.
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
 * Gant script which generates stats for a Grails project.
 *
 * @author Glen.Smith
 * @author Andres.Almiray
 */

// includeTargets << grailsScript("_GrailsSettings")
includeTargets << grailsScript("_GrailsEvents")

target (default: "Generates basic stats for a Grails project") {
    def EMPTY = /^\s*$/
    def SLASH_SLASH = /^\s*\/\/.*/
    def SLASH_STAR_STAR_SLASH = /^(.*)\/\*(.*)\*\/(.*)$/

    // TODO - handle slash_star comments inside strings
    def DEFAULT_LOC_MATCHER = { file ->
        loc = 0
        comment = 0
        file.eachLine { line ->
            if (line ==~ EMPTY) return
            if (line ==~ SLASH_SLASH) return
            def m = line =~ SLASH_STAR_STAR_SLASH
            if (m.count && m[0][1] ==~ EMPTY && m[0][3] ==~ EMPTY) return
            int open = line.indexOf("/*")
            int close = line.indexOf("*/")
            if (open != -1 && (close-open) <= 1) comment++
            else if (close != -1 && comment) comment--
            if (!comment) loc++
        }
        loc
    }

    // maps file path to
    def pathToInfo = [
        [name: "Controllers",        path: "^grails-app.controllers",      filetype: ["Controller.groovy"]],
        [name: "Domain Classes",     path: "^grails-app.domain",           filetype: [".groovy"]],
        [name: "Jobs",               path: "^grails-app.job",              filetype: [".groovy"]],
        [name: "Services",           path: "^grails-app.services",         filetype: ["Service.groovy"]],
        [name: "Tag Libraries",      path: "^grails-app.taglib",           filetype: ["TagLib.groovy"]],
        [name: "Groovy Helpers",     path: "^src.groovy",                  filetype: [".groovy"]],
        [name: "Java Helpers",       path: "^src.java",                    filetype: [".java"]],
        [name: "Unit Tests",         path: "^test.unit",                   filetype: [".groovy"]],
        [name: "Integration Tests",  path: "^test.integration",            filetype: [".groovy"]],
        [name: "Functional Tests",   path: "^test.functional",             filetype: [".groovy"]],
        [name: "Scripts",            path: "^scripts",                     filetype: [".groovy"]],
    ]

    event("StatsStart", [pathToInfo])

    def baseDirFile = new File(basedir)
    def baseDirPathLength = baseDirFile.path.size()+1
    baseDirFile.eachFileRecurse { file ->
        def match = pathToInfo.find { info ->
            file.path.substring(baseDirPathLength) =~ info.path &&
            info.filetype.any{ s -> file.path.endsWith(s) }
        }
        if (match && file.isFile()) {
            match.filecount = match.filecount ? match.filecount+1 : 1
            // strip whitespace
            loc = match.locmatcher ? match.locmatcher(file) : DEFAULT_LOC_MATCHER(file)
            match.loc = match.loc ? match.loc + loc : loc
        }
    }

    def totalFiles = 0
    def totalLOC = 0

    def sw = new StringWriter()
    def output = new PrintWriter(sw)

    output.println '''
    +----------------------+-------+-------+
    | Name                 | Files |  LOC  |
    +----------------------+-------+-------+'''

    pathToInfo.each { info ->
        if (info.filecount) {
            output.println "    | " +
                info.name.padRight(20," ") + " | " +
                info.filecount.toString().padLeft(5, " ") + " | " +
                info.loc.toString().padLeft(5," ") + " | "
            totalFiles += info.filecount
            totalLOC += info.loc
        }
    }

    output.println "    +----------------------+-------+-------+"
    output.println "    | Totals               | " + totalFiles.toString().padLeft(5, " ") + " | " + totalLOC.toString().padLeft(5, " ") + " | "
    output.println "    +----------------------+-------+-------+\n"

    println sw.toString()
}
