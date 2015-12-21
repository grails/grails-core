/*
 * Copyright 2015 original authors
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
package org.grails.gradle.plugin.publishing
import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import grails.util.GrailsNameUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
/**
 * A plugin to setup publishing to Grails central repo
 *
 * @author Graeme Rocher
 * @since 3.1
 */
class GrailsCentralPublishGradlePlugin implements Plugin<Project> {

    String getErrorMessage(String missingSetting) {
        return """No '$missingSetting' was specified. Please provide a valid publishing configuration. Example:

grailsPublish {
    user = 'user'
    key = 'key'
    userOrg = 'my-company' // optional, otherwise published to personal bintray account
    repo = 'plugins' // optional, defaults to 'plugins'


    websiteUrl = 'http://foo.com/myplugin'
    license {
        name = 'Apache-2.0'
    }
    issueTrackerUrl = 'http://github.com/myname/myplugin/issues'
    vcsUrl = 'http://github.com/myname/myplugin'
    title = "My plugin title"
    desc = "My plugin description"
    developers = [johndoe:"John Doe"]
}

or

grailsPublish {
    user = 'user'
    key = 'key'
    githubSlug = 'foo/bar'
    license {
        name = 'Apache-2.0'
    }
    title = "My plugin title"
    desc = "My plugin description"
    developers = [johndoe:"John Doe"]
}

Your publishing user and key can also be placed in PROJECT_HOME/gradle.properties or USER_HOME/gradle.properties. For example:

bintrayUser=user
bintrayKey=key
grailsPortalUsername=myusername
grailsPortalPassword=mypassword

Or using environment variables:

BINTRAY_USER=user
BINTRAY_KEY=key
GRAILS_PORTAL_USERNAME=myusername
GRAILS_PORTAL_PASSWORD=mypassword
"""
    }

    @Override
    void apply(Project project) {
        project.plugins.apply(MavenPublishPlugin)

        def extensionContainer = project.extensions
        def taskContainer = project.tasks
        def publishExtension = extensionContainer.create("grailsPublish", GrailsPublishExtension)


        def bintraySiteUrl = project.hasProperty('websiteUrl') ? project.websiteUrl : ""
        def bintrayIssueTrackerUrl = project.hasProperty('issueTrackerUrl') ? project.issueTrackerUrl : ""
        def bintrayVcsUrl = project.hasProperty('vcsUrl') ? project.vcsUrl : ""
        def bintrayLicense = project.hasProperty('license') ? [project.license] : []
        def bintrayOrg = project.hasProperty('userOrg') ? project.userOrg : ''
        def signingPassphrase = System.getenv("SIGNING_PASSPHRASE") ?: project.hasProperty("signingPassphrase") ? project.signingPassphrase : ''
        def bintrayUser = System.getenv("BINTRAY_USER") ?: project.hasProperty("bintrayUser") ? project.bintrayUser : ''
        def bintrayKey = System.getenv("BINTRAY_KEY") ?: project.hasProperty("bintrayKey") ? project.bintrayKey : ''
        def sonatypeOssUsername = System.getenv("SONATYPE_USERNAME") ?: project.hasProperty("sonatypeOssUsername") ? project.sonatypeOssUsername : ''
        def sonatypeOssPassword = System.getenv("SONATYPE_PASSWORD") ?: project.hasProperty("sonatypeOssPassword") ? project.sonatypeOssPassword : ''
        def bintrayRepo = project.hasProperty('repo') ? project.repo : ''
        def bintrayDescription = project.hasProperty('desc') ? project.desc : ""

        def configurer = {
            BintrayExtension bintrayExtension = extensionContainer.findByType(BintrayExtension)

            if(publishExtension.mavenCentralSync) {
                bintrayExtension.pkg.version.mavenCentralSync.sync = true
            }
            if(publishExtension.gpgSign) {
                bintrayExtension.pkg.version.gpg.sign = true
            }
            if(publishExtension.user) {
                bintrayExtension.user = publishExtension.user
            }
            if(publishExtension.repo) {
                bintrayExtension.pkg.repo = publishExtension.repo
            }
            else if(!bintrayExtension.pkg.repo) {
                bintrayExtension.pkg.repo = getDefaultRepo()
            }
            if(publishExtension.desc) {
                bintrayExtension.pkg.desc = publishExtension.desc
            }
            else if(!bintrayExtension.pkg.desc) {
                bintrayExtension.pkg.desc = getDefaultDescription(project)
            }
            if(publishExtension.key) {
                bintrayExtension.key = publishExtension.key
            }
            if(publishExtension.userOrg) {
                bintrayExtension.pkg.userOrg = publishExtension.userOrg
            }
            else {
                bintrayExtension.pkg.userOrg = ''
            }

            if(publishExtension.websiteUrl) {
                bintrayExtension.pkg.websiteUrl = publishExtension.websiteUrl
            }
            else if(publishExtension.githubSlug) {
                bintrayExtension.pkg.websiteUrl = "https://github.com/$publishExtension.githubSlug"
            }

            if(publishExtension.vcsUrl) {
                bintrayExtension.pkg.vcsUrl = publishExtension.vcsUrl
            }
            else if(publishExtension.githubSlug) {
                bintrayExtension.pkg.vcsUrl = "https://github.com/$publishExtension.githubSlug"
            }

            if(publishExtension.issueTrackerUrl) {
                bintrayExtension.pkg.issueTrackerUrl = publishExtension.issueTrackerUrl
            }
            else if(publishExtension.githubSlug) {
                bintrayExtension.pkg.issueTrackerUrl = "https://github.com/$publishExtension.githubSlug/issues"
            }

            if(publishExtension.license?.name) {
                bintrayExtension.pkg.licenses = [publishExtension.license.name] as String[]
            }

            if(publishExtension.signingPassphrase) {
                bintrayExtension.pkg.version.gpg.passphrase = publishExtension.signingPassphrase
            }
            if(publishExtension.sonatypeOssUsername) {
                bintrayExtension.pkg.version.mavenCentralSync.user = publishExtension.sonatypeOssUsername
            }
            if(publishExtension.sonatypeOssPassword) {
                bintrayExtension.pkg.version.mavenCentralSync.password = publishExtension.sonatypeOssPassword
            }

            def pkgVersion = bintrayExtension.pkg.version.name
            if(!pkgVersion || pkgVersion == 'unspecified') {
                bintrayExtension.pkg.version.name = project.version
            }
        }

        def grailsCentralUsername = System.getenv('GRAILS_CENTRAL_USERNAME') ?: project.hasProperty('grailsCentralUsername') ? project.grailsCentralUsername : ''
        def grailsCentralPassword = System.getenv("GRAILS_CENTRAL_PASSWORD") ?: project.hasProperty('grailsCentralPassword') ? project.grailsCentralPassword : ''
        def grailsPortalUsername = System.getenv('GRAILS_PORTAL_USERNAME') ?: project.hasProperty('grailsPortalUsername') ? project.grailsPortalUsername : ''
        def grailsPortalPassword = System.getenv("GRAILS_PORTAL_PASSWORD") ?: project.hasProperty('grailsPortalPassword') ? project.grailsPortalPassword : ''

        project.plugins.apply(BintrayPlugin)

        project.afterEvaluate(configurer)

        project.publishing {
            publications {
                maven(MavenPublication) {
                    pom.withXml {
                        Node pomNode = asNode()
                        def extension = project.extensions.findByType(GrailsPublishExtension)
                        if(pomNode.dependencyManagement) {
                            pomNode.dependencyManagement[0].replaceNode {}
                        }

                        if(extension != null) {
                            pomNode.children().last() + {
                                def title = extension.title ?: project.name
                                delegate.name title
                                delegate.description extension.desc ?: title

                                def websiteUrl = extension.websiteUrl ?: extension.githubSlug ? "https://github.com/$extension.githubSlug" : ''
                                if(!websiteUrl) {
                                    throw new RuntimeException(getErrorMessage('websiteUrl'))
                                }

                                delegate.url websiteUrl


                                def license = extension.license
                                if(license != null) {

                                    def concreteLicense = GrailsPublishExtension.License.LICENSES.get(license.name)
                                    if(concreteLicense != null) {

                                        delegate.licenses {
                                            delegate.license {
                                                delegate.name concreteLicense.name
                                                delegate.url concreteLicense.url
                                                delegate.distribution concreteLicense.distribution
                                            }
                                        }
                                    }
                                    else if(license.name && license.url )  {
                                        delegate.licenses {
                                            delegate.license {
                                                delegate.name license.name
                                                delegate.url license.url
                                                delegate.distribution license.distribution
                                            }
                                        }
                                    }
                                }
                                else {
                                    throw new RuntimeException(getErrorMessage('license'))
                                }

                                if(extension.githubSlug) {
                                    delegate.scm {
                                        delegate.url "https://github.com/$extension.githubSlug"
                                        delegate.connection "scm:git@github.com:${extension.githubSlug}.git"
                                        delegate.developerConnection "scm:git@github.com:${extension.githubSlug}.git"
                                    }
                                    delegate.issueManagement {
                                        delegate.system "Github Issues"
                                        delegate.url "https://github.com/$extension.githubSlug/issues"
                                    }
                                }
                                else {
                                    if(extension.vcsUrl) {
                                        delegate.scm {
                                            delegate.url extension.vcsUrl
                                            delegate.connection "scm:$extension.vcsUrl"
                                            delegate.developerConnection "scm:$extension.vcsUrl"
                                        }
                                    }
                                    else {
                                        throw new RuntimeException(getErrorMessage('vcsUrl'))
                                    }

                                    if(extension.issueTrackerUrl) {
                                        delegate.issueManagement {
                                            delegate.system "Issue Tracker"
                                            delegate.url extension.issueTrackerUrl
                                        }
                                    }
                                    else {
                                        throw new RuntimeException(getErrorMessage('issueTrackerUrl'))
                                    }

                                }

                                if(extension.developers) {

                                    delegate.developers {
                                        for(entry in extension.developers.entrySet()) {
                                            delegate.developer {
                                                delegate.id entry.key
                                                delegate.name entry.value
                                            }
                                        }
                                    }
                                }
                                else {
                                    throw new RuntimeException(getErrorMessage('developers'))
                                }
                            }

                        }

                        // simply remove dependencies without a version
                        // version-less dependencies are handled with dependencyManagement
                        // see https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/8 for more complete solutions
                        pomNode.dependencies.dependency.findAll {
                            it.version.text().isEmpty()
                        }.each {
                            it.replaceNode {}
                        }
                    }
                    artifactId project.name
                    from project.components.java
                    def sourcesJar = taskContainer.findByName("sourcesJar")
                    if(sourcesJar != null) {
                        artifact sourcesJar
                    }
                    def javadocJar = taskContainer.findByName("javadocJar")
                    if(javadocJar != null) {
                        artifact javadocJar
                    }
                    def extraArtefact = getDefaultExtraArtifact(project)
                    if(extraArtefact) {
                        artifact extraArtefact
                    }
                }
            }


            if(grailsCentralUsername && grailsCentralPassword) {

                repositories {
                    maven {
                        credentials {
                            username grailsCentralUsername
                            password grailsCentralPassword
                        }

                        if(project.version.toString().endsWith('-SNAPSHOT')) {
                            url getDefaultGrailsCentralSnapshotRepo()
                        }
                        else {
                            url getDefaultGrailsCentralReleaseRepo()
                        }
                    }
                }
            }

        }


            // pluginInfo = [name:'..', group:'..', version:'..', isSnapshot: true/false, url: portalUrl]


        boolean isSnapshot = project.version.toString().endsWith('-SNAPSHOT')


        def portalNotify = project.tasks.create('notifyPluginPortal')
        portalNotify.dependsOn('generatePomFileForMavenPublication')
        portalNotify << {

            GrailsPublishExtension extension = project.extensions.findByType(GrailsPublishExtension)

            def portalUsername = grailsPortalUsername ?: extension?.portalUser
            def portalPassword = grailsPortalPassword ?: extension?.portalPassword

            if(portalUsername && portalPassword) {
                def targetUrl = "${extension.portalUrl}/${project.name}"
                URL endpoint = new URL(targetUrl)
                HttpURLConnection conn = endpoint.openConnection()
                conn.doOutput = true
                conn.instanceFollowRedirects = false
                conn.useCaches = false
                conn.requestMethod = 'PUT'

                String usernameAndPassword = "$extension.portalUser:$extension.portalPassword"

                def sw = new StringWriter()
                usernameAndPassword.bytes.encodeBase64().writeTo(sw)
                conn.setRequestProperty "Authorization", "Basic ${sw.toString()}"
                conn.setRequestProperty "Content-type", "application/json"
                conn.setRequestProperty "Accept", "application/json"

                String url = extension.centralRepoUrl
                OutputStream out

                try {
                    def data = """{"name":"${project.name}","group":"${project.group}","version":"${project.version}","isSnapshot":${isSnapshot},"url":"${url}"}""".toString()

                    conn.setRequestProperty( "Content-Length", Integer.toString( data.length() ));

                    out = conn.outputStream
                    // write the data
                    out << data
                    out.flush()

                    def result = conn.responseCode
                    if(result.toString().startsWith('2')) {
                        println "Notification successful."
                    }
                    else {
                        throw new RuntimeException( "(HTTP ${result}) An error occurred. " )
                    }

                } finally {
                    try {
                        out?.close()
                    } catch (Throwable e) {
                        // ignore
                    }
                }
            }
            else {
                throw new RuntimeException("No Grails 'portalUser' and 'portalPassword' specified")
            }
        }

        project.bintray {
            user = bintrayUser
            key = bintrayKey
            publications = ['maven']
            publish = true
            pkg {
                repo = bintrayRepo
                userOrg = bintrayOrg
                name = project.name
                desc = bintrayDescription
                websiteUrl = bintraySiteUrl
                issueTrackerUrl = bintrayIssueTrackerUrl
                vcsUrl = bintrayVcsUrl

                licenses = bintrayLicense
                publicDownloadNumbers = true
                version {
                    def artifactType = getDefaultArtifactType()
                    attributes = [(artifactType): "$project.group:$project.name"]
                    name = project.version
                    gpg {
                        sign = false
                        passphrase = signingPassphrase
                    }
                    mavenCentralSync {
                        sync = false
                        user = sonatypeOssUsername
                        password = sonatypeOssPassword
                    }
                }
            }
        }

        def installTask = taskContainer.findByName("install")
        def bintrayUploadTask = taskContainer.findByName('bintrayUpload')
        if(bintrayUploadTask != null) {
            taskContainer.create(name:"publish${GrailsNameUtils.getClassName(defaultClassifier)}", dependsOn: bintrayUploadTask)
        }
        if(installTask == null) {
            def publishToMavenLocal = taskContainer.findByName("publishToMavenLocal")
            if(publishToMavenLocal != null) {
                taskContainer.create(name:"install", dependsOn: publishToMavenLocal)
            }
        }
    }

    protected String getDefaultArtifactType() {
        "grails-$defaultClassifier"
    }

    protected String getDefaultGrailsCentralReleaseRepo() {
        "https://repo.grails.org/grails/plugins3-releases-local"
    }

    protected String getDefaultGrailsCentralSnapshotRepo() {
        "https://repo.grails.org/grails/plugins3-snapshots-local"
    }

    protected Map<String, String> getDefaultExtraArtifact(Project project) {
        [source: "${project.sourceSets.main.output.classesDir}/META-INF/grails-plugin.xml".toString(),
         classifier: getDefaultClassifier(),
         extension: 'xml']
    }

    protected String getDefaultClassifier() {
        "plugin"
    }

    protected String getDefaultDescription(Project project) {
        "Grails ${project.name} $defaultClassifier"
    }

    protected String getDefaultRepo() {
        "plugins"
    }
}
