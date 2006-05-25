<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN"
"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
	<head>
		<title><g:layoutTitle default="${site?.name}" /></title>
		<link rel="stylesheet" href="\${createLinkTo(dir:"css/${site.domain}",file:'style.css')}" type="text/css" media="screen,projection" />
		<link rel="stylesheet" href="\${createLinkTo(dir:"css/${site.domain}",file:'site.css')}" type="text/css" media="screen,projection" />		
		<g:layoutHead />
	</head>
	<body onload="\${pageProperty(name:'body.onload')}">
		<div id="container">
		<div id="sitename"></div>
		<g:if test="\${session.user}">
			<div id="nav">
				<ul>
					<%firstLevel.eachWithIndex { c,i ->%>
						
						<li \${ifPageProperty(name:'meta.level.2',equals:'${c.id}','id="current"')}>
							<%if(c.type == Page.LINK && !(c.content ==~ /\d+/)) {%>
								<a href="${c.content}">${c.title}</a>
							<%}
							else {%>							
								<g:link controller="page" action="show" id="${c.id}">${c.title}</g:link>
							<%}%>
						</li>
					<%}%>
				</ul>
			</div>	
		  <div id="subnav">
			<g:ifPageProperty name="['meta.level.2']">
					<%firstLevel.eachWithIndex { c,i ->%>
						<g:ifPageProperty name="meta.level.2" equals="${c.id}">
							<%
							def groups = []
							def children = getChildPages(c)
							
							if(children) {
							    def splitIndex = 4
								def splitCount = Math.floor(children.size()/splitIndex)
								if(children.size() % splitIndex)splitCount++
								children.eachWithIndex { cp,j ->
									splitCount.times { s ->
										def upper = (s+1)*splitIndex
										def lower = upper-splitIndex
										if((lower..upper).contains(j)) {
											if(!groups[s]) groups[s] = []
											
											groups[s] << cp										
										}									
									}
								}
							}
							
							groups.eachWithIndex { g, j ->
							%>
							<ul>
								<%g.eachWithIndex { cp,k ->%>
									<li \${ifPageProperty(name:'meta.level.3',equals:'${c.id}','id="current"')}>
											<%if(cp.type == Page.LINK && !(cp.content ==~ /\d+/)) {%>
												<a href="${cp.content}">${cp.title}</a>
											<%}
											else {%>							
												<g:link controller="page" action="show" id="${cp.id}">${cp.title}</g:link>
											<%}%>											
									</li>	
								<%}%>
							</ul>
							<%}%>
						</g:ifPageProperty>
					<%}%>
			</g:ifPageProperty>		  
		  </div>
		  </g:if>
			<div id="wrapper">
					<div id="content">
						<h1><g:layoutTitle /></h1>
						<div id="body">
							<g:layoutBody />
						</div>
						<g:if test="\${session.user}">
							<p class="hide"><a href="#top">Back to top</a></p>
						</g:if>
						<div id="footer"><b>Powered by Grails</b> <br />
							<a href="http://grails.org">grails.org</a> | <a href="#top">Top</a>
						</div>						
					</div>
						<g:if test="\${session.user}">
						<g:ifPageProperty name="['meta.level.2','meta.level.3']">
							<div id="leftside">
								<%firstLevel.eachWithIndex { c,i ->%>
										<%getChildPages(c).eachWithIndex { cp,j ->%>
											<g:ifPageProperty name="meta.level.3" equals="${cp.id}">
												<h1>${cp.title}</h1>
												<ul id="subsubnav">
													<%getChildPages(cp).eachWithIndex { cp2,k ->%>								
														<li><g:link class="nav" controller="page" action="show" id="${cp2.id}">${cp2.title}</g:link></li>
													<%}%>
												</ul>
											</g:ifPageProperty>									
										<%}%>
								<%}%>
							</div>
						</g:ifPageProperty>
						</g:if>

					
			</div>
	</body>
</html>
