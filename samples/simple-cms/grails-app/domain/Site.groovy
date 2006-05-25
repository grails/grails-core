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
 * Defines a Site, a site has a name and a domain. The domain is used to look-up
 * the look-and-feel of the site and requires appriate DNS configuration on the server
 * The CMS will look-up the site for a particular domain, if none is found it will
 * call the default site (localhost)
 *
 * @author Graeme Rocher
 * @since 10-May-2006
 */
class Site {
    // name of the default site
    static final DEFAULT = ApplicationConfig.DEFAULT_SITE

	@Property Long id
	@Property Long version

	@Property optionals = ['homePage']

	@Property Page homePage
	@Property String name
	@Property String domain
	
    String toString() { "${name}" }

	@Property constraints = {
		name(blank:false,length:1..50)
		domain(blank:false,length:1..50)
	}

    boolean equals(other) {
		if(this.is(other))return true
		if(!(other instanceof Site)) return false
		
		if(this.name == other.name && this.domain == other.domain)
			return true
		
		return false
    }

    int hashCode() { 
		int result = 23
		if(name) {
			result *= 37
			result += name.hashCode()
		}
		if(domain) {
			result *= 37
			result += domain.hashCode()
		}
		return result		
	}
}	
