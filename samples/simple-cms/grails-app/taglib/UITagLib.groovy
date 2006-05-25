/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT c;pWARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.springframework.validation.Errors;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.support.RequestContextUtils as RCU;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;

/**
*  A  tag lib that provides UI widgets for use with Grails
*
* @author Graeme Rocher
* @since 24-March-2006
*/
class UITagLib {

    /**
     * A tree widget based on the Yahoo UI library
     **/
	@Property tree = { attrs, body ->
		if(!request[JavascriptTagLib.INCLUDED_LIBRARIES] || !request[JavascriptTagLib.INCLUDED_LIBRARIES].contains('yahoo')) {
			out << '<script type="text/javascript">alert("Grails Message: The tree widget requires the \'yahoo\' and \'treeview\' libraries to be included with the <g:javascript library=\'yahooTree\' /> tag");</script>'		
		}
		else {
	        if(!attrs.id)
	            throwTagError("Tag [tree] is missing required attribute [id]")		
	        if(!attrs.root)
	            throwTagError("Tag [tree] is missing required attribute [root]")			            
	          
			def nodeEvents = attrs.findAll { k,v -> k ==~ /^onNode\w*/ }
			
			out << """
			<div id='${attrs.id}'></div>
			<script type=\"text/javascript\">
					YAHOO.widget.TreeView.prototype.resetEvents = function(children) {
						if(children == null)
							children = this.getRoot().children;	
							for(var i = 0;i<children.length;i++) {
								"""
								nodeEvents.each { k,v ->
									def name = k[6..k.size()-1].toLowerCase()
									out.println "YAHOO.util.Event.addListener(children[i].labelElId, '$name', $v);"
								}								
			out << """
								if(children[i].hasChildren(false)) {
									this.resetEvents(children[i].children);	
								}
							}
						
					}
					var ${attrs.id} = new YAHOO.widget.TreeView(\"${attrs.id}\")
					${attrs.id}.setExpandAnim(YAHOO.widget.TVAnim.FADE_IN);
					${attrs.id}.setCollapseAnim(YAHOO.widget.TVAnim.FADE_OUT);"""					

			if(attrs.onExpand) {
				out << """
				${attrs.id}.onExpand =${attrs.onExpand}
				"""				
			}		
			if(attrs.onCollapse) {
				out << """
				${attrs.id}.onCollapse = ${attrs.onCollapse}
				"""				
			}						
			if(attrs.root) {
				out.println "var ${attrs.id}Root = new YAHOO.widget.TextNode({label:'$attrs.root',id:'${attrs.root.ident()}'},${attrs.id}.getRoot(), false);"
				
				
				nodeEvents.each { k,v ->
					def name = k[6..k.size()-1].toLowerCase()
					out.println "YAHOO.util.Event.addListener(${attrs.id}Root.labelElId, '$name', $v);"
				}
		
				if(attrs.childrenProperty) {
					def children = attrs.root.getProperty(attrs.childrenProperty)
					buildTreeFromChildren(children,"${attrs.id}Root",attrs.childrenProperty,1)
				}
				else if(attrs.dynamicLoadUri) {
					out.println "${attrs.id}.setDynamicLoad(${attrs.id}DynamicTreeLoad);"							
					def method = (attrs.method? attrs.method : 'GET')
					out << """
						function ${attrs.id}DynamicTreeLoad(node,onCompleteCallback)  {
							var cObj = YAHOO.util.Connect.asyncRequest('$method', '"""
							
					createLink(attrs.dynamicLoadUri)
				
					out << """/' + node.data.id, {success:function(o) {
								var nodes = eval(o.responseText);								
								var tmpNode = null;
								for(var i = 0; i < nodes.length;i++) {
									var tmpNodeProps = { label: nodes[i].label, id: nodes[i].id }
									tmpNode = new YAHOO.widget.TextNode(tmpNodeProps, node, false);

								}
							
								onCompleteCallback();"""
					nodeEvents.each { k,v ->
						def name = k[6..k.size()-1].toLowerCase()
						out.println "for(var i = 0; i < node.children.length;i++) {"
							out.println "YAHOO.util.Event.addListener(node.children[i].labelElId, '$name', $v);"
						out.println "}"
					}								

     							
					out <<	"""}});																			
						}
					"""
				}
				else {
		            throwTagError("Tag [tree] requires one of either the [dynamicLoadUri] or [childrenProperty] attributes to be specified")							
				}
			}
			out <<	"""
				${attrs.id}.draw()
			</script>"""
		}			
	}
	
	// Recursive method that traverses an object graphic building up the tree
	def buildTreeFromChildren(children,parentName,childrenPropertyName,index) {
		if(children) {
			children.each { child ->
				def label = child.toString()
			
				out.println "var node${index} = new YAHOO.widget.TextNode({label:'$label',id:'${child.ident()}'}, ${parentName}, false);"				
				def moreChildren = child.getProperty(childrenPropertyName)
				if(moreChildren) {
					buildTreeFromChildren(moreChildren,"node${index}",childrenPropertyName,++index)
					index = index + moreChildren.size()					
				}											
			}					
		}	
	}
	
	/**
	 * A Rich Text Editor component that by default uses fckeditor with a basepath of /fckeditor.
	 * TODO: Add support for other rich text editing components like those from the Dojo framework
	 *
	 * Example:
	 *
	 * <g:richTextEditor name="editor" height="400" />
	 */
	@Property richTextEditor = { attrs ->
		withTag(name:'script',attributes:[type:'text/javascript']) {
			if(attrs.onComplete) {
				out.println "function FCKeditor_OnComplete( editorInstance ) {"
					out.println "${attrs.onComplete}(editorInstance);"					
				out.println "}"
			}
			out << """
			var oFCKeditor = new FCKeditor( '${attrs.name}' ) ;
			oFCKeditor.BasePath	 = \""""
			if(attrs.basepath) {
				createLinkTo(dir:attrs.basepath)
			}
			else {
				createLinkTo(dir:"fckeditor/")
			}
			out.println '";'
			if(attrs.toolbar) {
				out << "oFCKeditor.ToolbarSet	 = '${attrs.toolbar}';" 	
			}			
			if(attrs.height)			
				out.println "oFCKeditor.Height	= ${attrs.height};"
			if(attrs.value) {
				out << "oFCKeditor.Value	= \""
				escapeJavascript(Collections.EMPTY_MAP,attrs.value)
				out.println "\" ;"
			}
			
			out.println "oFCKeditor.Create();"			
		}
	}
}