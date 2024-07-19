/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.asm;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

/**
 * {@link AnnotationVisitor} to recursively visit annotation attributes.
 *
 * <p>Note: This class was ported to Grails 7 from Spring Framework 5.3 as it was
 * removed in Spring 6 without a public replacement.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1.1
 * @deprecated As of Spring Framework 5.2, this class and related classes in this
 * package have been replaced by SimpleAnnotationMetadataReadingVisitor
 * and related classes for internal use within the framework.
 */
@Deprecated
class RecursiveAnnotationAttributesVisitor extends AbstractRecursiveAnnotationVisitor {

    protected final String annotationType;


    public RecursiveAnnotationAttributesVisitor(
            String annotationType, AnnotationAttributes attributes, @Nullable ClassLoader classLoader) {

        super(classLoader, attributes);
        this.annotationType = annotationType;
    }


    @Override
    public void visitEnd() {
        AnnotationUtils.registerDefaultValues(this.attributes);
    }

}
