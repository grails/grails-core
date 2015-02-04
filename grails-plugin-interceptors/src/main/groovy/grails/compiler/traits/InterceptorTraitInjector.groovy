/*
 * Copyright 2015 original authors
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
package grails.compiler.traits

import grails.artefact.Interceptor
import groovy.transform.CompileStatic
import org.grails.io.support.GrailsResourceUtils

import java.util.regex.Pattern


/**
 * Injects the {@link Interceptor} trait by convention
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class InterceptorTraitInjector implements TraitInjector {

    static Pattern INTERCEPTOR_PATTERN = Pattern.compile(".+/" +
            GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Interceptor\\.groovy");

    @Override
    Class getTrait() {
        Interceptor
    }

    @Override
    boolean shouldInject(URL url) {
        return url != null && INTERCEPTOR_PATTERN.matcher(url.getFile()).find();
    }

    @Override
    String[] getArtefactTypes() {
        ['Interceptor'] as String[]
    }
}
