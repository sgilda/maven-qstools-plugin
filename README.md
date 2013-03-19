qstools: Quickstarts tools Maven plugin
=======================================
Author: Rafael Benevides
Summary: Maven plugin that helps JDF quickstarts maintenance


What is it?
-----------

This a Maven Plugin that helps JDF qucikstarts maintenance


System requirements
-------------------

All you need is Maven 3.0+ and a working internet connection.


Checking the quickstarts
------------------------

Select a Maven project an run:

    mvn org.jboss.maven.plugins:qstools:check

This will check your project and all modules to seek for potential violations.

The report will be generated at: `MAVEN_PROJECT/target/site/qschecker.html`

Updating the BOMs
-----------------

This goal will check the [Stacks file](https://github.com/jboss-jdf/jdf-stack/blob/1.0.0.Final/stacks.yaml)  and look for the recommended Version for each BOM.

If the recommended BOM is newer it will replace the version, otherwise it will only warn you that your quickstarts is using a newer version than the recommended one and won't  update the BOM.

`NOTE: It's high recommended that you have your changes saved before running this plugin because it modifies your pom files.`

To run the plugin:

    mvn org.jboss.maven.plugins:qstools:updateBoms  

Using a custom stacks.yaml definition
-------------------------------------

If you need to use a custom Stacks.yaml definition you can overwrite the stacks url adding the property `qstools.stacks.url` to the command line:

    mvn org.jboss.maven.plugins:qstools:(check|updateBoms) -Dqstools.stacks.url=file:/somewhere/on/your/disk/stacks.yaml     

or

    mvn org.jboss.maven.plugins:qstools:(check|updateBoms) -Dqstools.stacks.url=http://www.somewhere.net/somepath/stacks.yaml 


Troubleshooting
---------------

You can turn on debugging messages:   

    mvn org.jboss.maven.plugins:qstools:check -X
