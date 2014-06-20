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
package org.grails.compiler.web.async;

import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import grails.compiler.ast.AstTransformer;
import org.codehaus.groovy.grails.compiler.web.ControllerTransformer;
import org.grails.plugins.web.async.api.ControllersAsyncApi;

import java.net.URL;

/**
 * Adds the the controller async APIs to Grails at compile time.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class ControllerAsyncTransformer extends AbstractGrailsArtefactTransformer {

    @Override
    protected boolean requiresAutowiring() {
        return false;
    }

    @Override
    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllersAsyncApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // none
    }

    public boolean shouldInject(URL url) {
        return url != null && ControllerTransformer.CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }
}
