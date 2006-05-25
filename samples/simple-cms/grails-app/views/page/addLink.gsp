<html>
	<head>
		<title>Add Link Dialog</title>
		<g:javascript library="yahoo"></g:javascript>
		<g:javascript library="treeview"></g:javascript>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
         <link rel="stylesheet" href="${createLinkTo(dir:'css/tree/default',file:'tree.css')}"></link>
		<g:javascript>
			function $(id) {
				return YAHOO.util.Dom.get(id);
			}
			function nextStep() {
				if($('internal').checked) {
					$('step1').style.display='none';
					$('step2a').style.display='';
				}
				else if($('external').checked) {
					$('step1').style.display='none';
					$('step2b').style.display='';				
				}
				else {
					alert('Please select whether it is an internal or external link');
				}
			}
			function handleLinkSelect() {
				var linkText = $('linkText1');
				if(linkText.value.length==0) {
					alert("The 'Link Text' field cannot be blank");
				}
				else {
					if(confirm("Create link to '"+this.innerHTML+"'?")) {
						var node = linkTree.getNodeByProperty('label',this.innerHTML);
						var id = node.data.id;	
						if(window.opener!=null) {
							window.opener.addLink(${parent?.id},id,linkText.value)
							window.close();
						}
						else {
							alert('This page only works when opened from the site editor. Please close this window and return to the site editor.');
						}
					}				
				}
			}
			function saveExternalLink() {
				var linkText = $('linkText2');
				var linkURL =  $('linkURL');
				if(linkText.value.length==0) {
					alert("The 'Link Text' field cannot be blank");
				}
				else if(linkURL.value.length==0) {
					alert("The 'Link URL' field cannot be blank");
				}
				else {
					window.opener.addLink(${parent?.id},linkURL.value,linkText.value)
					window.close();				
				}
			}
			function switchSite(select) {
				var siteId = select.options[select.selectedIndex].value
				location.href = "${createLink(action:'addLink')}?parent.id=${parent?.id}&site.id="+siteId;
			}
		</g:javascript>
	</head>
<body>	
	<div class="body">
		<h2>Create Link</h2>
	   <div id="step1">
	   		Link Type:<br /><br />
	   		<input id="internal" name="type" type="radio" selected="true">Internal</input><br /><br />
			<input id="external" name="type" type="radio">External</input><br /><br />
	   		<input type="button" value="Next" onclick="nextStep();" />
	   </div>
	   <div id="step2a" style="display:none;">
		   <div style="float:right;">
				Change Site: <g:select value="${site?.id}" onchange="switchSite(this)" optionKey="id" name="site.id" from="${Site.list()}" />
		   </div>	
		   <g:if test="${flash.message}">
				 <div class="message">${flash.message}</div>
		   </g:if>	
			<g:if test="site">
			   <div class="linkText">Link Text: <input id="linkText1" type="text" id="text"></input></div>
			   <div class="tree">
					<br />       		
					Select Page: <br />	
						<g:tree 
							id="linkTree" 
							root="${site?.homePage}" 
							dynamicLoadUri="[controller:'site',action:'loadSubPages']"
							onNodeClick="handleLinkSelect"
							 />    	
			   </div>		
			</g:if>
		</div>
		<div id="step2b" style="display:none;">			
			Link Text: <input id="linkText2" type="text" id="text"></input><br /><br />
			Link URL: <input id="linkURL" type="text" id="text"></input><br /><br />
			<input type="button" id="saveLinkButton" onclick="saveExternalLink()" value="Save" />
		</div>
	</div>
</body>
</html>
