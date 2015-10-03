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
import org.gradle.api.Plugin
import org.gradle.api.Project
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
    license = 'APACHE 2.0'
    issueTrackerUrl = 'http://github.com/myname/myplugin/issues'
    vcsUrl = 'http://github.com/myname/myplugin'
    desc = "My plugin description"
}

or

grailsPublish {
    user = 'user'
    key = 'key'
    githubSlug = 'foo/bar'
    license = 'APACHE 2.0'
}

The values can also be placed in PROJECT_HOME/gradle.properties or USER_HOME/gradle.properties
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
            else if(!bintrayExtension.user) {
                throw new RuntimeException(getErrorMessage("user"))
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
            else if(!bintrayExtension.key) {
                throw new RuntimeException(getErrorMessage("key"))
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
            else if(!bintrayExtension.pkg.websiteUrl) {
                throw new RuntimeException(getErrorMessage("websiteUrl"))
            }

            if(publishExtension.vcsUrl) {
                bintrayExtension.pkg.vcsUrl = publishExtension.vcsUrl
            }
            else if(publishExtension.githubSlug) {
                bintrayExtension.pkg.websiteUrl = "https://github.com/$publishExtension.githubSlug"
            }
            else if(!bintrayExtension.pkg.vcsUrl) {
                throw new RuntimeException(getErrorMessage("vcsUrl"))
            }

            if(publishExtension.issueTrackerUrl) {
                bintrayExtension.pkg.issueTrackerUrl = publishExtension.issueTrackerUrl
            }
            else if(publishExtension.githubSlug) {
                bintrayExtension.pkg.websiteUrl = "https://github.com/$publishExtension.githubSlug/issues"
            }
            else if(!bintrayExtension.pkg.issueTrackerUrl) {
                throw new RuntimeException(getErrorMessage("issueTrackerUrl"))
            }

            if(publishExtension.license) {
                bintrayExtension.pkg.licenses = [publishExtension.license] as String[]
            }
            else if(!bintrayExtension.pkg.licenses) {
                throw new RuntimeException(getErrorMessage("license"))
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
        }


        project.plugins.apply(BintrayPlugin)

        project.afterEvaluate(configurer)

        project.publishing {
            publications {
                maven(MavenPublication) {
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

            def grailsCentralUsername = System.getenv('GRAILS_CENTRAL_USERNAME') ?: project.hasProperty('grailsPluginsUsername') ? project.grailsPluginsUsername : ''
            def grailsCentralPassword = System.getenv("GRAILS_CENTRAL_PASSWORD") ?: project.hasProperty('grailsPluginsPassword') ? project.grailsPluginsPassword : ''

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
        project.bintray {
            user = bintrayUser
            key = bintrayKey
            publications = ['maven']
            publish = true
            pkg {
                repo = bintrayRepo
                userOrg = bintrayOrg
                name = "$project.group:$project.name"
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
        if(installTask == null) {

            taskContainer.create(name:"install", dependsOn: taskContainer.withType(PublishToMavenLocal))
        }
    }

    protected String getDefaultArtifactType() {
        'grails-plugin'
    }

    protected String getDefaultGrailsCentralReleaseRepo() {
        "https://repo.grails.org/grails/plugins3-releases-local"
    }

    protected String getDefaultGrailsCentralSnapshotRepo() {
        "https://repo.grails.org/grails/plugins3-snapshots-local"
    }

    protected Map<String, String> getDefaultExtraArtifact(Project project) {
        [source: "${project.sourceSets.main.output.classesDir}/META-INF/grails-plugin.xml".toString(),
         classifier: "plugin",
         extension: 'xml']
    }

    protected String getDefaultDescription(Project project) {
        "Grails ${project.name} plugin"
    }

    protected String getDefaultRepo() {
        "plugins"
    }
}
