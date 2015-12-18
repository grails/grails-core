## Grails Web Gradle Plugin 

This gradle plugin is required for all grails 3 applications and plugins that require MVC, sitemesh, url mapping and GSP processing.  If your project requires these functions you should add the following to your `build.gradle`.

``` gradle
apply plugin: "org.grails.grails-web"
```

Dependencies
-----
To see what additional plugins will be included with this plugin you can view this project's [build.gradle](https://github.com/grails/grails-core/blob/master/grails-web/build.gradle)
