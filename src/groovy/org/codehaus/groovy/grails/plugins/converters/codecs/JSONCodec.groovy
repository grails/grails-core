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
package org.codehaus.groovy.grails.plugins.converters.codecs

/**
 * A Grails codec capability of converting an object to JSON

 * @author Siegfried Puchbauer
 * @since 0.6
  *
 * Created: Aug 3, 2007
 * Time: 5:54:23 PM
 *
 */
import grails.converters.JSON

class JSONCodec {

	static encode = { target ->
		return new JSON(target).toString(true)
	}

}