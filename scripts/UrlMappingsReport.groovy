/*
 * Copyright 2013 the original author or authors.
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
 * Generates a report of the URL mappings in a project
 *
 * @author Graeme Rocher
 *
 * @since 2.3
 */
import org.codehaus.groovy.grails.commons.UrlMappingsArtefactHandler

includeTargets << grailsScript("_GrailsBootstrap")

target(urlMappingsReport:"Produces a URL mappings report for the current Grails application") {
    depends(classpath, compile, loadApp)

    def mappings = grailsApp.getArtefacts(UrlMappingsArtefactHandler.TYPE)
    def evaluator = classLoader.loadClass("org.codehaus.groovy.grails.web.mapping.DefaultUrlMappingEvaluator").newInstance(classLoader.loadClass('org.springframework.mock.web.MockServletContext').newInstance())
    def allMappings = []

    for(m in mappings) {
        List grailsClassMappings
        if (Script.isAssignableFrom(m.getClazz())) {
            grailsClassMappings = evaluator.evaluateMappings(m.getClazz())
        }
        else {
            grailsClassMappings = evaluator.evaluateMappings(m.getMappingsClosure())
        }
        allMappings.addAll(grailsClassMappings)
    }

    grailsConsole.addStatus "URL Mappings Configured for Application"
    grailsConsole.addStatus "---------------------------------------"
    def renderer = classLoader.loadClass("org.codehaus.groovy.grails.web.mapping.reporting.AnsiConsoleUrlMappingsRenderer").newInstance()
    // renderer.isAnsiEnabled=false
    println()
    renderer.render(allMappings)
}

setDefaultTarget(urlMappingsReport)
