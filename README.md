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

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:check
    

This will check your project and all modules to seek for potential violations.

The report will be generated at: `MAVEN_PROJECT/target/site/qschecker.html`

If you need to ignore some files from being checked
--------------------------------------------------

`Note:` Some files are already excluded by default: 
 - hidden files
 - files inside the `target` folder
 - README.html files
 - files from the following libraries: jquery, cordova, angular, qunit, backbone, lodash, modernizr, yepnope.

If you need for any reason remove some files from the the Checker, you can add the *excludes expression* to 'qstools.excludes' parameter:

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:check 
       -Dqstools.excludes="**/somefile.txt, *.bkp"
    

You can *also use a file that lists the files to be ignored* using the `qstools.excudes.file` property: 

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:check 
       -Dqstools.excludes.file=<relative or absolute path to a file>
    
`Note:` The file format uses individual patterns on each line

File content example:
        
        src/main/resources/import.sql
        src/main/webapp/js/somejs.js
        **/*.css
        **/somelibrary*.js
        src/main/resources/**/*.xml


If you need to update quickstarts BOMs
--------------------------------------

This goal will check the [Stacks file](https://github.com/jboss-jdf/jdf-stack/blob/1.0.0.Final/stacks.yaml)  and look for the recommended Version for each BOM.

If the recommended BOM is newer it will replace the version, otherwise it will only warn you that your quickstarts is using a newer version than the recommended one and won't  update the BOM.

`NOTE:` It's high recommended that you have your changes saved before running this plugin because it modifies your pom files.

To run the plugin:

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:updateBoms  
    


Using a custom stacks.yaml definition
-------------------------------------

If you need to use a custom Stacks.yaml definition you can overwrite the stacks url adding the property `qstools.stacks.url` to the command line:

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:(check|updateBoms) 
       -Dqstools.stacks.url=file:/somewhere/on/your/disk/stacks.yaml 
    

or

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:(check|updateBoms) 
       -Dqstools.stacks.url=http://www.somewhere.net/somepath/stacks.yaml 
    


Plugin Documentation
---------------------

The plugin documentation *generated from mvn site* [is available here](target/site/plugin-info.html) 


Troubleshooting
---------------

You can turn on debugging messages:   

    mvn org.jboss.maven.plugins:maven-qstools-plugin:1.0.0.CR5:check -X
    


