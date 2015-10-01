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
     * The bintray user
     */
    String bintrayUser
    /**
     * The bintray key
     */
    String bintrayKey
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
    String license

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
}
