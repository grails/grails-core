/* Copyright 2004-2005 the original author or authors.
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
package org.codehaus.groovy.grails.commons;


import groovy.lang.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.*;
import org.springframework.util.Assert;
import org.springframework.core.JdkVersion;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Graeme Rocher
 * @since 08-Jul-2005
 * 
 * Class containing utility methods for dealing with Grails class artifacts
 * 
 */
public class GrailsClassUtils {

    private static final String PROPERTY_SET_PREFIX = "set";
	public static final Map PRIMITIVE_TYPE_COMPATIBLE_CLASSES = new HashMap();

    /**
     * Just add two entries to the class compatibility map
     * @param left
     * @param right
     */
    private static final void registerPrimitiveClassPair(Class left, Class right)
    {
        PRIMITIVE_TYPE_COMPATIBLE_CLASSES.put( left, right);
        PRIMITIVE_TYPE_COMPATIBLE_CLASSES.put( right, left);
    }

    static
    {
        registerPrimitiveClassPair( Boolean.class, boolean.class);
        registerPrimitiveClassPair( Integer.class, int.class);
        registerPrimitiveClassPair( Short.class, short.class);
        registerPrimitiveClassPair( Byte.class, byte.class);
        registerPrimitiveClassPair( Character.class, char.class);
        registerPrimitiveClassPair( Long.class, long.class);
        registerPrimitiveClassPair( Float.class, float.class);
        registerPrimitiveClassPair( Double.class, double.class);
    }

    /**
     *
     * Returns true if the specified property in the specified class is of the specified type
     *
     * @param clazz The class which contains the property
     * @param propertyName The property name
     * @param type The type to check
     *
     * @return A boolean value
     */
    public static boolean isPropertyOfType( Class clazz, String propertyName, Class type ) {
        try {

            Class propType = getPropertyType( clazz, propertyName );
            return propType != null && propType.equals(type);
        }
        catch(Exception e) {
            return false;
        }
    }


    /**
     * Returns the value of the specified property and type from an instance of the specified Grails class
     *
     * @param clazz The name of the class which contains the property
     * @param propertyName The property name
     * @param propertyType The property type
     *
     * @return The value of the property or null if none exists
     */
    public static Object getPropertyValueOfNewInstance(Class clazz, String propertyName, Class propertyType) {
        // validate
        if(clazz == null || StringUtils.isBlank(propertyName))
            return null;

        Object instance = null;
        try {
            instance = BeanUtils.instantiateClass(clazz);
        } catch (BeanInstantiationException e) {
            return null;
        }


        return getPropertyOrStaticPropertyOrFieldValue(instance, propertyName);
    }

    /**
     * Returns the value of the specified property and type from an instance of the specified Grails class
     *
     * @param clazz The name of the class which contains the property
     * @param propertyName The property name
     *
     * @return The value of the property or null if none exists
     */
    public static Object getPropertyValueOfNewInstance(Class clazz, String propertyName) {
        // validate
        if(clazz == null || StringUtils.isBlank(propertyName))
            return null;

        Object instance = null;
        try {
            instance = BeanUtils.instantiateClass(clazz);
        } catch (BeanInstantiationException e) {
            return null;
        }


        return getPropertyOrStaticPropertyOrFieldValue(instance, propertyName);
    }


    /**
     * Retrieves a PropertyDescriptor for the specified instance and property value
     *
     * @param instance The instance
     * @param propertyValue The value of the property
     * @return The PropertyDescriptor
     */
    public static PropertyDescriptor getPropertyDescriptorForValue(Object instance, Object propertyValue) {
        if(instance == null || propertyValue == null)
            return null;

        BeanWrapper wrapper = new BeanWrapperImpl(instance);
        PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();

        for (int i = 0; i < descriptors.length; i++) {
            Object value = wrapper.getPropertyValue( descriptors[i].getName() );
            if(propertyValue.equals(value))
                return descriptors[i];
        }
        return null;
    }
    /**
     * Returns the type of the given property contained within the specified class
     *
     * @param clazz The class which contains the property
     * @param propertyName The name of the property
     *
     * @return The property type or null if none exists
     */
    public static Class getPropertyType(Class clazz, String propertyName) {
        if(clazz == null || StringUtils.isBlank(propertyName))
            return null;

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(clazz);
            if(wrapper.isReadableProperty(propertyName)) {
                return wrapper.getPropertyType(propertyName);
            }
            else {
                return null;
            }
        } catch (Exception e) {
            // if there are any errors in instantiating just return null for the moment
            return null;
        }
    }

    /**
     * Retrieves all the properties of the given class for the given type
     *
     * @param clazz The class to retrieve the properties from
     * @param propertyType The type of the properties you wish to retrieve
     *
     * @return An array of PropertyDescriptor instances
     */
    public static PropertyDescriptor[] getPropertiesOfType(Class clazz, Class propertyType) {
        if(clazz == null || propertyType == null)
            return new PropertyDescriptor[0];

        Set properties = new HashSet();
        try {
            BeanWrapper wrapper = new BeanWrapperImpl(clazz.newInstance());
            PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();

            for (int i = 0; i < descriptors.length; i++) {
                Class currentPropertyType = descriptors[i].getPropertyType();
                if(isTypeInstanceOfPropertyType(propertyType, currentPropertyType)) {
                    properties.add(descriptors[i]);
                }
            }

        } catch (Exception e) {
            // if there are any errors in instantiating just return null for the moment
            return new PropertyDescriptor[0];
        }
        return (PropertyDescriptor[])properties.toArray( new PropertyDescriptor[ properties.size() ] );
    }

    private static boolean isTypeInstanceOfPropertyType(Class type, Class propertyType) {
        return propertyType.isAssignableFrom(type) && !propertyType.equals(Object.class);
    }

    /**
     * Retrieves all the properties of the given class which are assignable to the given type
     *
     * @param clazz             The class to retrieve the properties from
     * @param propertySuperType The type of the properties you wish to retrieve
     * @return An array of PropertyDescriptor instances
     */
    public static PropertyDescriptor[] getPropertiesAssignableToType(Class clazz, Class propertySuperType) {
        if (clazz == null || propertySuperType == null) return new PropertyDescriptor[0];

        Set properties = new HashSet();
        try {
            PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(clazz);

            for (int i = 0; i < descriptors.length; i++) {
                if (propertySuperType.isAssignableFrom(descriptors[i].getPropertyType())) {
                    properties.add(descriptors[i]);
                }
            }
        } catch (Exception e) {
            return new PropertyDescriptor[0];
        }
        return (PropertyDescriptor[]) properties.toArray(new PropertyDescriptor[properties.size()]);
    }

    /**
     * Retrieves a property of the given class of the specified name and type
     * @param clazz The class to retrieve the property from
     * @param propertyName The name of the property
     * @param propertyType The type of the property
     *
     * @return A PropertyDescriptor instance or null if none exists
     */
    public static PropertyDescriptor getProperty(Class clazz, String propertyName, Class propertyType) {
        if(clazz == null || propertyName == null || propertyType == null)
            return null;

        try {
            BeanWrapper wrapper = new BeanWrapperImpl(clazz.newInstance());
            PropertyDescriptor pd = wrapper.getPropertyDescriptor(propertyName);
            if(pd.getPropertyType().equals( propertyType )) {
                return pd;
            }
            else {
                return null;
            }
        } catch (Exception e) {
            // if there are any errors in instantiating just return null for the moment
            return null;
        }
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
     * Returns the class name representation of the given name
     *
     * @param name The name to convert
     * @return The property name representation
     */
	public static String getClassNameRepresentation(String name) {
        String className;

        StringBuffer buf = new StringBuffer();
        if(name != null && name.length() > 0) {
            String[] tokens = name.split("[^\\w\\d]");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                buf.append(token.substring(0, 1).toUpperCase(Locale.ENGLISH))
                        .append(token.substring(1));
            }
        }
        className = buf.toString();

        return className;
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
     * Retrieves the script name representation of the supplied class. For example
     * MyFunkyGrailsScript would be my-funky-grails-script
     * 
     * @param clazz The class to convert
     * @return The script name representation
     */
    public static String getScriptName(Class clazz) {
    	return getScriptName(clazz.getName());
    }
    
    public static String getScriptName(String name) {  
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
     * Converts foo-bar into fooBar
     *
     * @param name The lower case hyphen separated name
     * @return The property name equivalent
     */
    public static String getPropertyNameForLowerCaseHyphenSeparatedName(String name) {
        return getPropertyName(getClassNameForLowerCaseHyphenSeparatedName(name));
    }

    /**
     * Converts foo-bar into FooBar
     *
     * @param name The lower case hyphen separated name
     * @return The class name equivalent
     */
    private static String getClassNameForLowerCaseHyphenSeparatedName(String name) {
        if(name.indexOf('-') > -1) {
            StringBuffer buf = new StringBuffer();
            String[] tokens = name.split("-");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if(token == null || token.length() == 0) continue;
                buf.append(token.substring(0,1).toUpperCase())
                   .append(token.substring(1));
            }
            return buf.toString();
        }
        else {
            return name.substring(0,1).toUpperCase() + name.substring(1);
        }
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

        StringBuffer buf = new StringBuffer();

        for (Iterator j = words.iterator(); j.hasNext();) {
            String word = (String) j.next();
            buf.append(word);
            if(j.hasNext())
                buf.append(' ');
        }
        return buf.toString();
    }


    /**
     * Convenience method for converting a collection to an Object[]
     * @param c The collection
     * @return  An object array
     */
    public static Object[] collectionToObjectArray(Collection c) {
        if(c == null) return new Object[0];

        return c.toArray(new Object[c.size()]);
    }


    /**
     * Detect if left and right types are matching types. In particular,
     * test if one is a primitive type and the other is the corresponding
     * Java wrapper type. Primitive and wrapper classes may be passed to
     * either arguments.
     *
     * @param leftType
     * @param rightType
     * @return true if one of the classes is a native type and the other the object representation
     * of the same native type
     */
    public static boolean isMatchBetweenPrimativeAndWrapperTypes(Class leftType, Class rightType) {
        if (leftType == null) {
            throw new NullPointerException("Left type is null!");
        } else if (rightType == null) {
            throw new NullPointerException("Right type is null!");
        } else {
            Class r = (Class)PRIMITIVE_TYPE_COMPATIBLE_CLASSES.get(leftType);
            return r == rightType;
        }
    }

    /**
     * <p>Tests whether or not the left hand type is compatible with the right hand type in Groovy
     * terms, i.e. can the left type be assigned a value of the right hand type in Groovy.</p>
     * <p>This handles Java primitive type equivalence and uses isAssignableFrom for all other types,
     * with a bit of magic for native types and polymorphism i.e. Number assigned an int.
     * If either parameter is null an exception is thrown</p>
     *
     * @param leftType The type of the left hand part of a notional assignment
     * @param rightType The type of the right hand part of a notional assignment
     * @return True if values of the right hand type can be assigned in Groovy to variables of the left hand type.
     */
    public static boolean isGroovyAssignableFrom(
            Class leftType, Class rightType)
    {
        if (leftType == null) {
            throw new NullPointerException("Left type is null!");
        } else if (rightType == null) {
            throw new NullPointerException("Right type is null!");
        } else if (leftType == Object.class) {
            return true;
        } else if (leftType == rightType) {
            return true;
        } else {
            // check for primitive type equivalence
            Class r = (Class)PRIMITIVE_TYPE_COMPATIBLE_CLASSES.get(leftType);
            boolean result = r == rightType;

            if (!result)
            {
                // If no primitive <-> wrapper match, it may still be assignable
                // from polymorphic primitives i.e. Number -> int (AKA Integer)
                if (rightType.isPrimitive())
                {
                    // see if incompatible
                    r = (Class)PRIMITIVE_TYPE_COMPATIBLE_CLASSES.get(rightType);
                    if (r != null)
                    {
                        result = leftType.isAssignableFrom(r);
                    }
                } else
                {
                    // Otherwise it may just be assignable using normal Java polymorphism
                    result = leftType.isAssignableFrom(rightType);
                }
            }
            return result;
        }
    }

    /**
     * <p>Work out if the specified property is readable and static. Java introspection does not
     * recognize this concept of static properties but Groovy does. We also consider public static fields
     * as static properties with no getters/setters</p>
     *
     * @param clazz The class to check for static property
     * @param propertyName The property name
     * @return true if the property with name propertyName has a static getter method
     */
    public static boolean isStaticProperty( Class clazz, String propertyName)
    {
        Method getter = BeanUtils.findDeclaredMethod(clazz, getGetterName(propertyName), null);
        if (getter != null)
        {
            return isPublicStatic(getter);
        }
        else
        {
            try
            {
                Field f = clazz.getDeclaredField(propertyName);
                if (f != null)
                {
                    return isPublicStatic(f);
                }
            }
            catch (NoSuchFieldException e)
            {
            }
        }

        return false;
    }

    /**
     * Determine whether the method is declared public static
     * @param m
     * @return True if the method is declared public static
     */
    public static boolean isPublicStatic( Method m)
    {
        final int modifiers = m.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    /**
     * Determine whether the field is declared public static
     * @param f
     * @return True if the field is declared public static
     */
    public static boolean isPublicStatic( Field f)
    {
        final int modifiers = f.getModifiers();
        return Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers);
    }

    /**
     * Calculate the name for a getter method to retrieve the specified property
     * @param propertyName
     * @return The name for the getter method for this property, if it were to exist, i.e. getConstraints
     */
    public static String getGetterName(String propertyName)
    {
        return "get" + Character.toUpperCase(propertyName.charAt(0))
            + propertyName.substring(1);
    }

    /**
     * <p>Get a static property value, which has a public static getter or is just a public static field.</p>
     *
     * @param clazz The class to check for static property
     * @param name The property name
     * @return The value if there is one, or null if unset OR there is no such property
     */
    public static Object getStaticPropertyValue(Class clazz, String name)
    {
        Method getter = BeanUtils.findDeclaredMethod(clazz, getGetterName(name), null);
        try
        {
            if (getter != null)
            {
                return getter.invoke(null, null);
            }
            else
            {
                Field f = clazz.getDeclaredField(name);
                if (f != null)
                {
                    return f.get(null);
                }
            }
        }
        catch (Exception e)
        {
        }
        return null;
    }

    /**
     * <p>Looks for a property of the reference instance with a given name.</p>
     * <p>If found its value is returned. We follow the Java bean conventions with augmentation for groovy support
     * and static fields/properties. We will therefore match, in this order:
     * </p>
     * <ol>
     * <li>Standard public bean property (with getter or just public field, using normal introspection)
     * <li>Public static property with getter method
     * <li>Public static field
     * </ol>
     *
     * @return property value or null if no property found
     */
    public static Object getPropertyOrStaticPropertyOrFieldValue(Object obj, String name) throws BeansException
    {
        BeanWrapper ref = new BeanWrapperImpl(obj);
        if (ref.isReadableProperty(name)) {
            return ref.getPropertyValue(name);
        }
        else
        {
            // Look for public fields
            if (isPublicField(obj, name))
            {
                return getFieldValue(obj, name);
            }

            // Look for statics
            Class clazz = obj.getClass();
            if (isStaticProperty(clazz, name))
            {
                return getStaticPropertyValue(clazz, name);
            }
            else
            {
               return null;
            }
        }
    }

    /**
     * Get the value of a declared field on an object
     *
     * @param obj
     * @param name
     * @return The object value or null if there is no such field or access problems
     */
    public static Object getFieldValue(Object obj, String name)
    {
        Class clazz = obj.getClass();
        Field f = null;
        try
        {
            f = clazz.getDeclaredField(name);
            return f.get(obj);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Work out if the specified object has a public field with the name supplied.
     *
     * @param obj
     * @param name
     * @return True if a public field with the name exists
     */
    public static boolean isPublicField(Object obj, String name)
    {
        Class clazz = obj.getClass();
        Field f = null;
        try
        {
            f = clazz.getDeclaredField(name);
            return Modifier.isPublic(f.getModifiers());
        }
        catch (NoSuchFieldException e)
        {
            return false;
        }
    }

    /**
     * Checks whether the specified property is inherited from a super class
     * 
     * @param clz The class to check
     * @param propertyName The property name
     * @return True if the property is inherited
     */
    public static boolean isPropertyInherited(Class clz, String propertyName) {
		if(clz == null) return false;
		if(StringUtils.isBlank(propertyName))
			throw new IllegalArgumentException("Argument [propertyName] cannot be null or blank");
		
		Class superClass = clz.getSuperclass();
		
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(superClass, propertyName);
		if (pd != null && pd.getReadMethod() != null) {
			return true;
		}
		return false;
	}

	/**
	 * Creates a concrete collection for the suppied interface
	 * @param interfaceType The interface
	 * @return ArrayList for List, TreeSet for SortedSet, HashSet for Set etc.
	 */
	public static Collection createConcreteCollection(Class interfaceType) {
		Collection elements;
		if(interfaceType.equals(List.class)) {
		    elements = new ArrayList();
		}
		else if(interfaceType.equals(SortedSet.class)) {
		    elements = new TreeSet();
		}
		else {
		    elements = new HashSet();
		}
		return elements;
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
     * Retrieves the logical name of the classs without the trailing name
     * @param name The name of the class
     * @param trailingName The trailing name
     * @return The logical name
     */
    public static String getLogicalName(String name, String trailingName ) {
        if(!StringUtils.isBlank(trailingName)) {
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
	 * Retrieves the name of a setter for the specified property name
	 * @param propertyName The property name
	 * @return The setter equivalent
	 */
	public static String getSetterName(String propertyName) {
		return PROPERTY_SET_PREFIX+propertyName.substring(0,1).toUpperCase()+ propertyName.substring(1);
	}

	/**
	 * Returns true if the name of the method specified and the number of arguments make it a javabean property
	 * 
	 * @param name True if its a Javabean property
	 * @param args The arguments
	 * @return True if it is a javabean property method
	 */
	public static boolean isGetter(String name, Class[] args) {
		if(StringUtils.isBlank(name) || args == null)return false;
        if(args.length != 0)return false;

        if(name.startsWith("get")) {
			name = name.substring(3);
			if(name.length() > 0 && Character.isUpperCase(name.charAt(0))) return true;			
		}
        else if(name.startsWith("is")) {
            name = name.substring(2);
            if(name.length() > 0 && Character.isUpperCase(name.charAt(0))) return true;
        }
        return false;
	}

	/**
	 * Returns a property name equivalent for the given getter name or null if it is not a getter
	 * 
	 * @param getterName The getter name
	 * @return The property name equivalent
	 */
	public static String getPropertyForGetter(String getterName) {
		if(StringUtils.isBlank(getterName))return null;
		
		if(getterName.startsWith("get")) {
			String prop = getterName.substring(3);
			return convertPropertyName(prop);
		}
        else if(getterName.startsWith("is")) {
            String prop = getterName.substring(2);
            return convertPropertyName(prop);
        }
        return null;
	}

	private static String convertPropertyName(String prop) {
		if(Character.isUpperCase(prop.charAt(0)) && Character.isUpperCase(prop.charAt(1))) {
			return prop;
		}
		else if(Character.isDigit(prop.charAt(0))) {
			return prop;
		}
		else {
			return Character.toLowerCase(prop.charAt(0)) + prop.substring(1);
		}
	}
	
	
	/**
	 * Returns a property name equivalent for the given setter name or null if it is not a getter
	 * 
	 * @param setterName The setter name
	 * @return The property name equivalent
	 */
	public static String getPropertyForSetter(String setterName) {
		if(StringUtils.isBlank(setterName))return null;
		
		if(setterName.startsWith("set")) {
			String prop = setterName.substring(3);
			return convertPropertyName(prop);
		}
		return null;
	}

	public static boolean isSetter(String name, Class[] args) {
		if(StringUtils.isBlank(name) || args == null)return false;
		
		if(name.startsWith("set")) {
			if(args.length != 1) return false;
			name = name.substring(3);
			if(name.length() > 0 && Character.isUpperCase(name.charAt(0))) return true;
		}
		
		return false;
	}

	public static MetaClass getExpandoMetaClass(Class clazz) {
        MetaClassRegistry registry = GroovySystem.getMetaClassRegistry();
        Assert.isTrue(registry.getMetaClassCreationHandler() instanceof ExpandoMetaClassCreationHandle, "Grails requires an instance of [ExpandoMetaClassCreationHandle] to be set in Groovy's MetaClassRegistry!");
        MetaClass mc = registry.getMetaClass(clazz);
        AdaptingMetaClass adapter = null;
        if(mc instanceof AdaptingMetaClass) {
            adapter = (AdaptingMetaClass) mc;
            mc= ((AdaptingMetaClass)mc).getAdaptee();
		}

        if(!(mc instanceof ExpandoMetaClass)) {
            // removes cached version
            registry.removeMetaClass(clazz);
            mc= registry.getMetaClass(clazz);
            if(adapter != null) {
                adapter.setAdaptee(mc);
            }
        }
        Assert.isTrue(mc instanceof ExpandoMetaClass,"BUG! Method must return an instance of [ExpandoMetaClass]!");
        return mc;
	}	
	
    /**
     * Returns true if the specified clazz parameter is either the same as, or is a superclass or superinterface 
     * of, the specified type parameter. Converts primitive types to compatible class automatically.
     * 
     * @param clazz
     * @param type
     * @return True if the class is a taglib
     * @see java.lang.Class#isAssignableFrom(Class)
     */
    public static boolean isAssignableOrConvertibleFrom(Class clazz, Class type) {
    	if (type == null || clazz == null) {
    		return false;
    	}
    	else if (type.isPrimitive()) {
    		// convert primitive type to compatible class 
    		Class primitiveClass = (Class)GrailsClassUtils.PRIMITIVE_TYPE_COMPATIBLE_CLASSES.get(type);
    		if (primitiveClass == null) {
    			// no compatible class found for primitive type
    			return false;
    		}
    		else {
    			return clazz.isAssignableFrom(primitiveClass);
    		}
    	}
    	else {
    		return clazz.isAssignableFrom(type);
    	}
    }


    /**
     * Retrieves a boolean value from a Map for the given key
     *
     * @param key The key that references the boolean value
     * @param map The map to look in
     * @return A boolean value which will be false if the map is null, the map doesn't contain the key or the value is false 
     */
    public static boolean getBooleanFromMap(String key, Map map) {
        if(map == null) return false;
        if(map.containsKey(key)) {
            Object o = map.get(key);
            if(o == null)return false;
            else if(o instanceof Boolean) {
                return ((Boolean)o).booleanValue();
            }
            else {
                 return Boolean.valueOf(o.toString()).booleanValue();
            }
        }
        return false;
    }

    /**
     * Returns the class name for the given logical name and trailing name. For example "person" and "Controller" would evaluate to "PersonController"
     *
     * @param logicalName The logical name
     * @param trailingName The trailing name
     * @return The class name
     */
    public static String getClassName(String logicalName, String trailingName) {
        if(StringUtils.isBlank(logicalName)) throw new IllegalArgumentException("Argument [logicalName] cannot be null or blank");

        String className = logicalName.substring(0,1).toUpperCase() + logicalName.substring(1);
        if(trailingName != null) className = className + trailingName;
        return className;
    }

    /**
     * Checks whether the given class is a JDK 1.5 enum or not
     *
     * @param type The class to check
     * @return True if it is an enum
     */
    public static boolean isJdk5Enum(Class type) {
        if (JdkVersion.getMajorJavaVersion() >= JdkVersion.JAVA_15) {
            Method m = BeanUtils.findMethod(type.getClass(),"isEnum", null);
            if(m == null) return false;
            try {
                Object result = m.invoke(type, null);
                return result instanceof Boolean && ((Boolean) result).booleanValue();
            } catch (Exception e ) {
                return false;
            }
        } else {
            return false;
        }
    }
}
