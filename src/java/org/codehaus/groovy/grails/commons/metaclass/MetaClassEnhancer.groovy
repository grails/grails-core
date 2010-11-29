/*
* Copyright 2010 the original author or authors.
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

package org.codehaus.groovy.grails.commons.metaclass

import java.lang.reflect.Method;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;

/**
* Enhances one or many MetaClasses with the given API methods provided by the super class BaseApiProvider
*
* @author Graeme Rocher
* @since 1.4
*
*/
class MetaClassEnhancer extends BaseApiProvider {

	public void enhance(MetaClass metaClass) {
		for (MetaMethod method : instanceMethods) {
			metaClass.registerInstanceMethod(method)
		}

		for (Method method : staticMethods) {
			def methodName = method.name
			metaClass."${methodName}" = method.declaringClass.&"${methodName}"
		}
	}
	
	public void enhanceAll(Iterable metaClasses) {
		for(metaClass in metaClasses) {
			enhance metaClass
		}
	}
}
