/*
 * Copyright 2013 the original author or authors.
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
package grails.rest

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * A transform that makes it possible to attach {@link Link} instances to any class. Two methods are added, one called link(Map args) and another called links() to retrieve the links.
 *
 * Example:
 *
 * <pre>
 *     <code>
 *          @Linkable class Book {
 *              String title
 *          }
 *
 *          def b = new Book()
 *          b.link(rel:"publisher", href="http://foo.com/books")
 *          println b.links()
 *
 *     </code>
 * </pre>
 *
 *
 * @author Graeme Rocher
 * @since 2.3
 */

@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE])
@GroovyASTTransformationClass("org.grails.plugins.web.rest.transform.LinkableTransform")
public @interface Linkable {

}
