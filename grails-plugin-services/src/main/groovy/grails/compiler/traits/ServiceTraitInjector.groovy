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

import grails.artefact.Service
import org.grails.io.support.GrailsResourceUtils

import java.util.regex.Pattern


/**
 * A {@link TraitInjector} for services
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class ServiceTraitInjector implements TraitInjector {

    static Pattern SERVICE_PATTERN = Pattern.compile(".+/" +
            GrailsResourceUtils.GRAILS_APP_DIR + "/services/(.+)Service\\.groovy");

    @Override
    Class getTrait() {
        Service
    }

    @Override
    boolean shouldInject(URL url) {
        return url != null && SERVICE_PATTERN.matcher(url.getFile()).find();
    }

    @Override
    String[] getArtefactTypes() {
        ['Service'] as String[]
    }
}
