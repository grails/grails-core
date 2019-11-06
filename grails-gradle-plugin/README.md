Grails Gradle Plugins
========

Below are the plugins that are provided by the grails-gradle-plugin dependency.

```
buildscript {
	dependencies {
		classpath "org.grails:grails-gradle-plugin:$grailsVersion"
	}
}
```

grails-core
---------
_Todo_: Add the docs

grails-doc
---------
_Todo_: Add the docs

grails-gsp
---------
* Configure GSP Compiling Task

grails-plugin-publish
---------
_Todo_: Add the docs

grails-plugin
---------
* Configure Ast Sources
* Configure Project Name And Version AST Metadata
* Configure Plugin Resources
* Configure Plugin Jar Task
* Configure Sources Jar Task

grails-profile
---------
_Todo_: Add the docs

grails-web
---------
* Adds web specific extensions


Typical Project Type Gradle Plugin Includes
========
Below are typical Gradle plugin applies that certain types of projects should expect.  These should be automatically added of you when using `grails create-app` and `grails create-plugin` commands.  However, if you wish to enhance or change the scope of your plugin or project you may have to change (add or remove) a grails gradle plugin.

Create App
----

<h4>Grails Web Project</h4>
-----
A project created with a typical `grails create-app --profile=web`

```
apply plugin: "org.grails.grails-web"
apply plugin: "org.grails.grails-gsp"
```

<h4>Grails Web API Project</h4>
----
A project created with a typical `grails create-app --profile=web-api`

```
apply plugin: "org.grails.grails-web"
```

<h4>Grails Web Micro Project</h4>

A project created with a typical `grails create-app --profile=web-micro`

There is no plugins used here as this project type creates a stand alone runnable groovy application and no `build.gradle` file.


Create Plugin
---

<h4>Grails Plugin Web Project</h4>
A project created with a typical `grails create-plugin --profile=web-plugin`

```
apply plugin: "org.grails.grails-plugin"
apply plugin: "org.grails.grails-gsp"
```

<h4>Grails Plugin Web API Project</h4>
A project created with a typical `grails create-plugin --profile=web-api`. _Note: No org.grails.grails-plugin include_

```
apply plugin: "org.grails.grails-web"
```


<h4>Grails Plugin Web Plugin Project</h4>
A project created with a typical `grails create-plugin --profile=plugin`.

```
apply plugin: "org.grails.grails-plugin"
```

<h4>Grails Plugin Web Micro Project</h4>

A project created with a typical `grails create-plugin --profile=web-micro`

There is no plugins used here as this project type creates a stand alone runnable groovy application and no `build.gradle`` file.
