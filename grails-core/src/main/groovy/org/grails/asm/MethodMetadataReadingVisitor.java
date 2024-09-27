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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * ASM method visitor which looks for the annotations defined on a method,
 * exposing them through the {@link org.springframework.core.type.MethodMetadata}
 * interface.
 *
 * <p>Note: This class was ported to Grails 7 from Spring Framework 5.3 as it was
 * removed in Spring 6 without a public replacement.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @deprecated As of Spring Framework 5.2, this class and related classes in this
 * package have been replaced by SimpleAnnotationMetadataReadingVisitor
 * and related classes for internal use within the framework.
 */
@Deprecated
public class MethodMetadataReadingVisitor extends MethodVisitor implements MethodMetadata {

    protected final String methodName;

    protected final int access;

    protected final String declaringClassName;

    protected final String returnTypeName;

    @Nullable
    protected final ClassLoader classLoader;

    protected final Set<MethodMetadata> methodMetadataSet;

    protected final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<>(4);

    protected final LinkedMultiValueMap<String, AnnotationAttributes> attributesMap = new LinkedMultiValueMap<>(3);


    public MethodMetadataReadingVisitor(String methodName, int access, String declaringClassName,
                                        String returnTypeName, @Nullable ClassLoader classLoader, Set<MethodMetadata> methodMetadataSet) {

        super(SpringAsmInfo.ASM_VERSION);
        this.methodName = methodName;
        this.access = access;
        this.declaringClassName = declaringClassName;
        this.returnTypeName = returnTypeName;
        this.classLoader = classLoader;
        this.methodMetadataSet = methodMetadataSet;
    }


    @Override
    public MergedAnnotations getAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
        if (!visible) {
            return null;
        }
        this.methodMetadataSet.add(this);
        String className = Type.getType(desc).getClassName();
        return new AnnotationAttributesReadingVisitor(
                className, this.attributesMap, this.metaAnnotationMap, this.classLoader);
    }


    @Override
    public String getMethodName() {
        return this.methodName;
    }

    @Override
    public boolean isAbstract() {
        return ((this.access & Opcodes.ACC_ABSTRACT) != 0);
    }

    @Override
    public boolean isStatic() {
        return ((this.access & Opcodes.ACC_STATIC) != 0);
    }

    @Override
    public boolean isFinal() {
        return ((this.access & Opcodes.ACC_FINAL) != 0);
    }

    @Override
    public boolean isOverridable() {
        return (!isStatic() && !isFinal() && ((this.access & Opcodes.ACC_PRIVATE) == 0));
    }

    @Override
    public boolean isAnnotated(String annotationName) {
        return this.attributesMap.containsKey(annotationName);
    }

    @Override
    @Nullable
    public AnnotationAttributes getAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        AnnotationAttributes raw = AnnotationReadingVisitorUtils.getMergedAnnotationAttributes(
                this.attributesMap, this.metaAnnotationMap, annotationName);
        if (raw == null) {
            return null;
        }
        return AnnotationReadingVisitorUtils.convertClassValues(
                "method '" + getMethodName() + "'", this.classLoader, raw, classValuesAsString);
    }

    @Override
    @Nullable
    public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        if (!this.attributesMap.containsKey(annotationName)) {
            return null;
        }
        MultiValueMap<String, Object> allAttributes = new LinkedMultiValueMap<>();
        List<AnnotationAttributes> attributesList = this.attributesMap.get(annotationName);
        if (attributesList != null) {
            String annotatedElement = "method '" + getMethodName() + '\'';
            for (AnnotationAttributes annotationAttributes : attributesList) {
                AnnotationAttributes convertedAttributes = AnnotationReadingVisitorUtils.convertClassValues(
                        annotatedElement, this.classLoader, annotationAttributes, classValuesAsString);
                convertedAttributes.forEach(allAttributes::add);
            }
        }
        return allAttributes;
    }

    @Override
    public String getDeclaringClassName() {
        return this.declaringClassName;
    }

    @Override
    public String getReturnTypeName() {
        return this.returnTypeName;
    }

}
