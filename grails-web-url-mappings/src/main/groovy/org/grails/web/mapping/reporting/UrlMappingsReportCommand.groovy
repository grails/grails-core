/*
 * Copyright 2014 original authors
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
package org.grails.web.mapping.reporting

import grails.build.logging.GrailsConsole
import grails.dev.commands.ApplicationContextCommand
import grails.web.mapping.UrlMappings
import grails.web.mapping.reporting.UrlMappingsRenderer
import groovy.transform.CompileStatic
import org.springframework.context.ConfigurableApplicationContext



/**
 * A {@link ApplicationContextCommand} that renders the URL mappings
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class UrlMappingsReportCommand implements ApplicationContextCommand {
    @Override
    boolean handle(ConfigurableApplicationContext applicationContext) {
        try {
            def urlMappings = applicationContext.getBean("grailsUrlMappingsHolder", UrlMappings)

            UrlMappingsRenderer renderer = new AnsiConsoleUrlMappingsRenderer()
            renderer.render(urlMappings.getUrlMappings().toList())
            return true
        } catch (Throwable e) {
            GrailsConsole.instance.error("Failed to render URL mappings: ${e.message}", e)
            return false
        }
    }

    @Override
    String getName() {
        "url-mappings-report"
    }
}
