## Grails Web

This subproject is required for all Grails 3 applications and plugins that require MVC, sitemesh, url mapping and GSP processing.  If your project requires these functions you should add the following to your `build.gradle`  which is provided by the [Grails Gradle Plugin](https://github.com/grails/grails-core/tree/master/grails-gradle-plugin).

``` gradle
apply plugin: "org.grails.grails-web"
```

Dependencies
-----
To see what additional subprojects will be included with this, you can view this project's [build.gradle](https://github.com/grails/grails-core/blob/master/grails-web/build.gradle)
