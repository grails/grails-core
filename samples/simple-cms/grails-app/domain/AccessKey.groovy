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
 
/**
 * Represents an access key. An access key is sometimes required when a user
 * registers allowing access for a certain period and assigning them a particular role
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class AccessKey { 
	@Property Long id
	@Property Long version
	
	@Property belongsTo = [User,Site]

	@Property Role role
	@Property Date created = new Date()
	@Property Date startDate
	@Property Date endDate
	@Property Date expiryDate
	@Property Integer usages = 1
	@Property String code
	@Property Site site
	
	AccessKey() {
		def r = new Random()
		def sb = new StringBuffer()
		sb << r.nextInt(9999)
		sb << r.nextInt(9999)
		sb << r.nextInt(9999)
		sb << r.nextInt(9999)

		code = sb.toString()
	}
    String toString() { "${this.class.name} :  $id" }

	@Property constraints = {
		startDate(nullable:false)
		endDate(nullable:false)
		expiryDate(nullable:false)
		usages(minSize:0)
		code(blank:false,maxLength:16)
	}
}	
