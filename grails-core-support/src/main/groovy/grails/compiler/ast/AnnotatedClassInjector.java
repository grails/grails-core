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
package grails.compiler.ast;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.SourceUnit;

/**
 * Injector applied to annotated (local transform) entities
 *
 * @author Graeme Rocher
 * @since 2.2.3
 */
public interface AnnotatedClassInjector {

    /**
     * Performs injection on an annotated entity
     * @param source The source unit
     * @param classNode The class node
     */
    void performInjectionOnAnnotatedClass(SourceUnit source, GeneratorContext context, ClassNode classNode);

}
