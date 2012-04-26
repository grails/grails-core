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

package org.codehaus.groovy.grails.test.compiler;

import grails.test.mixin.TestFor;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.grails.compiler.Grailsc;

/**
 * Extended compiler for automatically applying the @TestFor and @Mock annotations to tests by convention.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsTestCompiler extends Grailsc {

    public GrailsTestCompiler() {
        setEncoding("UTF-8");
    }

    @Override
    protected CompilationUnit makeCompileUnit() {
        ImportCustomizer importCustomizer = new ImportCustomizer();
        importCustomizer.addStarImports("grails.test.mixin");
        importCustomizer.addStarImports("org.junit");
        importCustomizer.addStaticStars("groovy.util.GroovyTestCase");

        ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(TestFor.class);

        configuration.addCompilationCustomizers(importCustomizer, astTransformationCustomizer);

        return super.makeCompileUnit();
    }
}
