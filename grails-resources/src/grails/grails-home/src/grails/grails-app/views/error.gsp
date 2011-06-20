<html>
  <head>
		
	  <title>Grails Runtime Exception</title>
  	  <meta name="layout" content="main"></meta>
	  <style type="text/css">
			h1, h2 {
				margin: 10px;
			}
	  		.exceptionMessage {
				margin: 10px;
	  			border: 1px solid black;
	  			padding: 5px;
	  			background-color:#E9E9E9;
	  		}
	  		.stack {
				margin:10px;	
	  			margin-left:25px;
	  			margin-right:25px;	
	  			border: 1px solid black;
	  			padding: 5px;
	  			overflow:auto;
	  			height: 150px;
	  		}
	  		.snippet {
	  			background-color:white;
	  			border:1px solid black;
				margin:10px;	
	  			margin-left:25px;
	  			margin-right:25px;

	  			font-family:courier;
	  		}
			.snippet .lineNumber {
				background-color:black;
				font-weight:white;
				color:white;
				padding-left:3px;
				padding-right:3px;				
			}	
			.snippet .errorLine .lineNumber {
				background-color:#cc0000;
				font-weight:bold;
				color:white;
			} 
	  </style>
  </head>

  <body>
	<g:renderException exception="${exception}" />
  </body>
</html>