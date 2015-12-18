## Grails GSP Gradle Plugin 

This gradle plugin is required for all grails 3 applications and plugins that require GSP processing.  If your project includes GSPs you should add the following to your `build.gradle`.

``` gradle
apply plugin: "org.grails.grails-gsp"
```

It is typical of standard grails 3 application to use this in conjunction with `grails-web` as in the following example: 

``` gradle
apply plugin: "org.grails.grails-web"
apply plugin: "org.grails.grails-gsp"
```

Dependencies
-----
To see what additional plugins will be included with this plugin you can view this project's [build.gradle](https://github.com/grails/grails-core/blob/master/grails-gsp/build.gradle)
