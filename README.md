# Quickstarts tools maven plugin

Author: Rafael Benevides
Summary: Maven plugin that helps JBoss Developer materials maintenance

## What is it?


This a Maven Plugin that helps JBoss Developer materials maintenance.

You can use it to verify if your project/quickstart follow the JBoss Developer Guidelines. It will run all JBoss Developer Guideline checkers and generate a report that provides information about any violations that your project/quickstarts has.

For Maven BOMs,it can be used to verify if your Maven BOM has all dependencies being resolved.

You can also use QSTools to synchronize an Maven Archetype with and existing project.


## System requirements


All you need is [Apache Maven 3.0.X](http://maven.apache.org/) and a working internet connection.


## Checking the quickstarts made easy


Select a Maven project and run:

    mvn -U org.jboss.maven.plugins:qstools:check
    

This will check your project and all modules to seek for potential violations.

The report will be generated at: `MAVEN_PROJECT/target/site/qschecker.html`


### Configuring QSTools


QSTools configuration is made by editing the online file https://github.com/jboss-developer/maven-qstools-plugin/blob/master/config/qstools_config.yaml

You can use a local config file by overwriting qstools property:

    mvn -U org.jboss.maven.plugins:qstools:check
         -Dqstools.configFileURL=file:///Users/rafaelbenevides/path/qstools_config.yaml
         


### Using a custom stacks.yaml definition


[Stacks](https://github.com/jboss-jdf/jdf-stack) is used to check the BOM versions used on you project.

If you need to use a custom Stacks.yaml definition you can overwrite the stacks url adding the property `qstools.stacks.url` to the command line:

    mvn -U org.jboss.maven.plugins:qstools:check
       -Dqstools.stacks.url=file:/somewhere/on/your/disk/stacks.yaml
       
or

    mvn -U org.jboss.maven.plugins:qstools:(check)
       -Dqstools.stacks.url=http://www.somewhere.net/somepath/stacks.yaml
       
 
## Automatically fixing the quickstarts


QSTools can fix most of the violations.

You can run:


    mvn -U org.jboss.maven.plugins:qstools:fix
    

`NOTE:` It's high recommended that you have your changes saved before running this plugin because it modifies your pom files.


## Setup JBoss Maven approved repositories


QSTools can add/remove the <repositories/> section on pom.xml files. It uses [Approved JBoss Maven Repositories](https://github.com/jboss-developer/maven-qstools-plugin/blob/master/config/qstools_config.yaml#L24) list.

You can run:


    mvn -U org.jboss.maven.plugins:qstools:repositories
    

`NOTE:` It's high recommended that you have your changes saved before running this plugin because it modifies your pom files.

## If you need to update quickstarts BOMs


This goal will check the [Expected BOM Version](https://github.com/jboss-developer/maven-qstools-plugin/blob/master/config/qstools_config.yaml#L24)  and update the BOM versions to the expected version.

It will also replace any community BOMs by the Product BOMs if it is specified under `project-boms-migration` sections of [QSTools configuration file](https://github.com/jboss-developer/maven-qstools-plugin/blob/master/config/qstools_config.yaml)

`NOTE:` It's high recommended that you have your changes saved before running this plugin because it modifies your pom files.

To run the plugin:

    mvn -U org.jboss.maven.plugins:qstools:updateBoms
    

## Checking the BOM dependencies


This goal will check the given BOM project if all declared dependencies under </dependencyManagement> section is resolvable.

To run the plugin: 

    mvn -U org.jboss.maven.plugins:qstools:bom-check
    

If you need to ignore certain known dependencies from being checked, you can use the `qstools.bom-check.ignoredDependencies` property.

Example:

    mvn -U org.jboss.maven.plugins:qstools:bom-check 
       -Dqstools.bom-check.ignoredDependencies=<groupId 1>:<artifactId 1>:jar:<version 1>,<groupId 2>:<artifactId 2>:jar:<version 2>
    

By default, the project build will fail if some managed dependency is not resolvable. You can overwrite this behavior by using `qstools.bom-check.failbuild` property.
This will run on `REPORT ONLY` mode.

Example:

    mvn -U org.jboss.maven.plugins:qstools:bom-check 
       -Dqstools.bom-check.failbuild=false
    

To specify a custom settings.xml file you must use `org.apache.maven.user-settings` property. This is because [Shrinkwrap Resolver](https://github.com/shrinkwrap/resolver#resolution-of-artifacts-specified-by-maven-coordinates) doesn't consume settings.xml you specified on command line (-s settings.xml) or in the IDE. It reads settings.xml files at their standard locations, which are `~/.m2/settings.xml` and `$M2_HOME/conf/settings.xml` unless overridden in the API or via System property.

Example:

    mvn -U org.jboss.maven.plugins:qstools:bom-check 
       -Dorg.apache.maven.user-settings=<your custom settings.xml>
    

## Synchronizing Archetypes with Quickstarts


QSTools can be used on archetype to synchronize the archetype-resources with a given project

This is an example of configuration:

        <plugins>
            <plugin>
                <groupId>org.jboss.maven.plugins</groupId>
                <artifactId>qstools</artifactId>
                <version>1.5.2.Final</version>
                <configuration>
                    <projectGitRepo>git://github.com/jboss-developer/jboss-eap-quickstarts.git</projectGitRepo>
                    <projectPath>kitchensink-ear</projectPath>
                    <rootPackage>org.jboss.as.quickstarts.kitchensink_ear</rootPackage>
                    <multiModuleProject>true</multiModuleProject>
                    <applyPatch>A-patch-file.patch</applyPatch>
                    <branch>[a branch name or a commit hash]</branch>
                    <-- Replace the following strings by {archetypeId} or __artifactId__ in file name
                    <archetypeExpressionReplaceValues>
                        <archetypeExpressionReplaceValue>jboss-as-kitchensink-ear</archetypeExpressionReplaceValue>
                        <archetypeExpressionReplaceValue>kitchensink-ear-quickstart</archetypeExpressionReplaceValue>
                        <archetypeExpressionReplaceValue>kitchensink-quickstart</archetypeExpressionReplaceValue>
                        <archetypeExpressionReplaceValue>KitchensinkEarQuickstart</archetypeExpressionReplaceValue>
                        <archetypeExpressionReplaceValue>JBoss EAP Quickstart: kitchensink-ear</archetypeExpressionReplaceValue>
                    </archetypeExpressionReplaceValues>
                    <!-- Expressions that will be ignored during the replacement -->
                    <ignoredArchetypeExpressionReplaceValues>
                              <ignoredArchetypeExpressionReplaceValue>https://github.com/wildfly/quickstart/tree/master/</ignoredArchetypeExpressionReplaceValue>
                    </ignoredArchetypeExpressionReplaceValues>
                    <!-- Replace the html5mobi string by {tableSuffix} -->
                    <replaceValueWithExpression>
                        <html5mobi>tableSuffix</html5mobi>
                    </replaceValueWithExpression>
                </configuration>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>archetypeSync</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>

The `archetypeExpressionReplaceValues` argument is used to replace the given expression to the `${artifactId}` expression or `__artifactId__` in file name.

In some situations will may want to ignore a line to be replaced. You can specify the line content on the `ignoredArchetypeExpressionReplaceValues` argument.

## Plugin Documentation

The plugin documentation *generated from mvn site* [is available here](target/site/plugin-info.html) 


## Troubleshooting

You can turn on debugging messages:   

    mvn -U org.jboss.maven.plugins:qstools:check -X
    
