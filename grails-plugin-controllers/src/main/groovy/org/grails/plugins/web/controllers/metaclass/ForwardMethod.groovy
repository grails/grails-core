/*
 * Copyright 2004-2005 the original author or authors.
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
package org.grails.plugins.web.controllers.metaclass

import groovy.transform.CompileStatic
import org.grails.web.util.GrailsApplicationAttributes

/**
 * Implements performing a forward.
 *
 * @author Graeme Rocher
 * @since 1.1
 *
 * @deprecated Use {@link GrailsApplicationAttributes} instead
 */
@CompileStatic
@Deprecated
class ForwardMethod {
    public static final String IN_PROGRESS = GrailsApplicationAttributes.FORWARD_IN_PROGRESS
    public static final String CALLED = GrailsApplicationAttributes.FORWARD_ISSUED
}
