/* Copyright 2008 the original author or authors.
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
package grails.util;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Contains utility methods for converting between different name types,
 * for example from class names -> property names and vice-versa. The
 * key aspect of this class is that it has no dependencies outside the
 * JDK! 
 */
public class GrailsNameUtils {
    /**
     * Returns the class name for the given logical name and trailing name. For example "person" and "Controller" would evaluate to "PersonController"
     *
     * @param logicalName The logical name
     * @param trailingName The trailing name
     * @return The class name
     */
    public static String getClassName(String logicalName, String trailingName) {
        if(isBlank(logicalName)) throw new IllegalArgumentException("Argument [logicalName] cannot be null or blank");

        String className = logicalName.substring(0,1).toUpperCase() + logicalName.substring(1);
        if(trailingName != null) className = className + trailingName;
        return className;
    }

    /**
     * Returns the class name representation of the given name
     *
     * @param name The name to convert
     * @return The property name representation
     */
	public static String getClassNameRepresentation(String name) {
        String className;

        StringBuilder buf = new StringBuilder();
        if(name != null && name.length() > 0) {
            String[] tokens = name.split("[^\\w\\d]");
            for (String token1 : tokens) {
                String token = token1.trim();
                buf.append(token.substring(0, 1).toUpperCase(Locale.ENGLISH))
                        .append(token.substring(1));
            }
        }
        className = buf.toString();

        return className;
	}

    /**
     * Converts foo-bar into FooBar. Empty and null strings are returned
     * as-is. 
     *
     * @param name The lower case hyphen separated name
     * @return The class name equivalent.
     */
    private static String getClassNameForLowerCaseHyphenSeparatedName(String name) {
        // Handle null and empty strings.
        if (name == null || name.length() == 0) return name;

        if(name.indexOf('-') > -1) {
            StringBuilder buf = new StringBuilder();
            String[] tokens = name.split("-");
            for (String token : tokens) {
                if (token == null || token.length() == 0) continue;
                buf.append(token.substring(0, 1).toUpperCase())
                        .append(token.substring(1));
            }
            return buf.toString();
        }
        else {
            return name.substring(0,1).toUpperCase() + name.substring(1);
        }
    }

    /**
	 * Retrieves the logical class name of a Grails artifact given the Grails class
     * and a specified trailing name
     *
     * @param clazz The class
     * @param trailingName The trailing name such as "Controller" or "TagLib"
     * @return The logical class name
     */
    public static String getLogicalName(Class clazz, String trailingName) {
        return getLogicalName(clazz.getName(), trailingName);
    }

    /**
     * Retrieves the logical name of the class without the trailing name
     * @param name The name of the class
     * @param trailingName The trailing name
     * @return The logical name
     */
    public static String getLogicalName(String name, String trailingName ) {
        if(!isBlank(trailingName)) {
            String shortName = getShortName(name);
            if(shortName.indexOf( trailingName ) > - 1) {
                return shortName.substring(0, shortName.length() - trailingName.length());
            }
        }
        return name;
    }

    public static String getLogicalPropertyName(String className, String trailingName) {
        return getLogicalName(getPropertyName(className), trailingName);
    }

    /**
     * Shorter version of getPropertyNameRepresentation
     * @param name The name to convert
     * @return The property name version
     */
    public static String getPropertyName(String name) {
    	return getPropertyNameRepresentation(name);
    }

    /**
     * Shorter version of getPropertyNameRepresentation
     * @param clazz The clazz to convert
     * @return The property name version
     */
    public static String getPropertyName(Class clazz) {
    	return getPropertyNameRepresentation(clazz);
    }

    /**
     * Returns the property name equivalent for the specified class
     *
     * @param targetClass The class to get the property name for
     * @return A property name reperesentation of the class name (eg. MyClass becomes myClass)
     */
    public static String getPropertyNameRepresentation(Class targetClass) {
        String shortName = getShortName(targetClass);
        return getPropertyNameRepresentation(shortName);
    }

    /**
     * Returns the property name representation of the given name
     *
     * @param name The name to convert
     * @return The property name representation
     */
    public static String getPropertyNameRepresentation(String name) {
        // Strip any package from the name.
        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            name = name.substring(pos + 1);
        }

        // Check whether the name begins with two upper case letters.
        if(name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1)))  {
            return name;
        }
        else {

            String propertyName = name.substring(0,1).toLowerCase(Locale.ENGLISH) + name.substring(1);
            if(propertyName.indexOf(' ') > -1) {
                propertyName = propertyName.replaceAll("\\s", "");
            }
            return propertyName;
        }
    }

    /**
     * Converts foo-bar into fooBar
     *
     * @param name The lower case hyphen separated name
     * @return The property name equivalent
     */
    public static String getPropertyNameForLowerCaseHyphenSeparatedName(String name) {
        return getPropertyName(getClassNameForLowerCaseHyphenSeparatedName(name));
    }

    /**
     * Returns the class name without the package prefix
     *
     * @param targetClass The class to get a short name for
     * @return The short name of the class
     */
    public static String getShortName(Class targetClass) {
        String className = targetClass.getName();
        return getShortName(className);
    }

    /**
     * Returns the class name without the package prefix
     *
     * @param className The class name to get a short name for
     * @return The short name of the class
     */
    public static String getShortName(String className) {
        int i = className.lastIndexOf(".");
        if(i > -1) {
            className = className.substring( i + 1, className.length() );
        }
        return className;
    }

    /**
     * Retrieves the script name representation of the supplied class. For example
     * MyFunkyGrailsScript would be my-funky-grails-script
     *
     * @param clazz The class to convert
     * @return The script name representation
     */
    public static String getScriptName(Class clazz) {
        if (clazz == null) return null;
    	else return getScriptName(clazz.getName());
    }

    /**
     * Retrieves the script name representation of the given class name.
     * For example MyFunkyGrailsScript would be my-funky-grails-script.
     *
     * @param name The class name to convert.
     * @return The script name representation.
     */
    public static String getScriptName(String name) {
        if (name == null) return null;

		if(name.endsWith(".groovy")) {
			name = name.substring(0, name.length()-7);
		}
    	String naturalName = getNaturalName(getShortName(name));
    	return naturalName.replaceAll("\\s", "-").toLowerCase();
    }

    /**
     * Calculates the class name from a script name in the form
     * my-funk-grails-script
     *
     * @param scriptName The script name
     * @return A class name
     */
    public static String getNameFromScript(String scriptName) {

        return getClassNameForLowerCaseHyphenSeparatedName(scriptName);
    }

    /**
     * Converts a property name into its natural language equivalent eg ('firstName' becomes 'First Name')
     * @param name The property name to convert
     * @return The converted property name
     */
    public static String getNaturalName(String name) {
        List words = new ArrayList();
        int i = 0;
        char[] chars = name.toCharArray();
        for (int j = 0; j < chars.length; j++) {
            char c = chars[j];
            String w;
            if(i >= words.size()) {
                w = "";
                words.add(i, w);
            }
            else {
                w = (String)words.get(i);
            }

            if(Character.isLowerCase(c) || Character.isDigit(c)) {
                if(Character.isLowerCase(c) && w.length() == 0)
                    c = Character.toUpperCase(c);
                else if(w.length() > 1 && Character.isUpperCase(w.charAt(w.length() - 1)) ) {
                    w = "";
                    words.add(++i,w);
                }

                words.set(i, w + c);
            }
            else if(Character.isUpperCase(c)) {
                if((i == 0 && w.length() == 0) || Character.isUpperCase(w.charAt(w.length() - 1)) ) 	{
                    words.set(i, w + c);
                }
                else {
                    words.add(++i, String.valueOf(c));
                }
            }

        }

        StringBuilder buf = new StringBuilder();

        for (Iterator j = words.iterator(); j.hasNext();) {
            String word = (String) j.next();
            buf.append(word);
            if(j.hasNext())
                buf.append(' ');
        }
        return buf.toString();
    }

    /**
     * <p>Determines whether a given string is <code>null</code>, empty,
     * or only contains whitespace. If it contains anything other than
     * whitespace then the string is not considered to be blank and the
     * method returns <code>false</code>.</p>
     * <p>We could use Commons Lang for this, but we don't want GrailsNameUtils
     * to have a dependency on any external library to minimise the number of
     * dependencies required to bootstrap Grails.</p>
     * @param str The string to test.
     * @return <code>true</code> if the string is <code>null</code>, or
     * blank.
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }
}
