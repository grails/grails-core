/*
 * Copyright 2004-2005 the original author or authors.
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

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;

import java.net.URL;

/**
 * An interface that when implemented allows additional properties to be injected into Grails
 * classes at compile time (ie when they are loaded by the GroovyClassLoader)
 * 
 * @author Graeme Rocher
 *
 * @since 0.2
 * 
 * Created: 20th June 2006
 */
public interface ClassInjector {

	/**
	 * Method that handles injection of properties, methods etc. into a class
	 * 
	 * @param source The source unit
	 * @param context The generator context
	 * @param classNode The ClassNode instance
	 */
	void performInjection(SourceUnit source, GeneratorContext context, ClassNode classNode);

    /**
     * Returns whether this injector should inject
     *
     * @param url The URL of the source file
     * @return True if injection should occur
     */
    boolean shouldInject(URL url);
}
