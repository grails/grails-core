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
package org.codehaus.groovy.grails.orm.hibernate.events;

import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;

/**
 * Intermediary class, because def in package name is blocking IntelliJ from compiling.
 * Seems to be an intellij bug not a Groovy one
 *
 * @author Graeme Rocher
 * @since 1.1
 */
public class SaveOrUpdateEventListener extends DefaultSaveOrUpdateEventListener {
    private static final long serialVersionUID = 5329080291582964284L;
}
