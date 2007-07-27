/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.compiler.injection;

import groovy.lang.GroovyResourceLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.commons.GrailsResourceUtils;
import org.springframework.util.Assert;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A Groovy compiler injection operation that uses a specified array of ClassInjector instances to
 * attempt AST injection.
 *
 * @author Graeme Rocher
 * @since 0.6
 *
 *        <p/>
 *        Created: Jul 27, 2007
 *        Time: 10:57:35 AM
 */
public class GrailsAwareInjectionOperation extends CompilationUnit.PrimaryClassNodeOperation  {

    private static final Log LOG = LogFactory.getLog(GrailsAwareInjectionOperation.class);

    private GroovyResourceLoader grailsResourceLoader;
    private ClassInjector[] classInjectors = new ClassInjector[0];

    public GrailsAwareInjectionOperation(GroovyResourceLoader resourceLoader, ClassInjector[] injectors) {
        Assert.notNull(resourceLoader, "The argument [resourceLoader] is required!");
        this.grailsResourceLoader = resourceLoader;
        if(injectors != null) {
            this.classInjectors = injectors;
        }
    }

    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
            for (int i = 0; i < classInjectors.length; i++) {
                ClassInjector classInjector = classInjectors[i];
                URL url;

                try {
                    if(GrailsResourceUtils.isGrailsPath(source.getName())) {
                        url = grailsResourceLoader.loadGroovySource(GrailsResourceUtils.getClassName(source.getName()));
                    }
                    else {
                        url = grailsResourceLoader.loadGroovySource(source.getName());
                    }

                    if(classInjector.shouldInject(url)) {
                        classInjector.performInjection(source, context, classNode);
                    }
                } catch (MalformedURLException e) {
                    LOG.error("Error loading URL during addition of compile time properties: " + e.getMessage(),e);
                    throw new CompilationFailedException(Phases.CONVERSION,source,e);
                }

            }
        }

    }