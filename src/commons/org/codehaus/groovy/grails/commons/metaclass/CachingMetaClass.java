/* Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.commons.metaclass;

import groovy.lang.*;

import java.util.Map;
import java.util.HashMap;

/**
 * A MetaClass that intercepts and caches invocations
 * 
 * @author Marc Palmer (marc@anyware.co.uk)
 * @since 0.5
 */
public class CachingMetaClass extends DelegatingMetaClass
{
    protected Map methodInfoCache = new HashMap();
    protected Map propertyInfoCache = new HashMap();

    public CachingMetaClass(MetaClass metaClass)
    {
        super(metaClass);
    }

    public CachingMetaClass(Class aClass)
    {
        super(aClass);
    }

    public boolean hasMethod(Object object, String name, Object[] args) {
        Object cacheInfo = getCachedFeatureInfo(name, args, methodInfoCache);
        if (cacheInfo != null) {
            return ((Boolean)cacheInfo).booleanValue();
        }

        Class[] argTypes = null;
        if (args != null) {
            argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++)
            {
                argTypes[i] = (args[i] != null) ? args[i].getClass() : null;
            }
        }

        boolean found = false;
        MetaMethod m = pickMethod(name, argTypes);
        found = m != null;

        if (!found) {
            found = hasClosure(object, name);
        }

        toggleFeature(name, found, methodInfoCache);
        return found;
    }

    protected boolean hasClosure(Object object, String name)
    {
        boolean found = false;
        Object possibleClosure = getProperty(object, name);
        if (possibleClosure != null) {
            found = possibleClosure instanceof Closure;
        }
        return found;
    }

    protected void toggleFeature(String name, boolean found, Map cache)
    {
        synchronized(cache) {
            cache.put( name, Boolean.valueOf(found));
        }
    }

    protected Object getCachedFeatureInfo(String name, Object[] args, Map cache)
    {
        Object result = null;
        synchronized(cache) {
            result = cache.get(name);
        }
        return result;
    }

    public boolean hasProperty(Object object, String name) {
        Object cacheInfo = getCachedFeatureInfo(name, null, propertyInfoCache);
        if (cacheInfo != null) {
            return ((Boolean)cacheInfo).booleanValue();
        }

        boolean outcome;
        try
        {
            Object value = super.getProperty(object, name);
            outcome = true;
        }
        catch (MissingPropertyException e)
        {
            outcome = false;
        }

        toggleFeature(name, outcome, propertyInfoCache);
        return outcome;
    }
}
