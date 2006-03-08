<%@ taglib prefix="g" uri="http://grails.codehaus.org/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<html>
    <head>
        <title>Welcome to Grails</title>
        <link rel="stylesheet" href="css/main.css"></link>
    </head>
    <body>
        <p><img src="images/grails_logo.jpg" alt="Grails Logo" /></p>
        <h1 style="margin-left:20px;">Welcome to Grails</h1>
        <p style="margin-left:20px;width:80%">Congratulations, you have successfully started your first Grails application! At the moment
        this is the default page, feel free to modify it to either redirect to a controller or display whatever
        content you may choose. Below is a list of controllers that are currently deployed in this application,
        click on each to execute its default action:</p>
        <div class="dialog" style="margin-left:20px;width:60%;">
            <ul>
              <c:forEach var="c" items="${applicationScope.grailsApplication.controllers}">
                    <li class="controller"><a href="<c:out value='${c.propertyName}' />/"><c:out value="${c.fullName}" /></a> </li>
              </c:forEach>
            </ul>
        </div>

    </body>
</html>