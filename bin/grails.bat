@echo off

rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

if "%JAVA_HOME%"=="" goto javaHomeNotSet
if "%GRAILS_HOME%"=="" goto grailsHomeNotSet
goto getArguments

:javaHomeNotSet
echo Error: JAVA_HOME is not defined
echo Please set the JAVA_HOME environment variable and start Grails again
goto errorExit

:grailsHomeNotSet
if "%OS%"=="Windows_NT" set GRAILS_HOME=%~dp0%..
if not "%GRAILS_HOME%" == "" goto okGrailsHome
echo Error: GRAILS_HOME is not defined
echo Please set the GRAILS_HOME environment variable and start Grails again
goto errorExit

:okGrailsHome

:getArguments
set GRAILS_ARGUMENTS=%1
if ""%1""=="""" goto getClasspath
shift
:loopArguments
if ""%1""=="""" goto getClasspath
set GRAILS_ARGUMENTS=%GRAILS_ARGUMENTS% %1
shift
goto loopArguments

:getClasspath
set GRAILS_ANT_CLASSPATH="%GRAILS_HOME%\lib\bsf.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\groovy-all-1.0-RC-01-SNAPSHOT.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\org.mortbay.jetty.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\commons-logging-1.1.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\commons-el-1.0.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\servletapi-2.4.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\jsp-api-2.0.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\log4j-1.2.8.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\jasper-compiler.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\jasper-runtime.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\xml-apis.jar
set GRAILS_ANT_CLASSPATH=%GRAILS_ANT_CLASSPATH%;%GRAILS_HOME%\lib\xercesImpl-2.5.0.jar"
goto startGrails

:startGrails
set ANT_HOME=%GRAILS_HOME%\ant
call "%ANT_HOME%\bin\ant.bat" -lib %GRAILS_ANT_CLASSPATH% -f "%GRAILS_HOME%\src\grails\build.xml" -Dgrails.home="%GRAILS_HOME%" -Dbasedir="%CD%"  %GRAILS_ARGUMENTS%

:errorExit

rem End local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" endlocal
