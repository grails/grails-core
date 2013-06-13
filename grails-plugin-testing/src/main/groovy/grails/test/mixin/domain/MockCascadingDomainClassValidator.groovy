/*
 * Copyright 2012 GoPivotal, Inc. All Rights Reserved
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
package grails.test.mixin.domain

import org.grails.datastore.gorm.validation.CascadingValidator
import org.codehaus.groovy.grails.validation.GrailsDomainClassValidator

/**
 * Integrates Grails cascading validation with datastore API
 *
 * @author Graeme Rocher
 * @since 2.1.1
 */
class MockCascadingDomainClassValidator extends GrailsDomainClassValidator implements CascadingValidator  {
}
