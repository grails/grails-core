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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * ASM class visitor which looks for the class name and implemented types as
 * well as for the annotations defined on the class, exposing them through
 * the {@link org.springframework.core.type.AnnotationMetadata} interface.
 *
 * <p>Note: This class was ported to Grails 7 from Spring Framework 5.3 as it was
 * removed in Spring 6 without a public replacement.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Costin Leau
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 2.5
 * @deprecated As of Spring Framework 5.2, this class has been replaced by
 * SimpleAnnotationMetadataReadingVisitor for internal use within the
 * framework, but there is no public replacement for
 * {@code AnnotationMetadataReadingVisitor}.
 */
@Deprecated
public class AnnotationMetadataReadingVisitor extends ClassMetadataReadingVisitor implements AnnotationMetadata {

    @Nullable
    protected final ClassLoader classLoader;

    protected final Set<String> annotationSet = new LinkedHashSet<>(4);

    protected final Map<String, Set<String>> metaAnnotationMap = new LinkedHashMap<>(4);

    /**
     * Declared as a {@link LinkedMultiValueMap} instead of a {@link MultiValueMap}
     * to ensure that the hierarchical ordering of the entries is preserved.
     * @see AnnotationReadingVisitorUtils#getMergedAnnotationAttributes
     */
    protected final LinkedMultiValueMap<String, AnnotationAttributes> attributesMap = new LinkedMultiValueMap<>(3);

    protected final Set<MethodMetadata> methodMetadataSet = new LinkedHashSet<>(4);


    public AnnotationMetadataReadingVisitor(@Nullable ClassLoader classLoader) {
        this.classLoader = classLoader;
    }


    @Override
    public MergedAnnotations getAnnotations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Skip bridge methods - we're only interested in original annotation-defining user methods.
        // On JDK 8, we'd otherwise run into double detection of the same annotated method...
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
        return new MethodMetadataReadingVisitor(name, access, getClassName(),
                Type.getReturnType(desc).getClassName(), this.classLoader, this.methodMetadataSet);
    }

    @Override
    @Nullable
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (!visible) {
            return null;
        }
        String className = Type.getType(desc).getClassName();
        if (AnnotationUtils.isInJavaLangAnnotationPackage(className)) {
            return null;
        }
        this.annotationSet.add(className);
        return new AnnotationAttributesReadingVisitor(
                className, this.attributesMap, this.metaAnnotationMap, this.classLoader);
    }


    @Override
    public Set<String> getAnnotationTypes() {
        return this.annotationSet;
    }

    @Override
    public Set<String> getMetaAnnotationTypes(String annotationName) {
        Set<String> metaAnnotationTypes = this.metaAnnotationMap.get(annotationName);
        return (metaAnnotationTypes != null ? metaAnnotationTypes : Collections.emptySet());
    }

    @Override
    public boolean hasMetaAnnotation(String metaAnnotationType) {
        if (AnnotationUtils.isInJavaLangAnnotationPackage(metaAnnotationType)) {
            return false;
        }
        Collection<Set<String>> allMetaTypes = this.metaAnnotationMap.values();
        for (Set<String> metaTypes : allMetaTypes) {
            if (metaTypes.contains(metaAnnotationType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAnnotated(String annotationName) {
        return (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) &&
                this.attributesMap.containsKey(annotationName));
    }

    @Override
    public boolean hasAnnotation(String annotationName) {
        return getAnnotationTypes().contains(annotationName);
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
                "class '" + getClassName() + "'", this.classLoader, raw, classValuesAsString);
    }

    @Override
    @Nullable
    public MultiValueMap<String, Object> getAllAnnotationAttributes(String annotationName, boolean classValuesAsString) {
        MultiValueMap<String, Object> allAttributes = new LinkedMultiValueMap<>();
        List<AnnotationAttributes> attributes = this.attributesMap.get(annotationName);
        if (attributes == null) {
            return null;
        }
        String annotatedElement = "class '" + getClassName() + "'";
        for (AnnotationAttributes raw : attributes) {
            for (Map.Entry<String, Object> entry : AnnotationReadingVisitorUtils.convertClassValues(
                    annotatedElement, this.classLoader, raw, classValuesAsString).entrySet()) {
                allAttributes.add(entry.getKey(), entry.getValue());
            }
        }
        return allAttributes;
    }

    @Override
    public boolean hasAnnotatedMethods(String annotationName) {
        for (MethodMetadata methodMetadata : this.methodMetadataSet) {
            if (methodMetadata.isAnnotated(annotationName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
        Set<MethodMetadata> annotatedMethods = new LinkedHashSet<>(4);
        for (MethodMetadata methodMetadata : this.methodMetadataSet) {
            if (methodMetadata.isAnnotated(annotationName)) {
                annotatedMethods.add(methodMetadata);
            }
        }
        return annotatedMethods;
    }

    @Override
    public Set<MethodMetadata> getDeclaredMethods() {
        return Set.of();
    }

}
