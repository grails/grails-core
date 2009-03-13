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
package org.codehaus.groovy.grails.cli.support;

import org.codehaus.groovy.tools.RootLoader;

import java.net.URL;

/**
 * A custom Groovy RootLoader that works around issue
 * <a href="http://jira.codehaus.org/browse/GROOVY-2303">GROOVY-2303</a>.
 * The problem is with Jaxen, so if we manage to remove that dependency
 * (or it stops packaging UserDataHandler) we can revert back to using
 * plain old Groovy RootLoader.
 * @author Graeme Rocher
 * @since 1.0
 *        <p/>
 *        Created: Nov 29, 2007
 */
public class GrailsRootLoader extends RootLoader {
    public GrailsRootLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public GrailsRootLoader() {
        super(new URL[0],ClassLoader.getSystemClassLoader());
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            return super.loadClass(name, resolve);
        } catch (LinkageError e) {
            return getParent().loadClass(name);
        }
    }
}
