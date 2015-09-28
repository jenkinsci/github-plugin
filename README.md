Jenkins Github Plugin
=====================
[![covarage](https://img.shields.io/sonar/http/sonar.lanwen.ru/com.coravy.hudson.plugins.github:github/coverage.svg?style=flat)](http://sonar.lanwen.ru/dashboard/index?id=com.coravy.hudson.plugins.github:github)

Read more: [http://wiki.jenkins-ci.org/display/JENKINS/Github+Plugin](http://wiki.jenkins-ci.org/display/JENKINS/Github+Plugin)

Development
===========

Start the local Jenkins instance:

    mvn hpi:run


Jenkins Plugin Maven goals
--------------------------

	hpi:create  Creates a skeleton of a new plugin.
	
	hpi:hpi Builds the .hpi file

	hpi:hpl Generates the .hpl file

	hpi:run Runs Jenkins with the current plugin project

	hpi:upload Posts the hpi file to java.net. Used during the release.
	
	
How to install
--------------

Run 

	mvn hpi:hpi
	
to create the plugin .hpi file.


To install:

1. copy the resulting ./target/rdoc.hpi file to the $JENKINS_HOME/plugins directory. Don't forget to restart Jenkins afterwards.
	
2. or use the plugin management console (http://example.com:8080/pluginManager/advanced) to upload the hpi file. You have to restart Jenkins in order to find the pluing in the installed plugins list.


Plugin releases
---------------

	mvn release:prepare release:perform -Dusername=juretta -Dpassword=******


License
-------

	(The MIT License)

	Copyright (c) 2009 Stefan Saasen

	Permission is hereby granted, free of charge, to any person obtaining
	a copy of this software and associated documentation files (the
	'Software'), to deal in the Software without restriction, including
	without limitation the rights to use, copy, modify, merge, publish,
	distribute, sublicense, and/or sell copies of the Software, and to
	permit persons to whom the Software is furnished to do so, subject to
	the following conditions:

	The above copyright notice and this permission notice shall be
	included in all copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
	EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
	MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
	IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
	CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
	TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
	SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.


This plugin uses part of Guava's code in class named 
`org.jenkinsci.plugins.github.util.FluentIterableWrapper` licensed under Apache 2.0 license
