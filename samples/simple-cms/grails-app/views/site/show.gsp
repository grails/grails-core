
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Show Site</title>
         <g:javascript library="yahoo"></g:javascript>
		 <g:javascript library="treeview"></g:javascript>   
		 <script type="text/javascript" src="${createLinkTo(dir:'fckeditor',file:'fckeditor.js')}"></script>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
         <link rel="stylesheet" href="${createLinkTo(dir:'css/tree/default',file:'tree.css')}"></link>         
		 <g:javascript>
		 	var contextId = null;
			var contextTitle = null;
		 	var contextShowClick = true;
		 	// I miss this from prototype when using Yahoo :(
		 	function $(id) {
		 		return YAHOO.util.Dom.get(id);
		 	}
			function moveDown() {
				YAHOO.util.Connect.asyncRequest('GET','${createLink(controller:"page",action:"moveDown")}/' + contextId,{success: handleMoveDown},null);			
			}
			function moveUp() {
				YAHOO.util.Connect.asyncRequest('GET','${createLink(controller:"page",action:"moveUp")}/' + contextId,{success: handleMoveUp},null);				
			}
			function addLink(parentId,linkId,linkText) {
				setTimeout(function() {
					YAHOO.util.Connect.asyncRequest('POST','${createLink(controller:"page",action:"saveLink")}',{success: handleSaveLink,failure:handleSaveLink},'parent.id='+parentId+'&link.id='+linkId+'&linkText='+linkText);
				},100);
			}
			function handleSaveLink(o) {
					handleAddPage(o);
			}
			function handleEditorPreview() {
				window.open("${createLink(controller:'page',action:'preview')}/"+$('pageId').value, 'addLinkWindow','width=800,height=600,toolbar=yes,menubar=yes,scrollbars=yes');
			}				
			function handleMoveUp(o) {
				var pageXml = o.responseXML;
				if(!checkForErrors(pageXml)) {
					var page = pageXml.getElementsByTagName("page")[0];
					var id = page.attributes[0].value;
					var parentId = page.attributes[1].value;
					
					var parentNode = siteTree.getNodeByProperty('id',parentId);
					// find the child node for the id
					for(var i=0;i<parentNode.children.length;i++) {
						if(parentNode.children[i].data.id == id) {
							if(i != 0) {
								var child = parentNode.children[i];
								var prev = parentNode.children[i-1];
								parentNode.children[i] = prev;
								parentNode.children[i-1] = child;
								break;							
							}
						}
					}
					siteTree.draw();
					siteTree.resetEvents();
				}				
			}
			function handleMoveDown(o) {
				var pageXml = o.responseXML;
				if(!checkForErrors(pageXml)) {
					var page = pageXml.getElementsByTagName("page")[0];
					var id = page.attributes[0].value;
					var parentId = page.attributes[1].value;
					
					var parentNode = siteTree.getNodeByProperty('id',parentId);
					// find the child node for the id
					for(var i=0;i<parentNode.children.length;i++) {
						if(parentNode.children[i].data.id == id) {
							if(i != parentNode.children.length) {
								var child = parentNode.children[i];
								var next = parentNode.children[i+1];
								parentNode.children[i] = next;
								parentNode.children[i+1] = child;
								break;							
							}
						}
					}
					siteTree.draw();
					siteTree.resetEvents();
				}				
			}			
			function checkForErrors(xml) {
				if(xml == null) return true;
				var errors = xml.getElementsByTagName("error");
				if(errors.length > 0) {					
				   $('error').innerHTML = errors[0].attributes[0].value;
				   $('error').style.display = '';
				   setTimeout(function() {
				   		$('error').style.display = 'none';
				   },6000);
				   return true;
				}			
				return false;
			}
			function checkForMessages(xml) {
				if(xml == null) return false;
				var messages = xml.getElementsByTagName("alert");
				if(messages.length > 0) {					
				   $('message').innerHTML = messages[0].attributes[0].value;
				   $('message').style.display = '';
				   setTimeout(function() {
				   		$('message').style.display = 'none';
				   },6000);
				   return true;
				}			
				return false;			
			}
			function handleDeletePage(o) {
				var pageXml = o.responseXML;
				if(!checkForErrors(pageXml)) {
					checkForMessages(pageXml);
					var id = pageXml.getElementsByTagName('page')[0].attributes[0].value;
					displayPage(id);
				}
			}
			function handleAddPage(o) {
				var pageXml = o.responseXML;
				if(!checkForErrors(pageXml)) {
					checkForMessages(pageXml);
					if(pageXml!=null) {
						var page = pageXml.getElementsByTagName("page")[0];
						var parentNode = siteTree.getNodeByProperty('id',page.attributes[1].value);
						var id = page.attributes[0].value;
						var label = pageXml.getElementsByTagName("title")[0].firstChild.nodeValue;
						var tmpNodeProps = { label: label, id: id };
						var tmpNode = new YAHOO.widget.TextNode(tmpNodeProps, parentNode, false);
						siteTree.draw();
						siteTree.resetEvents();
						parentNode.expand();
						displayPage(id);					
					}
				}				
			}
		 	function handleClick(e) {
		 		var rightClick = true;
				var node = siteTree.getNodeByProperty('label',this.innerHTML);
				var labelId = node.data.id;
				$('addParentId').value = labelId;
				$('deletePageId').value = labelId;
				if(rightClick) {
					contextId = labelId;
					contextTitle = this.innerHTML;
					var xy = YAHOO.util.Dom.getXY(this);
					var cm = document.getElementById('contextMenu');
									
					cm.style.display = '';                    
					
					
					YAHOO.util.Dom.setX('contextMenu',xy[0] + (this.offsetWidth/2))
					YAHOO.util.Dom.setY('contextMenu',xy[1] + this.offsetHeight)						
					contextShowClick = false;
				}
         		displayPage(labelId);				
				return false;				
			}
			function displayPage(id) {
					var callback =
					{
					  success: handleDisplayPage,
					  argument: "id=" + id
					}
					
					YAHOO.util.Connect.asyncRequest('GET','${createLink(action:"displayPage")}/' + id,callback,null);			
			}
			var i = 0;		
			function handlePreview() {
				alert('previewing!');
			}
			function handleDisplayPage(o) {

				var pageXml = o.responseXML;
				if(pageXml!=null) {                 
					
					
					$('page').value = pageXml.getElementsByTagName('page')[0].getAttribute('id');
					$('pageTitle').innerHTML = pageXml.getElementsByTagName("title")[0].firstChild.nodeValue;
					$('dialog').innerHTML = pageXml.getElementsByTagName("details")[0].firstChild.nodeValue;
					var pageType = pageXml.getElementsByTagName('page')[0].getAttribute('type');
					if($('UndoButton')!=null) {
						$('UndoButton').onclick = function() {
							if(confirm('Are you sure you want to rollback the current changes?')) {
								var remoteSubmit = function(){<g:remoteFunction onSuccess="handleUndo" url="[controller:'page',action:'rollback']" />}
								YAHOO.util.Connect.setForm('pageControls');
								remoteSubmit();							
							}						
							return false;
						}					
					}
					if($('ApproveButton')!=null) {
						$('ApproveButton').onclick = function() {
								var remoteSubmit = function() {<g:remoteFunction onSuccess="handleUndo" url="[controller:'page',action:'approve']" />}
								YAHOO.util.Connect.setForm('pageControls');
																
								remoteSubmit();						
							return false;
						}					
					}		
					if($('PublishButton')!=null) {
						$('PublishButton').onclick = function() {
								var remoteSubmit = function() {<g:remoteFunction onSuccess="handleUndo" url="[controller:'page',action:'publish']" />}
								YAHOO.util.Connect.setForm('pageControls');
								remoteSubmit();						
							return false;
						}					
					}						
					if($('RejectButton') !=null) {
						$('RejectButton').onclick = function() {
								var remoteSubmit = function(){<g:remoteFunction onSuccess="handleUndo" url="[controller:'page',action:'reject']" />}
								YAHOO.util.Connect.setForm('pageControls');
								remoteSubmit();						
							return false;
						}					
					}
					$('siteButtons').style.display = 'none';
					
					var comments = pageXml.getElementsByTagName("comments")[0].firstChild;
					var content = pageXml.getElementsByTagName("content")[0].firstChild;
					
					if(comments !=null) {
						$('comments').innerHTML = comments.nodeValue;
					}
					$('addCommentLink').onclick = function() {
						$('commentBox').style.display='';
					}					
						
					if(pageType == "${Page.LINK}") {
						$('editor').style.visibility = "hidden";
						$('description').innerHTML = content.nodeValue;
						$('description').style.display='';
					}
					else {
						var oEditor = FCKeditorAPI.GetInstance('editor') ;
											
						if(content != null)
							oEditor.SetHTML( content.nodeValue );
						else
							oEditor.SetHTML( "" );
						
						$('description').style.display='none';
						$('editor').style.visibility = 'visible';					
					}				
				}
			}
			function handleUndo(o) {
				if(!checkForErrors(o.responseXML)) {
					checkForMessages(o.responseXML);
					handleDisplayPage(o);
				}			
			}
			function handleSave(o) {
				if(!checkForErrors(o.responseXML)) {
					checkForMessages(o.responseXML);
					handleDisplayPage(o);
				}
			}
		 	function handleExpand(node) {
		 		var el = document.getElementById(node.labelElId)

		 	}
			function getEditorValue() {
				var oEditor = FCKeditorAPI.GetInstance('editor') ;			
				document.forms['editorForm'].elements['editor'].value = oEditor.GetXHTML(true);
				
				$('pageComment').value = $('commentField').value
			}
			function editorLoaded(o)
			{
				$("loader").style.display = 'none';
			}	
			function pageLoaded() {
				YAHOO.util.Event.addListener('addLink', 'click',function() {
					$('contextMenu').style.display='none';
					window.open("${createLink(controller:'page',action:'addLink')}?parent.id="+contextId+"&site.id=${site?.id}", 'addLinkWindow','width=400,height=450,toolbar=no,menubar=no,scrollbars=yes');
				});
				YAHOO.util.Event.addListener('addPage', 'click',function() {
					$('pageType').value = "${Page.STANDARD}";
					showDialog('addPageDialog');
				});
				YAHOO.util.Event.addListener('addForum', 'click',function() {
					$('pageType').value = "${Page.FORUM}";
					showDialog('addPageDialog');
				});		
				YAHOO.util.Event.addListener('addQuestionnaire', 'click',function() {
					$('pageType').value = "${Page.QUESTIONNAIRE}";
					showDialog('addPageDialog');
				});						
				YAHOO.util.Event.addListener('deletePage', 'click',function() {					
					showDialog('deletePageDialog');
				});				
				YAHOO.util.Event.addListener(document, 'click', function() {
					var cm = $('contextMenu');
					if(!contextShowClick) {
						contextShowClick = true;
					}
					else {
						cm.style.display = 'none';					
					}
					
				});
			}
			
			function showDialog(id) {
				var cm = document.getElementById('contextMenu');
				var xy = YAHOO.util.Dom.getXY(cm);
				var apd = document.getElementById(id);				
				apd.style.display = '';
				xy[0] += cm.offsetWidth;
				YAHOO.util.Dom.setXY(apd,xy);			
			}
			YAHOO.util.Event.addListener(window, 'load', pageLoaded);
			YAHOO.util.Event.addListener(window, 'contextmenu', function() {return false;});
		 </g:javascript>
    </head>
    <body oncontextmenu="return false;">
		<div id="loader" class="loader" style="position:absolute;top:300px;right:300px;">Loading...</div>
		<div id="contextMenu" class="contextMenu" style="position:absolute;display:none;">
				<div id="addPage"><a href="#">Add Page</a></div>
				<div id="addForum"><a href="#">Add Forum</a></div>				
				<div id="addQuestionnaire"><a href="#">Add Questionnaire</a></div>								
				<div id="addLink"><a href="#">Add Link</a></div>	
				-------------------
				<div id="deletePage"><a href="#">Delete</a></div>								
				<div id="moveUp"><a href="#" onclick="moveUp()">Move Up</a></div>
				<div id="moveDown"><a href="#" onclick="moveDown()">Move Down</a></div>				
		</div>
		<g:dialog id="deletePageDialog" title="Confirm Delete">
			<g:formRemote 	name="deletePageForm" 
							after="\$('deletePageDialog').style.display = 'none';" 
							url="[controller:'page',action:'delete']"
							onSuccess="handleDeletePage">
				 <input id="deletePageId" type="hidden" name="id" value="${site?.homePage?.id}" />
                <table>
                      <tr class='prop'>
                      	<td valign='top' style='text-align:left;' width='20%'>
                      		<label for='name'>Comments:</label>
                      	</td>
                      	<td valign='top' style='text-align:left;'>
                      		<textarea name='comment'></textarea>
                      	</td></tr>    												
               </table>
               <div class="buttons">
	               <input type="submit" value="Delete" />				
	           </div>							
			</g:formRemote>
		</g:dialog>
		<g:dialog id="addPageDialog" title="Add Page">
			<g:formRemote 	name="addPageForm" 
							after="\$('addPageDialog').style.display = 'none';" 
							url="[controller:'page',action:'add']"
							onSuccess="handleAddPage">
				<input type="hidden" name="site.id" value="${site?.id}" />
				<input id="pageType" type="hidden" name="type" value="${Page.STANDARD}" />
				<input type="hidden" id="addParentId" name="parent.id" value="" />
                <table>
                      <tr class='prop'>
                      	<td valign='top' style='text-align:left;' width='20%'>
                      		<label for='title'>Title:</label>
                      	</td>
                      	<td valign='top' style='text-align:left;'>
                      		<input type="text" name='title'></input>
                      	</td></tr>     
                      <tr class='prop'>
                      	<td valign='top' style='text-align:left;' width='20%'>
                      		<label for='comment'>Comments:</label>
                      	</td>
                      	<td valign='top' style='text-align:left;'>
                      		<textarea name='comment'></textarea>
                      	</td></tr>    												
               </table>
               <div class="buttons">
	               <input type="submit" value="Add" />				
	           </div>
			</g:formRemote>
		</g:dialog>
       <div class="tree" style="float:left;position:absolute;left:10px;">
			<br />       		
       		Site Structure: <br />	
   		        <g:tree 
					id="siteTree" 
					root="${site.homePage}" 
					onExpand="handleExpand"
					dynamicLoadUri="[action:'loadSubPages']"
					onNodeMouseUp="handleClick" />    	
       </div>        
        <div class="body" style="margin-left:175px;">
			<div id="error" style="display:none;" class="error"></div>
			<div id="message" style="display:none;" class="message"></div>
           <h1 id="pageTitle">${site?.name} Site</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>

           <div id="dialog" class="dialog">
                 <table>
                   
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Id:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${site.id}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Name:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${site.name}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Domain:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${site.domain}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Home Page:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value"><a href="#" onclick="displayPage(${site?.homePage?.id});">${site?.homePage}</s></td>
                              
                        </tr>
                   
                 </table>
           </div>
           <div id="siteButtons" class="buttons">
               <g:form controller="site">
                 <input type="hidden" name="id" value="${site?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
               </g:form>
           </div>
			<div id="description"></div>			   
			<div id="editor" style="visibility:hidden;">
				<h2>Content Editor</h2>
				<g:formRemote name="editorForm"
							   url="[controller:'page',action:'save']"
							   onSuccess="handleSave"
							   before="getEditorValue()">
					<input type="hidden" name="user" value="${session.user?.id}" />
					<input type="hidden" id="page" name="page" value="${site?.homePage?.id}" />
					<input type="hidden" id="pageComment" name="comment" value="" />
					<g:richTextEditor name="editor" height="400" onComplete="editorLoaded" />					
				</g:formRemote>
			</div>	
			<div id="comments"></div>		
        </div>
    </body>
</html>
            