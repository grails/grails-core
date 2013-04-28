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
 * Gant script that handles upgrading of a Grails applications
 *
 * @author Graeme Rocher
 * @author Sergey Nebolsin
 *
 * @since 0.4
 */

import grails.util.Metadata

includeTargets << grailsScript("_GrailsClean")
includeTargets << grailsScript("_GrailsPackage")
includeTargets << grailsScript("_PluginDependencies")

UNMODIFIED_CHECKSUMS = [indexgsp:['e9f4d3450ba02fe92d55f4ae4b53dee8', 'e9f4d3450ba02fe92d55f4ae4b53dee8', '77f5ed5c2fca586a9ff1dc8e7beeb85b', '5313f072b2ed10129a446d5f648d8b41'],
                        errorgsp:['473b673fb3f04a60412ace1b7bc12a8c', '473b673fb3f04a60412ace1b7bc12a8c', '473b673fb3f04a60412ace1b7bc12a8c', '473b673fb3f04a60412ace1b7bc12a8c'],
                        maincss:['612301d27b1d5d6f670cc98905376f59','612301d27b1d5d6f670cc98905376f59', '820b415fc6e53b156b68e5a01fa5677e', '820b415fc6e53b156b68e5a01fa5677e']]

target(upgrade: "main upgrade target") {

    depends(createStructure, parseArguments)

    System.properties.put('runningGrailsUpgrade', 'true')
    boolean force = argsMap.force || !isInteractive ?: false

    if (appGrailsVersion != grailsVersion) {
        def gv = appGrailsVersion == null ? "pre-0.5" : appGrailsVersion
        event("StatusUpdate", ["NOTE: Your application currently expects grails version [$gv], " +
              "this target will upgrade it to Grails ${grailsVersion}"])
    }

    if (!force) {
        ant.input(message: """
        WARNING: This target will upgrade an older Grails application to ${grailsVersion}.
        Are you sure you want to continue?
                   """,
                    validargs: "y,n",
                    addproperty: "grails.upgrade.warning")

        def answer = ant.antProject.properties."grails.upgrade.warning"

        if (answer == "n") exit(0)

        if ((grailsVersion.startsWith("1.0")) &&
                !(['utf-8', 'us-ascii'].contains(System.getProperty('file.encoding')?.toLowerCase()))) {
            ant.input(message: """
            WARNING: This version of Grails requires all source code to be encoded in UTF-8.
            Your system file encoding indicates that your source code may not be saved in UTF-8.
            You can re-encode your source code manually after upgrading, but if you have used any
            non-ASCII chars in your source or GSPs your application may not operate correctly until
            you re-encode the files as UTF-8.

            Are you sure you want to upgrade your project now?
                       """,
                             validargs: "y,n",
                             addproperty: "grails.src.encoding.warning")
            answer = ant.antProject.properties."grails.src.encoding.warning"
            if (answer == "n") exit(0)
        }
    }

    clean()

    def coreTaglibs = new File("${basedir}/plugins/core")

    ant.delete(dir: "${coreTaglibs}", failonerror: false)

    ant.sequential {
        def testDir = "${basedir}/grails-tests"
        if (new File("${testDir}/CVS").exists()) {
            println """
WARNING: Your Grails tests directory '${testDir}' is currently under CVS control so the upgrade script cannot
move it to the new location of '${basedir}/test/integration'. Please move the directory using the relevant CVS commands."""
        }
        else if (new File("${testDir}/.svn").exists()) {
            println """
WARNING: Your Grails tests directory '${testDir}' is currently under SVN control so the upgrade script cannot
move it to the new location of '${basedir}/test/integration'. Please move the directory using the relevant SVN commands."""
        }
        else {
            if (new File(testDir).exists()) {
                move(todir: "${basedir}/test/integration") {
                    fileset(dir: testDir, includes: "**")
                }
                delete(dir: testDir)
            }
        }
        delete(dir: "${basedir}/tmp", failonerror: false)

        if (!new File("$grailsHome/src/war").exists()) {
            if (new File("${grailsHome}/grails-resources").exists()) {
                grailsHome = new File("${grailsHome}/grails-resources")
            }
        }

        copy(todir: "${basedir}/web-app") {
            fileset(dir: "${grailsHome}/src/war") {
                include(name: "**/**")
                exclude(name: "WEB-INF/**")
                present(present: "srconly", targetdir: "${basedir}/web-app")
            }
        }
        copy(file: "${grailsHome}/src/war/WEB-INF/sitemesh.xml",
             tofile: "${basedir}/web-app/WEB-INF/sitemesh.xml", overwrite: true)
        copy(file: "${grailsHome}/src/war/WEB-INF/applicationContext.xml",
             tofile: "${basedir}/web-app/WEB-INF/applicationContext.xml", overwrite: true)

        if (!isPluginProject) {
            // Install application-only files if needed, exact "one file only" matches
            ['Config.groovy'].each() {template ->
                if (!new File(baseFile, '/grails-app/conf').listFiles().find { it.name.equals(template) }) {
                    copyGrailsResource("${basedir}/grails-app/conf/${template}", grailsResource("src/grails/grails-app/conf/${template}"))
                }
            }

            ['BuildConfig.groovy'].each() {template ->
                if (!new File(baseFile, '/grails-app/conf').listFiles().find { it.name.equals(template) }) {
                    copyGrailsResource("${basedir}/grails-app/conf/${template}", grailsResource("src/grails/grails-app/conf/${template}"))
                }
            }

            // Install application-only files if needed, with suffix matching
            ['DataSource.groovy'].each() {template ->
                if (!new File(baseFile, '/grails-app/conf').listFiles().find { it.name.startsWith(template) }) {
                    copyGrailsResource("${basedir}/grails-app/conf/${template}", grailsResource("src/grails/grails-app/conf/${template}"))
                }
            }
        }

        // Both applications and plugins can have UrlMappings, but only install default if there is nothing already
        ['UrlMappings.groovy'].each() {template ->
            if (!new File(baseFile, '/grails-app/conf').listFiles().find { it.name.endsWith(template) }) {
                copyGrailsResource("${basedir}/grails-app/conf/${template}", grailsResource("src/grails/grails-app/conf/${template}"))
            }
        }

        def dsFile = new File(baseFile, "grails-app/conf/DataSource.groovy")
        if (dsFile.exists() && argsMap.'update-data-source') {
            replace file:dsFile, token:"jdbc:hsqldb:mem:devDB", value:"jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
            replace file:dsFile, token:"jdbc:hsqldb:mem:testDb",value: "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
            replace file:dsFile, token:"org.hsqldb.jdbcDriver", value:"org.h2.Driver"
        }
        // if Config.groovy exists and it does not contain values for
        // grails.views.default.codec or grails.views.gsp.encoding then
        // add reasonable defaults for them
        def configFile = new File(baseFile, '/grails-app/conf/Config.groovy')
        if (configFile.exists()) {
            def configText = configFile.text
            configFile.withWriterAppend {
                if (!configText.find(/grails\s*\{\s*views\s*\{\s*gsp\s*\{/)) {
                    it << """
// Uncomment and edit the following lines to start using Grails encoding & escaping improvements

/* remove this line 
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside ${}
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */
"""
                }
            }
        }

        if (new File("${basedir}/spring").exists()) {
            move(file: "${basedir}/spring", todir: "${basedir}/grails-app/conf")
        }
        if (new File("${basedir}/hibernate").exists()) {
            move(file: "${basedir}/hibernate", todir: "${basedir}/grails-app/conf")
        }

        def appKey = baseName.replaceAll(/\s/, '.').toLowerCase()

        replace(dir: "${basedir}/web-app/WEB-INF", includes: "**/*.*",
                token: "@grails.project.key@", value: "${appKey}")

        copy(todir: "${basedir}/web-app/WEB-INF/tld", overwrite: true) {
            fileset(dir: "${grailsHome}/src/war/WEB-INF/tld/${servletVersion}")
            fileset(dir: "${grailsHome}/src/war/WEB-INF/tld", includes: "spring.tld")
            fileset(dir: "${grailsHome}/src/war/WEB-INF/tld", includes: "grails.tld")
        }
        touch(file: "${basedir}/grails-app/i18n/messages.properties")
    }

    Metadata.current.setGrailsVersion grailsVersion
    Metadata.current.persist()

    // proceed with plugin-specific upgrade logic contained in 'scripts/_Upgrade.groovy' under every plugin's root
    def pluginDirs = pluginSettings.getPluginDirectories()
    for (pluginDir in pluginDirs) {
        def upgradeScript = new File(pluginDir.getFile(), "scripts/_Upgrade.groovy")
        if (upgradeScript.exists()) {
            event("StatusUpdate", ["Executing ${pluginDir.getFilename()} plugin upgrade script"])
            // instrumenting plugin scripts adding 'pluginBasedir' variable
            def instrumentedUpgradeScript = "def pluginDir = '${pluginDir}'\n" + upgradeScript.text
            // we are using text form of script here to prevent Gant caching
            includeTargets << instrumentedUpgradeScript
        }
    }

    event("StatusUpdate", ["Please make sure you view the README for important information about changes to your source code."])

    event("StatusFinal", ["Project upgraded"])
}

target("default": "Upgrades a Grails application from a previous version of Grails") {
    depends(upgrade)
}
