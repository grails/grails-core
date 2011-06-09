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
package org.codehaus.groovy.grails.cli.support;

import grails.build.logging.GrailsConsole;
import grails.util.BuildSettings;
import grails.util.PluginBuildSettings;
import groovy.util.slurpersupport.GPathResult;
import org.apache.ivy.plugins.resolver.ChainResolver;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.resolve.GrailsRepoResolver;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.uaa.client.UaaService;
import org.springframework.uaa.client.UaaServiceFactory;
import org.springframework.uaa.client.VersionHelper;
import org.springframework.uaa.client.protobuf.UaaClient;
import org.springframework.util.ClassUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;

/**
 * Integrates UAA usage tracking with Grails
 *
 * @author Graeme Rocher
 * @since 1.4
 */
public class UaaIntegration {

    private static final String MESSAGE = "##########################################################.\n"
           + "Grails would like to send information to VMware domains to improve your experience. We include anonymous usage information as part of these downloads.\n"
           + "\n"
           + "The Grails team gathers anonymous usage information to improve your Grails experience, not for marketing purposes. The information is used to discover which Grails plugins are most popular and is published on the plugin portal.\n"
           + "\n"
           + "We also use this information to help guide our roadmap, prioritizing the features and Grails plugins most valued by the community and enabling us to optimize the compatibility of technologies frequently used together.\n"
           + "\n"
           + "Please see the Grails User Agent Analysis (UAA) Terms of Use at http://www.springsource.org/uaa/terms_of_use for more information on what information is collected and how such information is used. There is also an FAQ at http://www.springsource.org/uaa/faq for your convenience.\n"
           + "\n"
           + "To consent to the Terms of Use, please enter 'Y'. Enter 'N' to indicate your do not consent and anonymous data collection will remain disabled.\n"
           + "##########################################################.\n"
           + "Enter Y or N:";
    public static final int ONE_MINUTE = 180000;


    public static boolean isAvailable() {
        return ClassUtils.isPresent("org.springframework.uaa.client.UaaServiceFactory", UaaIntegration.class.getClassLoader());
    }

    public static void enable(final BuildSettings settings, final PluginBuildSettings pluginSettings, boolean interactive) {
        final UaaService uaaService = UaaServiceFactory.getUaaService();

        final UaaClient.Privacy.PrivacyLevel privacyLevel = uaaService.getPrivacyLevel();
        if (!uaaService.isUaaTermsOfUseAccepted() && interactive) {
            // prompt for UAA choice
            if (privacyLevel.equals(UaaClient.Privacy.PrivacyLevel.UNDECIDED_TOU)) {
                while (true) {
                    GrailsConsole console = GrailsConsole.getInstance();
                    String selection = console.userInput(MESSAGE, new String[]{"y", "n"});
                    if ("y".equalsIgnoreCase(selection)) {
                        uaaService.setPrivacyLevel(UaaClient.Privacy.PrivacyLevel.ENABLE_UAA);
                        break;
                    }
                    else if ("n".equalsIgnoreCase(selection)) {
                        uaaService.setPrivacyLevel(UaaClient.Privacy.PrivacyLevel.DECLINE_TOU);
                        break;
                    }

                }
            }
        }
        else if (isUaaAccepted(privacyLevel)) {
            Runnable r = new Runnable() {
                public void run() {
                    final UaaClient.Product product = VersionHelper.getProduct("Grails", settings.getGrailsVersion());
                    uaaService.registerProductUsage(product);

                    final ChainResolver chainResolver = settings.getDependencyManager().getChainResolver();
                    GrailsRepoResolver centralRepo = findCentralRepoResolver(chainResolver);
                    if (centralRepo != null) {

                        final GPathResult pluginList = centralRepo.getPluginList(new File(settings.getGrailsWorkDir() + "/plugin-list-" + centralRepo.getName() + ".xml"));

                        final GrailsPluginInfo[] pluginInfos = pluginSettings.getPluginInfos(pluginSettings.getPluginDirPath());
                        for (GrailsPluginInfo pluginInfo : pluginInfos) {
                            boolean registerUsage = false;

                            if (settings.getDefaultPluginSet().contains(pluginInfo.getName())) {
                                registerUsage = true;
                            }
                            else {
                                final Object plugin = UaaIntegrationSupport.findPlugin(pluginList, pluginInfo.getName());
                                if (plugin != null) {
                                    registerUsage = true;
                                }
                            }
                            if (registerUsage) {
                                uaaService.registerFeatureUsage(product, VersionHelper.getFeatureUse(pluginInfo.getName(), pluginInfo.getVersion()));
                            }
                        }
                    }
                }
            };

            ConcurrentTaskScheduler scheduler = new ConcurrentTaskScheduler();
            scheduler.schedule(r, new Date(System.currentTimeMillis() + ONE_MINUTE));
        }
    }

    private static GrailsRepoResolver findCentralRepoResolver(ChainResolver chainResolver) {
        @SuppressWarnings("unchecked")
        final List<Object> resolvers = chainResolver.getResolvers();
        for (Object resolver : resolvers) {
            if (resolver instanceof GrailsRepoResolver) {
                final GrailsRepoResolver grailsRepoResolver = (GrailsRepoResolver) resolver;
                final String resolverName = grailsRepoResolver.getName();
                if (resolverName != null && resolverName.equals("grailsCentral")) {
                    return grailsRepoResolver;
                }
            }
        }
        return null;
    }

    private static boolean isUaaAccepted(UaaClient.Privacy.PrivacyLevel privacyLevel) {
        return privacyLevel.equals(UaaClient.Privacy.PrivacyLevel.ENABLE_UAA) ||
               privacyLevel.equals(UaaClient.Privacy.PrivacyLevel.LIMITED_DATA);
    }
}
