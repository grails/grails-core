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

import java.lang.reflect.Field;
import java.security.AccessControlException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link AnnotationVisitor} to recursively visit annotations.

 * <p>Note: This class was ported to Grails 7 from Spring Framework 5.3 as it was
 * removed in Spring 6 without a public replacement.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1.1
 * @deprecated As of Spring Framework 5.2, this class and related classes in this
 * package have been replaced by SimpleAnnotationMetadataReadingVisitor
 * and related classes for internal use within the framework.
 */
@Deprecated
abstract class AbstractRecursiveAnnotationVisitor extends AnnotationVisitor {

    protected final Log logger = LogFactory.getLog(getClass());

    protected final AnnotationAttributes attributes;

    @Nullable
    protected final ClassLoader classLoader;


    public AbstractRecursiveAnnotationVisitor(@Nullable ClassLoader classLoader, AnnotationAttributes attributes) {
        super(SpringAsmInfo.ASM_VERSION);
        this.classLoader = classLoader;
        this.attributes = attributes;
    }


    @Override
    public void visit(String attributeName, Object attributeValue) {
        this.attributes.put(attributeName, attributeValue);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String attributeName, String asmTypeDescriptor) {
        String annotationType = Type.getType(asmTypeDescriptor).getClassName();
        AnnotationAttributes nestedAttributes = new AnnotationAttributes(annotationType, this.classLoader);
        this.attributes.put(attributeName, nestedAttributes);
        return new RecursiveAnnotationAttributesVisitor(annotationType, nestedAttributes, this.classLoader);
    }

    @Override
    public AnnotationVisitor visitArray(String attributeName) {
        return new RecursiveAnnotationArrayVisitor(attributeName, this.attributes, this.classLoader);
    }

    @Override
    public void visitEnum(String attributeName, String asmTypeDescriptor, String attributeValue) {
        Object newValue = getEnumValue(asmTypeDescriptor, attributeValue);
        visit(attributeName, newValue);
    }

    protected Object getEnumValue(String asmTypeDescriptor, String attributeValue) {
        Object valueToUse = attributeValue;
        try {
            Class<?> enumType = ClassUtils.forName(Type.getType(asmTypeDescriptor).getClassName(), this.classLoader);
            Field enumConstant = ReflectionUtils.findField(enumType, attributeValue);
            if (enumConstant != null) {
                ReflectionUtils.makeAccessible(enumConstant);
                valueToUse = enumConstant.get(null);
            }
        }
        catch (ClassNotFoundException | NoClassDefFoundError ex) {
            logger.debug("Failed to classload enum type while reading annotation metadata", ex);
        }
        catch (IllegalAccessException | AccessControlException ex) {
            logger.debug("Could not access enum value while reading annotation metadata", ex);
        }
        return valueToUse;
    }

}
