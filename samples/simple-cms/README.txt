Simple CMS Sample
-----------------

This example demonstrates Grails' capability to develop a typical CRUD application: A CMS.
The CMS is system in that it only provides the basics, but the implementation
uses several advanced techniques including Ajax and a dynamic trees.

To run the example after installing Grails type:

grails init
grails run-app

And then in your browser go to the admin interface:

http://localhost:8080/simple-cms/site/admin

username: admin
password: letmein

The first thing you'll need to do is publish the home page, via the site editor.
Once you've done that you can visit the published material here:

http://localhost:8080/simple-cms/

NOTE: By default it is setup to run with the in-memory DB, but the generated
in-memory DB uses VARCHAR(255) for Strings which will cause errors if the content
is too long. Setup the app using the provide MySQL script if you want to use it 
properly.