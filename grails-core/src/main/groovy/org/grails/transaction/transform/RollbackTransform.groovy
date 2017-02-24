/*
 * Copyright 2012 the original author or authors.
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
package org.grails.transaction.transform

import grails.transaction.Rollback
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * The transform class for {@link grails.transaction.Rollback}
 *
 * @deprecated Use org.grails.datastore.gorm.transactions.transform.RollbackTransform instead
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
@Deprecated
class RollbackTransform extends TransactionalTransform {

    public static final ClassNode MY_TYPE = new ClassNode(Rollback)

    @Override
    protected String getTransactionTemplateMethodName() {
        return "executeAndRollback"
    }

    @Override
    protected boolean isTransactionAnnotation(AnnotationNode annotationNode) {
        MY_TYPE.equals(annotationNode.getClassNode())
    }
}
