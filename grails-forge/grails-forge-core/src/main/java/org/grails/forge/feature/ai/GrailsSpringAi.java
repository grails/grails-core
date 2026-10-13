/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.grails.forge.feature.ai;

import jakarta.inject.Singleton;
import org.grails.forge.application.ApplicationType;
import org.grails.forge.application.generator.GeneratorContext;
import org.grails.forge.build.dependencies.Dependency;
import org.grails.forge.feature.Category;
import org.grails.forge.feature.Feature;
import org.grails.forge.util.VersionInfo;

/**
 * Adds the Grails Spring AI plugin, which integrates Spring AI, together with its testing support. A provider is
 * chosen with one of the {@code spring-ai-*} features or by adding a Spring AI starter by hand.
 */
@Singleton
public class GrailsSpringAi implements Feature {

    public static final String FEATURE_NAME = "grails-spring-ai";

    @Override
    public String getName() {
        return FEATURE_NAME;
    }

    @Override
    public String getTitle() {
        return "Grails Spring AI";
    }

    @Override
    public String getDescription() {
        return "Integrates Spring AI: a chat client over the provider of your choice, chat responses streamed " +
                "from controller actions, and scripted models for tests.";
    }

    @Override
    public void apply(GeneratorContext generatorContext) {
        generatorContext.addDependency(Dependency.builder()
                .groupId("org.apache.grails")
                .artifactId("grails-spring-ai")
                .implementation());
        generatorContext.addDependency(Dependency.builder()
                .groupId("org.apache.grails")
                .artifactId("grails-testing-support-ai")
                .testImplementation());
    }

    @Override
    public boolean supports(ApplicationType applicationType) {
        return true;
    }

    @Override
    public String getCategory() {
        return Category.AI;
    }

    @Override
    public String getDocumentation() {
        return "https://grails.apache.org/docs/" + VersionInfo.getDocumentationVersion() + "/guide/usingAi.html";
    }

    @Override
    public String getThirdPartyDocumentation() {
        return "https://docs.spring.io/spring-ai/reference/";
    }

}
