/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.cli.support

import grails.build.logging.GrailsConsole
import grails.util.BuildSettings
import grails.util.PluginBuildSettings
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.GPathResult

import org.codehaus.groovy.grails.plugins.GrailsPluginInfo
import org.codehaus.groovy.grails.resolve.DependencyManager
import org.codehaus.groovy.tools.LoaderConfiguration

/**
 * Integrates UAA usage tracking with Grails.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
class UaaEnabler {

    private static final String MESSAGE = """##########################################################.
Grails would like to send information to VMware domains to improve your experience. We include anonymous usage information as part of these downloads.

The Grails team gathers anonymous usage information to improve your Grails experience, not for marketing purposes. The information is used to discover which Grails plugins are most popular and is published on the plugin portal.

We also use this information to help guide our roadmap, prioritizing the features and Grails plugins most valued by the community and enabling us to optimize the compatibility of technologies frequently used together.

Please see the Grails User Agent Analysis (UAA) Terms of Use at http://www.springsource.org/uaa/terms_of_use for more information on what information is collected and how such information is used. There is also an FAQ at http://www.springsource.org/uaa/faq for your convenience.

To consent to the Terms of Use, please enter 'Y'. Enter 'N' to indicate your do not consent and anonymous data collection will remain disabled.
##########################################################.
Enter Y or N:"""

    private static boolean enabled = false
    public static final int ONE_MINUTE = 1800
    GroovyClassLoader classLoader
    BuildSettings buildSettings
    PluginBuildSettings pluginBuildSettings

    UaaEnabler(BuildSettings buildSettings, PluginBuildSettings pluginBuildSettings) {

        this.buildSettings = buildSettings
        this.pluginBuildSettings = pluginBuildSettings
        final grailsHome = buildSettings.grailsHome
        final grailsVersion = buildSettings.grailsVersion
        def lc = new LoaderConfiguration()
        lc.setRequireMain(false)
        new File(grailsHome, "conf/uaa-starter.conf").withInputStream { InputStream it ->
            lc.configure(it)
        }

        this.classLoader = new GroovyClassLoader()
        final jarFiles = lc.getClassPathUrls()
        for(jar in jarFiles) {
            classLoader.addURL(jar)
        }
    }

    boolean isAvailable() {
        try {
            return  classLoader.loadClass("org.springframework.uaa.client.UaaServiceFactory") != null
        } catch (Throwable e) {
            return false
        }
    }

    static boolean isEnabled() {
        return enabled
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    void enable(boolean interactive) {
        def VersionHelper = classLoader.loadClass("org.springframework.uaa.client.VersionHelper")
        def UaaClient = classLoader.loadClass("org.springframework.uaa.client.protobuf.UaaClient")
        final uaaService = classLoader.loadClass("org.springframework.uaa.client.UaaServiceFactory").getUaaService()

        final privacyLevel = uaaService.getPrivacyLevel()
        if (!uaaService.isUaaTermsOfUseAccepted() && interactive) {
            // prompt for UAA choice
            if (privacyLevel.equals(UaaClient.Privacy.PrivacyLevel.UNDECIDED_TOU)) {
                while (true) {
                    GrailsConsole console = GrailsConsole.getInstance()
                    String selection = console.userInput(MESSAGE, ["y", "n"] as String[])
                    if ("y".equalsIgnoreCase(selection)) {
                        uaaService.setPrivacyLevel(UaaClient.Privacy.PrivacyLevel.ENABLE_UAA)
                        break
                    }
                    else if ("n".equalsIgnoreCase(selection)) {
                        uaaService.setPrivacyLevel(UaaClient.Privacy.PrivacyLevel.DECLINE_TOU)
                        break
                    }
                }
            }
        }

        if (isUaaAccepted(privacyLevel)) {
            Runnable r = new Runnable() {
                void run() {
                    try {
                        Thread.sleep(ONE_MINUTE)
                        final product = VersionHelper.getProduct("Grails", buildSettings.getGrailsVersion())
                        uaaService.registerProductUsage(product)

                        URL centralURL = new URL(DependencyManager.GRAILS_CENTRAL_PLUGIN_LIST)
                        InputStream input

                        try {
                            input = centralURL.openStream()
                            final GPathResult pluginList = new XmlSlurper().parse(input)

                            final GrailsPluginInfo[] pluginInfos = pluginBuildSettings.getPluginInfos(pluginBuildSettings.getPluginDirPath())
                            for (GrailsPluginInfo pluginInfo : pluginInfos) {
                                boolean registerUsage = false

                                if (buildSettings.getDefaultPluginSet().contains(pluginInfo.getName())) {
                                    registerUsage = true
                                }
                                else {
                                    final Object plugin = UaaIntegrationSupport.findPlugin(pluginList, pluginInfo.getName())
                                    if (plugin != null) {
                                        registerUsage = true
                                    }
                                }
                                if (registerUsage) {
                                    uaaService.registerFeatureUsage(product, VersionHelper.getFeatureUse(pluginInfo.getName(), pluginInfo.getVersion()))
                                }
                            }
                        } finally {
                            try {
                                if (input != null) input.close()
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    } catch (Exception e) {
                        // ignore, don't bother the user
                    }
                }
            }

            new Thread(r).start()
            enabled = true
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    private static boolean isUaaAccepted(privacyLevel) {
        return privacyLevel.equals(privacyLevel.ENABLE_UAA) ||
            privacyLevel.equals(privacyLevel.LIMITED_DATA)
    }
}
