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

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * ASM visitor which looks for annotations defined on a class or method,
 * including meta-annotations.
 *
 * <p>This visitor is fully recursive, taking into account any nested
 * annotations or nested annotation arrays.
 *
 * <p>Note: This class was ported to Grails 7 from Spring Framework 5.3 as it was
 * removed in Spring 6 without a public replacement.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 * @deprecated As of Spring Framework 5.2, this class and related classes in this
 * package have been replaced by SimpleAnnotationMetadataReadingVisitor
 * and related classes for internal use within the framework.
 */
@Deprecated
final class AnnotationAttributesReadingVisitor extends RecursiveAnnotationAttributesVisitor {

    private final MultiValueMap<String, AnnotationAttributes> attributesMap;

    private final Map<String, Set<String>> metaAnnotationMap;


    public AnnotationAttributesReadingVisitor(String annotationType,
                                              MultiValueMap<String, AnnotationAttributes> attributesMap, Map<String, Set<String>> metaAnnotationMap,
                                              @Nullable ClassLoader classLoader) {

        super(annotationType, new AnnotationAttributes(annotationType, classLoader), classLoader);
        this.attributesMap = attributesMap;
        this.metaAnnotationMap = metaAnnotationMap;
    }


    @Override
    public void visitEnd() {
        super.visitEnd();

        Class<? extends Annotation> annotationClass = this.attributes.annotationType();
        if (annotationClass != null) {
            List<AnnotationAttributes> attributeList = this.attributesMap.get(this.annotationType);
            if (attributeList == null) {
                this.attributesMap.add(this.annotationType, this.attributes);
            }
            else {
                attributeList.add(0, this.attributes);
            }
            if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationClass.getName())) {
                try {
                    Annotation[] metaAnnotations = annotationClass.getAnnotations();
                    if (!ObjectUtils.isEmpty(metaAnnotations)) {
                        Set<Annotation> visited = new LinkedHashSet<>();
                        for (Annotation metaAnnotation : metaAnnotations) {
                            recursivelyCollectMetaAnnotations(visited, metaAnnotation);
                        }
                        if (!visited.isEmpty()) {
                            Set<String> metaAnnotationTypeNames = new LinkedHashSet<>(visited.size());
                            for (Annotation ann : visited) {
                                metaAnnotationTypeNames.add(ann.annotationType().getName());
                            }
                            this.metaAnnotationMap.put(annotationClass.getName(), metaAnnotationTypeNames);
                        }
                    }
                }
                catch (Throwable ex) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to introspect meta-annotations on " + annotationClass + ": " + ex);
                    }
                }
            }
        }
    }

    private void recursivelyCollectMetaAnnotations(Set<Annotation> visited, Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String annotationName = annotationType.getName();
        if (!AnnotationUtils.isInJavaLangAnnotationPackage(annotationName) && visited.add(annotation)) {
            try {
                // Only do attribute scanning for public annotations; we'd run into
                // IllegalAccessExceptions otherwise, and we don't want to mess with
                // accessibility in a SecurityManager environment.
                if (Modifier.isPublic(annotationType.getModifiers())) {
                    this.attributesMap.add(annotationName,
                            AnnotationUtils.getAnnotationAttributes(annotation, false, true));
                }
                for (Annotation metaMetaAnnotation : annotationType.getAnnotations()) {
                    recursivelyCollectMetaAnnotations(visited, metaMetaAnnotation);
                }
            }
            catch (Throwable ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Failed to introspect meta-annotations on " + annotation + ": " + ex);
                }
            }
        }
    }

}
