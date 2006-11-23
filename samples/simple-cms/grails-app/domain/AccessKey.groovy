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
	 static belongsTo = [User,Site]

	 Role role
	 Date created = new Date()
	 Date startDate
	 Date endDate
	 Date expiryDate
	 Integer usages = 1
	 String code
	 Site site
	
	AccessKey() {
		def r = new Random()
		def sb = new StringBuffer()
		sb << r.nextInt(9999)
		sb << r.nextInt(9999)
		sb << r.nextInt(9999)
		sb << r.nextInt(9999)

		code = sb.toString()
	}

	static constraints = {
		startDate(nullable:false)
		endDate(nullable:false)
		expiryDate(nullable:false)
		usages(minSize:0)
		code(blank:false,maxLength:16)
	}
}	
