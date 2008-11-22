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
package org.codehaus.groovy.grails.compiler.injection;

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.PropertyNode;

import java.util.Iterator;
import java.util.List;

/**
 * Helper methods for working with Groovy AST trees
 *
 * @author Graeme Rocher
 * @since 0.3
 *        <p/>
 *        Created: 20th June 2006
 */
public class GrailsASTUtils {

    /**
     * Returns whether a classNode has the specified property or not
     *
     * @param classNode    The ClassNode
     * @param propertyName The name of the property
     * @return True if the property exists in the ClassNode
     */
    public static boolean hasProperty(ClassNode classNode, String propertyName) {
        if (classNode == null || StringUtils.isBlank(propertyName))
            return false;
        List properties = classNode.getProperties();
        for (Iterator i = properties.iterator(); i.hasNext();) {
            PropertyNode pn = (PropertyNode) i.next();
            if (pn.getName().equals(propertyName) && !pn.isPrivate()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOrInheritsProperty(ClassNode classNode, String propertyName) {
        if (hasProperty(classNode, propertyName)) {
            return true;
        } else {
            ClassNode parent = classNode.getSuperClass();
            while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
                if (hasProperty(parent, propertyName)) {
                    return true;
                }
                parent = parent.getSuperClass();
            }
        }
        return false;
    }

    /**
     * Tests whether the ClasNode implements the specified method name
     *
     * @param classNode  The ClassNode
     * @param methodName The method name
     * @return True if it does implement the method
     */
    public static boolean implementsZeroArgMethod(ClassNode classNode, String methodName) {
        MethodNode method = classNode.getDeclaredMethod(methodName, new Parameter[]{});
        return method != null && (method.isPublic() || method.isProtected()) && !method.isAbstract();
    }

    public static boolean implementsOrInheritsZeroArgMethod(ClassNode classNode, String methodName, List ignoreClasses) {
        if (implementsZeroArgMethod(classNode, methodName)) {
            return true;
        } else {
            ClassNode parent = classNode.getSuperClass();
            while (parent != null && !getFullName(parent).equals("java.lang.Object")) {
                if (!ignoreClasses.contains(parent) && implementsZeroArgMethod(parent, methodName)) {
                    return true;
                }
                parent = parent.getSuperClass();
            }
        }
        return false;
    }

    /**
     * Gets the full name of a ClassNode
     *
     * @param classNode The class node
     * @return The full name
     */
    public static String getFullName(ClassNode classNode) {
      return classNode.getName();
    }

}
