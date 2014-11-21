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
package org.grails.cli.profile.commands.templates

import groovy.text.GStringTemplateEngine
import groovy.text.Template
import groovy.transform.CompileStatic
import org.grails.cli.profile.ExecutionContext
import org.grails.cli.profile.commands.io.FileSystemInteraction
import org.grails.io.support.DefaultResourceLoader
import org.grails.io.support.Resource
import org.grails.io.support.ResourceLoader


/**
 * Interface for classes that can render templates
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class TemplateRenderer  {

    ExecutionContext executionContext
    @Delegate FileSystemInteraction fileSystemInteraction
    private Map<String, Template> templatecCache = [:]

    TemplateRenderer(ExecutionContext executionContext, ResourceLoader resourceLoader = new DefaultResourceLoader()) {
        this.executionContext = executionContext
        this.fileSystemInteraction = new FileSystemInteraction(executionContext, resourceLoader)
    }

    /**
     * Render with the given named arguments
     *
     * @param namedArguments The named arguments are 'template', 'destination' and 'model'
     */
    void render(Map<String, Object> namedArguments) {
        if(namedArguments?.template && namedArguments?.destination) {
            render resource(namedArguments.template), file(namedArguments.destination), namedArguments.model ? (Map)namedArguments.model : Collections.emptyMap()
        }
    }

    /**
     * Render the given template to the given destination
     *
     * @param template The contents of the template
     * @param destination The destination
     * @param model The model
     */
    void render(CharSequence template, File destination, Map model = Collections.emptyMap()) {
        if(template && destination) {
            def templateEngine = new GStringTemplateEngine()
            def t = templateEngine.createTemplate(template.toString())
            writeTemplateToDestination(t, model, destination)
        }
    }



    /**
     * Render the given template to the given destination
     *
     * @param template The template
     * @param destination The destination
     * @param model The model
     */
    void render(File template, File destination, Map model = Collections.emptyMap()) {
        if(template && destination) {
            Template t = templatecCache[template.absolutePath]
            if(t == null) {
                def templateEngine = new GStringTemplateEngine()
                t = templateEngine.createTemplate(template)
            }
            writeTemplateToDestination(t, model, destination)
        }
    }

    /**
     * Render the given template to the given destination
     *
     * @param template The template
     * @param destination The destination
     * @param model The model
     */
    void render(Resource template, File destination, Map model = Collections.emptyMap()) {
        if(template && destination) {
            Template t = templatecCache[template.filename]
            if(t == null) {

                def templateEngine = new GStringTemplateEngine()
                def reader = new InputStreamReader(template.inputStream, "UTF-8")
                try {
                    t = templateEngine.createTemplate(reader)
                } finally {
                    try {
                        reader.close()
                    } catch (e) {
                        // ignore
                    }
                }
            }
            if(t != null) {
                writeTemplateToDestination(t, model, destination)
            }
        }
    }

    Iterable<Resource> templates(String pattern) {
        Collection<Resource> resList = []
        resList.addAll( resources(pattern) )
        resList.addAll( resources("classpath*:META-INF/templates/$pattern"))
        return resList.unique()
    }


    private static void writeTemplateToDestination(Template template, Map model, File destination) {
        destination.parentFile.mkdirs()
        destination.withWriter { Writer w ->
            template.make(model).writeTo(w)
        }
    }
}