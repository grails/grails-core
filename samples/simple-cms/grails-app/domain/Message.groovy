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
 * A Message class, messages are left within Topics
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Message { 
	 static belongsTo = [Topic,Page,User]
	
	 Topic topic
	 String title
	 String message
	 User by
	 Date datePosted = new Date()
	
	 static constraints = {
		title(size:1..50)
		message(blank:false)
		topic(nullable:false)
		by(nullable:false)
	}
}	
