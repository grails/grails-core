/*
 * Copyright 2003-2007 Graeme Rocher.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.compiler;

import org.codehaus.groovy.ant.Groovyc;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareInjectionOperation;

/**
 * Extends Groovyc and adds Grails' compiler extensions
 *
 * @author Graeme Rocher
 */
public class Grailsc extends Groovyc {

    @Override protected CompilationUnit makeCompileUnit() {
        CompilationUnit unit = super.makeCompileUnit();
        GrailsAwareInjectionOperation operation = new GrailsAwareInjectionOperation();
        unit.addPhaseOperation(operation, Phases.CANONICALIZATION);
        return unit;
    }
}
