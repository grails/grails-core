<?xml version="1.0" encoding="us-ascii" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:import href="webtestCC.xsl"/>
	<xsl:output method="html"/>

	<xsl:template match="/">
		<html>
			<head>
				<title>WebTest's CruiseControl Build Result</title>
			</head>
			<body>
				<xsl:apply-imports/>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
