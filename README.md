Quickstarts tools Maven plugin
==============================
Author: Rafael Benevides
Summary: Maven plugin that helps JDF quickstarts maintenance

What is it?
-----------

This a Maven Plugin that helps JDF quickstarts maintenance.

You can use it to verify if your project/quickstart follow the JDF Guidelines. It will run all JDF Guideline checkers and generate a report that provides information about any violations that your project/quickstarts has.


System requirements
-------------------

All you need is [Apache Maven](http://maven.apache.org/) and a working internet connection.


Checking the quickstarts made easy
----------------------------------

Select a Maven project and run:

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0-SNAPSHOT:check

This will check your project and all modules to seek for potential violations.

The report will be generated at: `MAVEN_PROJECT/target/site/qschecker.html`


If you need to update quickstarts BOMs
--------------------------------------

This goal will check the [Stacks file](https://github.com/jboss-jdf/jdf-stack/blob/1.0.0.Final/stacks.yaml)  and look for the recommended Version for each BOM.

If the recommended BOM is newer it will replace the version, otherwise it will only warn you that your quickstarts is using a newer version than the recommended one and won't  update the BOM.

`NOTE: It's high recommended that you have your changes saved before running this plugin because it modifies your pom files.`

To run the plugin:

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0-SNAPSHOT:updateBoms  


Using a custom stacks.yaml definition
-------------------------------------

If you need to use a custom Stacks.yaml definition you can overwrite the stacks url adding the property `qstools.stacks.url` to the command line:

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0-SNAPSHOT:(check|updateBoms) -Dqstools.stacks.url=file:/somewhere/on/your/disk/stacks.yaml     

or

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0-SNAPSHOT:(check|updateBoms) -Dqstools.stacks.url=http://www.somewhere.net/somepath/stacks.yaml 


Plugin Documentation
---------------------

The plugin documentation *generated from mvn site* [is available here](target/site/plugin-info.html) 


Troubleshooting
---------------

You can turn on debugging messages:   

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0-SNAPSHOT:check -X


