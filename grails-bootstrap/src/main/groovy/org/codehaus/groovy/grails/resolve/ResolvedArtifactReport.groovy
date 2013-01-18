/* Copyright 2013 the original author or authors.
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
package org.codehaus.groovy.grails.resolve

import groovy.transform.Canonical
import groovy.transform.ToString

/**
 * Used to represent a resolved artifact (downloaded and cached) in the dependency resolution system
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@Canonical
@ToString
public class ResolvedArtifactReport {

    ResolvedArtifactReport(Dependency dependency, File file) {
        this.dependency = dependency
        this.file = file
    }

    /**
     * @return The dependency
     */
    Dependency dependency

    /**
     * @return The file for this dependency
     */
    File file

}