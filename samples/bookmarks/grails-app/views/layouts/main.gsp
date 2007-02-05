<%@ page import="org.grails.bookmarks.*" %> 
<html>
	<head>
		<title><g:layoutTitle default="Grails" /></title>
		<link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
		<g:layoutHead />
		<g:javascript library="scriptaculous" />	
		<g:javascript library="application" />
		<style type="text/css">
			.searchbar { 
				margin-top:10px;
				background-color: lightgrey;
				border:1px solid darkgrey;
				width:97%;
				height:30px;
				padding:5px;
			}
			.total { 
				padding-top:5px;
				float:left;
				color:white;
			}
			.search {
				float:right;
				color:white;
			}
			.userDetails {
				position:absolute;
				right:10px;
				top: 180px;
				border:1px solid darkgrey;
				background-color:lightgrey;
				padding:10px;
				width:150px;
			}
			.pageContent {
				width:80%;
			}
			.logo h1 {
				float:right;
				margin:30px;
			}
			.spinner {
				position: absolute;
				right:0px;
				padding:5px;
			}
		</style>		
	</head>
	<body onload="<g:pageProperty name='body.onload'/>">
		<div id="spinner" class="spinner" style="display:none;">
			<img src="${createLinkTo(dir:'images',file:'spinner.gif')}" alt="Spinner" />
		</div>
        <div class="logo">
        <g:if test="${session.user}">
			<h1 id="header">Bookmarks by ${session.user.firstName} ${session.user.lastName}</h1>
		</g:if>
        <img src="${createLinkTo(dir:'images',file:'grails_logo.jpg')}" alt="Grails" />
        </div>	
		<g:if test="${session.user}">
			<div class="searchbar">
				<div id="total" class="total">You have (${Bookmark.countByUser(session.user)}) bookmarks</div>
				<div class="search">
					<g:form id="searchForm" url="[action:'search', controller:'bookmark']">
						<input type="text" name="q" /> <input type="submit" value="search" />
					</g:form>
				</div>
			</div>
		</g:if>
		<div id="pageContent" class="pageContent">
			<g:layoutBody />
		</div>
		<g:if test="${session.user}">
			<div class="userDetails">
			<h3>Your Profile</h3>
			<p><strong>Login</strong>: ${session.user.login}</p>
			<p><strong>First Name</strong>: ${session.user.firstName}</p>
			<p><strong>Last Name</strong>: ${session.user.lastName}</p>
			<p><strong>Email</strong>: ${session.user.email}</p>
			</div>
			<div class="tags">
				<g:each in="${tags}">
				
				</g:each>
			</div>
		</g:if>		
	</body>	
</html>