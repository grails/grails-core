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

package org.codehaus.groovy.grails.compiler.injection

/**
 * Abstract transformer that takes an implementation class and creates methods
 * in a target ClassNode that delegate to that implementation class. Subclasses
 * should override to provide the implementation class details
 *
 * @since 2.0
 * @author  Graeme Rocher
 * @deprecated Use {@link org.grails.compiler.injection.AbstractGrailsArtefactTransformer} instead
 */
@Deprecated
abstract class AbstractGrailsArtefactTransformer extends org.grails.compiler.injection.AbstractGrailsArtefactTransformer{
}
