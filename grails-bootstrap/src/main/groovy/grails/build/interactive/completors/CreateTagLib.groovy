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
import org.codehaus.groovy.grails.io.support.GrailsResourceUtils
import org.springframework.core.io.Resource

/**
 * A completor for the create-tag-lib command
 */
class CreateTagLib extends ClassNameCompletor{
    @Override
    String getCommandName() { "create-tag-lib" }

    @Override
    boolean shouldInclude(Resource res) {
        GrailsResourceUtils.isDomainClass(res.getURL())
    }


}
