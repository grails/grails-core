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


/**
 * @author Graeme Rocher
 * @since 3.1
 */
class GrailsPublishExtension {

    /**
     * The slug from github
     */
    String githubSlug
    /**
     * The the publishing user
     */
    String user
    /**
     * The the publishing key
     */
    String key

    /**
     * The username for the plugin portal
     */
    String portalUser

    /**
     * The password for the plugin portal
     */
    String portalPassword

    /**
     * The plugin endpoint for updating plugins
     */
    String portalUrl = "https://grails.org/plugin"

    /**
     * The location of the Grails central repository
     */
    String centralRepoUrl = "http://repo.grails.org/grails/core"
    /**
     * The website URL of the plugin
     */
    String websiteUrl
    /**
     * The source control URL of the plugin
     */
    String vcsUrl
    /**
     * The license of the plugin
     */
    License license = new License()

    /**
     * The developers of the plugin
     */
    Map<String, String> developers = [:]

    /**
     * Title of the plugin, defaults to the project name
     */
    String title

    /**
     * Description of the plugin
     */
    String desc
    /**
     * THe organisation on bintray
     */
    String userOrg

    /**
     * THe repository on bintray
     */
    String repo
    /**
     * The issue tracker URL
     */
    String issueTrackerUrl
    /**
     * Whether to GPG sign
     */
    boolean gpgSign = false

    /**
     * The passphrase to sign, only required if `gpgSign == true`
     */
    String signingPassphrase
    /**
     * Whether to sync to Maven central
     */
    boolean mavenCentralSync = false

    /**
     * Username for maven central
     */
    String sonatypeOssUsername

    /**
     * Password for maven central
     */
    String sonatypeOssPassword

    License getLicense() {
        return license
    }

    String getPortalUser() {
        return portalUser ?: user
    }

    void setPortalUser(String portalUser) {
        this.portalUser = portalUser
    }

    void setLicense(License license) {
        this.license = license
    }

    void setLicense(String license) {
        this.license.name = license
    }

    static class License {
        String name
        String url
        String distribution = 'repo'
    }
}
