/*
 * Copyright 2024 original authors
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
package grails.io

import grails.util.BuildSettings
import spock.lang.Specification


class IOUtilsSpec extends Specification{

    void "Test findClassResource finds a class resource"() {
        expect:
        IOUtils.findClassResource(BuildSettings)
        IOUtils.findClassResource(BuildSettings).path.contains('grails-bootstrap')
    }

    void "Test findJarResource finds a the JAR resource"() {
        expect:
        IOUtils.findJarResource(Specification)
        IOUtils.findJarResource(Specification).path.endsWith('spock-core-2.1-groovy-3.0.jar!/')
    }
}
