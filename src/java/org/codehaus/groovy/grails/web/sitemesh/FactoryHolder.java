/* Copyright 2006-2007 Graeme Rocher
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
package org.codehaus.groovy.grails.web.sitemesh;

import org.springframework.util.Assert;

import com.opensymphony.module.sitemesh.Factory;

/**
 * Holds a reference to the Sitemesh Factory object.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class FactoryHolder {

    private FactoryHolder() {
        // static only
    }

    private static Factory factory;

    public static Factory getFactory() {
        Assert.state(factory != null, "Cannot return Sitemesh factory it has not been set!");
        return factory;
    }

    public static void setFactory(Factory newFactory) {
        synchronized(FactoryHolder.class) {
            factory = newFactory;
        }
    }
}
