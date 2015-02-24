/*
 * Copyright 2015 the original author or authors.
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

package org.grails.compiler.injection

import org.codehaus.groovy.control.SourceUnit
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.expression.spel.support.StandardTypeLocator

class GroovyEclipseCompilationHelper {
    static File resolveEclipseCompilationTargetDirectory(SourceUnit sourceUnit) {
        if (sourceUnit.getClass().name == 'org.codehaus.jdt.groovy.control.EclipseSourceUnit') {
            StandardEvaluationContext context = new StandardEvaluationContext()
            context.setTypeLocator(new StandardTypeLocator(sourceUnit.getClass().getClassLoader()))
            context.setRootObject(sourceUnit)
            return (File) new SpelExpressionParser().parseExpression("eclipseFile.workspace.root.getFolder(T(org.eclipse.jdt.core.JavaCore).create(eclipseFile.project).outputLocation.makeAbsolute()).rawLocation.makeAbsolute().toFile().absoluteFile").getValue(context)
        }
        return null
    }
}