Jenkins Github Plugin
===================== 

[![Coverage](https://img.shields.io/sonar/http/sonar.lanwen.ru/com.coravy.hudson.plugins.github:github/coverage.svg?style=flat)](http://sonar.lanwen.ru/dashboard/index?id=com.coravy.hudson.plugins.github:github)
[![License](https://img.shields.io/github/license/jenkinsci/github-plugin.svg)](LICENSE)
[![wiki](https://img.shields.io/badge/GitHub%20Plugin-WIKI-blue.svg?style=flat)](http://wiki.jenkins-ci.org/display/JENKINS/Github+Plugin)


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


## License notes

This plugin uses part of Guava's code in class named 
`org.jenkinsci.plugins.github.util.FluentIterableWrapper` licensed under Apache 2.0 license
