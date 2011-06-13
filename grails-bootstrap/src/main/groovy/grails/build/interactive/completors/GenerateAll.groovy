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
package grails.build.interactive.completors

import org.codehaus.groovy.grails.cli.interactive.completors.ClassNameCompletor
import org.springframework.core.io.Resource
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils

/**
 * Completor for the generate-all command
 *
 * @author Graeme Rocher
 * @since 1.4
 *
 */
class GenerateAll extends ClassNameCompletor{
    @Override
    String getCommandName() { "generate-all" }

    @Override
    boolean shouldInclude(Resource res) {
        GrailsResourceUtils.isDomainClass(res.getURL())
    }


}
