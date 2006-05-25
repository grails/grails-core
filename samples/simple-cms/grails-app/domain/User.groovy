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
 * A User within the system. Each user has a role and an optional AccessKey
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class User {
    @Property belongsTo = [Role,AccessKey]

	@Property Long id
	@Property Long version
	
	@Property optionals = ['location','accessKey']

	@Property String title
	@Property String firstName
	@Property String lastName
	@Property String email
	@Property String company
	@Property String login
	@Property String pwd
	@Property Role role
	@Property String location
	@Property AccessKey accessKey
	@Property Boolean active = true
	
    String toString() { "$firstName $lastName" }

    @Property constraints = {
        title(length:1..10)
        firstName(blank:false)
        lastName(blank:false)
        email(email:true)
        company(blank:false)
        login(blank:false,unique:true)
        pwd(blank:false,password:true)
        role(nullable:false)		
    }
}	

