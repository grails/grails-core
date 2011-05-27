/*
 * Copyright 2011 SpringSource
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
package org.codehaus.groovy.grails.plugins.support

/**
 * @author Graeme Rocher
 * @since 1.4
 */
class WatchPattern {

    /**
     * The pattern. Eg. file:./grails-app/conf/spring/resources.xml
     */
    String pattern

    /**
     * The directories being watched, if any
     */
    File directory
    /**
     * A concrete file being watched, if any
     */
    File file
    /**
     * The file extensions within the directories being watched
     */
    String extension

    /**
     * Whether the given path matches this watch pattern
     *
     * @param path A file path
     * @return True if it does
     */
    boolean matchesPath(String path) {
        if (file != null) {
            return file.equals(new File(path))
        }

        if (directory != null) {
            try {
                def matchPath = /${directory.absolutePath}.+?${extension}/
                def absolutePath = new File(path).absolutePath
                return absolutePath ==~ matchPath
            } catch (e) {
                // ignore
            }
        }
    }
}
