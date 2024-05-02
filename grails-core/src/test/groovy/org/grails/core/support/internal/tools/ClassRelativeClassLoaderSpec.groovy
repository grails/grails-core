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
package org.grails.core.support.internal.tools

import spock.lang.Specification

/**
 * Created by graemerocher on 30/09/2016.
 */
class ClassRelativeClassLoaderSpec extends Specification {

    void "test class relative classloader"() {
        when:"A classloader is created for only resources relative to this class"
        def classLoader = new ClassRelativeClassLoader(ClassRelativeClassLoaderSpec)

        then:"The resources are found"
        classLoader.getResource('org/grails/core/support/internal/tools/ClassRelativeClassLoaderSpec.class')

        and:"other classpath resources are not found"
        !classLoader.getResource('springloaded.properties')
    }
}
