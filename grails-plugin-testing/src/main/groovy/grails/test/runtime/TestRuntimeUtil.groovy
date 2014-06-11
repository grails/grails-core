/*
 * Copyright 2014 Pivotal
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
package grails.test.runtime

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;

import org.springframework.core.annotation.AnnotationUtils;

/**
 * Utility functions used in TestRuntime
 * 
 * @author Lari Hotari
 * @since 2.4.1
 *
 */
class TestRuntimeUtil {
    /**
     * Collections annotations of given type of given class. Processes meta-annotations.
     *  
     * @param ae annotated element (class or package usually)
     * @param annotationType
     * @return collection of annotations
     */
    public static <T extends Annotation> Collection<T> getAnnotations(AnnotatedElement ae, Class<T> annotationType) {
        Collection<T> anns = new ArrayList<T>(2);

        // look at raw annotation
        T ann = ae.getAnnotation(annotationType);
        if (ann != null) {
            anns.add(ann);
        }

        // scan meta-annotations
        for (Annotation metaAnn : ae.getAnnotations()) {
            ann = metaAnn.annotationType().getAnnotation(annotationType);
            if (ann != null) {
                anns.add(ann);
            }
        }

        return anns;
    }
    
    /**
     * Collects all annotations of given type from given class, it's package, superclasses and their packages
     * Meta-annotations get processed too. 
     * 
     * @param annotatedClazz
     * @param annotationType
     * @param checkPackage
     * @return
     */
    public static <T extends Annotation> Collection<T> collectAllAnnotations(Class annotatedClazz, Class<T> annotationType, boolean checkPackage) {
        List<T> annotations = []
        Class<?> currentClass = annotatedClazz
        while(currentClass != Object) {
            annotations.addAll(getAnnotations(currentClass, annotationType))
            if(checkPackage && currentClass.getPackage() != null) {
                annotations.addAll(getAnnotations(currentClass.getPackage(), annotationType))
            }
            currentClass = currentClass.getSuperclass()
        }
        annotations
    }
    
    /**
     * Finds first annotation of given type from given class, it's package or superclasses (and their package)
     * Meta-annotations get processed too.
     * 
     * @param annotatedClazz
     * @param annotationType
     * @param checkPackage
     * @return
     */
    public static <T extends Annotation> T findFirstAnnotation(Class annotatedClazz, Class<T> annotationType, boolean checkPackage) {
        Class<?> currentClass = annotatedClazz
        while(currentClass != Object) {
            T annotation = AnnotationUtils.getAnnotation(annotatedClazz, annotationType)
            if(annotation != null) return annotation
            if(checkPackage && currentClass.getPackage() != null) {
                annotation = AnnotationUtils.getAnnotation(currentClass.getPackage(), annotationType)
                if(annotation != null) return annotation
            }
            currentClass = currentClass.getSuperclass()
        }
        return null
    }
}
