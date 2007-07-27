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
import org.codehaus.groovy.ast.PropertyNode;

import java.util.Iterator;
import java.util.List;

/**
 * Helper methods for working with Groovy AST trees
 * 
 * @author Graeme Rocher
 *
 * @since 0.3
 * 
 * Created: 20th June 2006
 */
public class GrailsASTUtils {

	/**
	 * Returns whether a classNode has the specified property or not
	 * 
	 * @param classNode The ClassNode
	 * @param propertyName The name of the property
	 * @return True if the property exists in the ClassNode
	 */
	public static boolean hasProperty(ClassNode classNode, String propertyName) {
		if(classNode == null || StringUtils.isBlank(propertyName))
			return false;
		
		List properties = classNode.getProperties();
		for (Iterator i = properties.iterator(); i.hasNext();) {
			PropertyNode pn = (PropertyNode) i.next();
			if(pn.getName().equals(propertyName))
				return true;
		}
		return false;
	}

	/**
	 * Tests whether the ClasNode implements the specified method name
	 * 
	 * @param classNode The ClassNode
	 * @param methodName The method name
	 * @return True if it does implement the method
	 */
	public static boolean implementsZeroArgMethod(ClassNode classNode, String methodName) {
		return implementsMethod(classNode, methodName, new Class[0]);
	}

	/**
	 * Tests whether the ClassNode implements the specified method name
	 * 
	 * @param classNode The ClassNode
	 * @param methodName The method name
	 * @param argTypes
	 * @return True if it implements the method
	 */
	private static boolean implementsMethod(ClassNode classNode, String methodName, Class[] argTypes) {
        String name = classNode.getSuperClass().getName();
        if(name.equals("java.lang.Object")) {
            List methods = classNode.getMethods();
            for (Iterator i = methods.iterator(); i.hasNext();) {
                MethodNode mn = (MethodNode) i.next();
                final boolean isZeroArg = (argTypes == null || argTypes.length ==0);
                boolean methodMatch = mn.getName().equals(methodName) && isZeroArg;
                if(methodMatch)return true;
                // TODO Implement further parameter analysis
            }
        }
        else {
            return true;
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
		String className = classNode.getPackageName();
		
		return className == null ? classNode.getName() : (className+=classNode.getName());
	}

}
