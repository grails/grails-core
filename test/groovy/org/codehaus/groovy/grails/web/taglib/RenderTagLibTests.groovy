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
package org.codehaus.groovy.grails.web.taglib;

import org.springframework.web.servlet.support.RequestContextUtils as RCU;

/**
 * Tests for the RenderTagLib.groovy file which contains tags for rendering
 *
 * @author Marcel Overdijk
 *
 */
class RenderTagLibTests extends AbstractGrailsTagTests {

	void testSortableColumnTag() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
	}
	
	void testSortableColumnTagWithTitleKey() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

		// test message not resolved; title property will be used (when provided)

		// without (default) title property provided
		withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", titleKey:"book.title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >book.title</a></th>', sw.toString()
    	}

        sw = new StringWriter();
    	pw = new PrintWriter(sw); 

		// with (default) title property provided
		withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", title:"Title", titleKey:"book.title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
		
		// test message resolved 

        sw = new StringWriter();
    	pw = new PrintWriter(sw); 
    	
		messageSource.addMessage("book.title", RCU.getLocale(request), "Book Title") 

    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", title:"Title", titleKey:"book.title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >Book Title</a></th>', sw.toString()
    	}
	}

	void testSortableColumnTagWithAction() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([action:"list2", property:"title", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list2?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
	}
	
	void testSortableColumnTagWithDefaultOrder() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

		// default order: desc

    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", defaultOrder:"desc", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=desc" >Title</a></th>', sw.toString()
    	}
    	
		// default order: asc
    	
        sw = new StringWriter();
    	pw = new PrintWriter(sw); 
    	
		withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", defaultOrder:"asc", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
    	
    	// invalid default order
    	
        sw = new StringWriter();
    	pw = new PrintWriter(sw); 
    	
		withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", defaultOrder:"invalid", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
	}
	
	void testSortableColumnTagWithAdditionalAttributes() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// use sorted map to be able to predict the order in which tag attributes are generated
			// adding the class property is a dirty hack to predict the order; it will be overridden in the tag anyway
			def attrs = new TreeMap([property:"title", title:"Title", class:"will be overridden", style:"width: 200px;"])
			tag.call(attrs)
			assertEquals '<th class="sortable" style="width: 200px;" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
	}
	
	void testSortableColumnTagSorted() {
    	final StringWriter sw = new StringWriter();
    	final PrintWriter pw = new PrintWriter(sw);

		// column sorted asc
		
    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// set request params
			webRequest.getParams().put("sort", "title")
			webRequest.getParams().put("order", "asc")
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable sorted asc" ><a href="/book/list?sort=title&order=desc" >Title</a></th>', sw.toString()
    	}
    	
		// column sorted desc
    	
    	sw = new StringWriter();
    	pw = new PrintWriter(sw);
    	
    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// set request params
			webRequest.getParams().put("sort", "title")
			webRequest.getParams().put("order", "desc")
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable sorted desc" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
    	
    	// other column sorted
    	
        sw = new StringWriter();
    	pw = new PrintWriter(sw);
    	
    	withTag("sortableColumn", pw) { tag ->
			webRequest.controllerName = "book" 
			// set request params
			webRequest.getParams().put("sort", "price")
			webRequest.getParams().put("order", "desc")
			// use sorted map to be able to predict the order in which tag attributes are generated
			def attrs = new TreeMap([property:"title", title:"Title"])
			tag.call(attrs)
			assertEquals '<th class="sortable" ><a href="/book/list?sort=title&order=asc" >Title</a></th>', sw.toString()
    	}
	}
}
