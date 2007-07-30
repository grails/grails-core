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

import groovy.lang.GroovyClassLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;


import java.security.CodeSource;

/**
 * A class loader that is aware of Groovy sources and injection operations
 *
 * @author Graeme Rocher
 * @since 0.6
 *        <p/>
 *        Created: Jul 27, 2007
 *        Time: 8:57:15 AM
 */
public class GrailsAwareClassLoader extends GroovyClassLoader {

    public GrailsAwareClassLoader() {
    }

    public GrailsAwareClassLoader(ClassLoader loader) {
        super(loader);
    }

    public GrailsAwareClassLoader(GroovyClassLoader parent) {
        super(parent);
    }

    public GrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration config, boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath);
    }

    public GrailsAwareClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config);
    }

    private static final Log LOG = LogFactory.getLog(GrailsAwareClassLoader.class);

    private ClassInjector[] classInjectors = new ClassInjector[0];
    
    public void setClassInjectors(ClassInjector[] classInjectors) {
        this.classInjectors = classInjectors;
    }

    /**
    * @see groovy.lang.GroovyClassLoader#createCompilationUnit(org.codehaus.groovy.control.CompilerConfiguration, java.security.CodeSource)
    */
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit cu = super.createCompilationUnit(config, source);
        cu.addPhaseOperation(new GrailsAwareInjectionOperation(getResourceLoader(), classInjectors), Phases.CONVERSION);
        return cu;
    }


}
