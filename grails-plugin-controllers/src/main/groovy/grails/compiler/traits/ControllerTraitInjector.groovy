/*
 * Copyright 2014 the original author or authors.
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

import grails.artefact.Controller
import groovy.transform.CompileStatic

import java.util.regex.Pattern

import org.grails.io.support.GrailsResourceUtils

/**
 *
 * A {@link TraitInjector} that injects controllers with the {@link Controller} trait
 *
 * @author Jeff Brown
 * @author Graeme Rocher
 *
 * @since 3.0
 *
 */
@CompileStatic
class ControllerTraitInjector implements TraitInjector {
    
    static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
        GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");
 
    @Override
    Class getTrait() {
        Controller
    }
 
    @Override
    boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
 
    @Override
    String[] getArtefactTypes() {
        ['Controller'] as String[]
    }
}
