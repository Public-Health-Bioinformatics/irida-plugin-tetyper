[![Build Status](https://travis-ci.org/dfornika/irida-plugin-tetyper.svg?branch=master)](https://travis-ci.org/dfornika/irida-plugin-tetyper)
[![codecov](https://codecov.io/gh/dfornika/irida-plugin-tetyper/branch/master/graph/badge.svg)](https://codecov.io/gh/dfornika/irida-plugin-tetyper)
[![Current Release Version](https://img.shields.io/github/release/dfornika/irida-plugin-tetyper.svg)](https://github.com/dfornika/irida-plugin-tetyper/releases)

# IRIDA TETyper Pipeline Plugin

![galaxy-workflow-diagram.png][]

This project contains a pipeline implemented as a plugin for the [IRIDA][] bioinformatics analysis system. 
This can be used to determine the sequence type of specific transposable elements that may be present in bacterial isolates.

# Table of Contents

   * [IRIDA TETyper Pipeline Plugin](#irida-tetyper-pipeline-plugin)
   * [Installation](#installation)
      * [Installing Galaxy Dependencies](#installing-galaxy-dependencies)
      * [Installing to IRIDA](#installing-to-irida)
   * [Usage](#usage)
      * [Analysis Results](#analysis-results)
      * [Metadata Table](#metadata-table)
   * [Building](#building)
      * [Installing IRIDA to local Maven repository](#installing-irida-to-local-maven-repository)
      * [Building the plugin](#building-the-plugin)
   * [Dependencies](#dependencies)

# Installation

## Installing Galaxy Dependencies

In order to use this pipeline, you will also have to install the following Galaxy tools and data 
managers within your Galaxy instance. These can be found at:

| Name                               | Version         | Owner                          | Metadata Revision | Galaxy Toolshed Link                                                                                                                              |
|------------------------------------|-----------------|------------------------------- |-------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| tetyper                            | `1.1`           | `iuc`                          | 0 (2019-11-28)    | [tetyper-0:8bc80c2a15b3](https://toolshed.g2.bx.psu.edu/view/iuc/tetyper/8bc80c2a15b3)                                                            |

## Installing to IRIDA

Please download the provided `irida-plugin-tetyper-[version].jar` from the [releases][] page and copy to your 
`/etc/irida/plugins` directory.  Now you may start IRIDA and you should see the pipeline appear in your list of pipelines.

*Note:* This plugin requires you to be running IRIDA version >= `19.01`. Please see the [IRIDA][] documentation for more details.

# Usage

The plugin should now show up in the **Analyses > Pipelines** section of IRIDA.

![plugin-pipeline.png][]
![pipeline-parameters.png][]

## Analysis Results

You should be able to run a pipeline with this plugin and get analysis results. The results include a full `abricate` 
report, and a screened `abricate` report that includes only your genes of interest.

![plugin-results-1.png][]
![plugin-results-2.png][]
![plugin-results-3.png][]

## Metadata Table

This pipeline plugin does not currently write any data to the IRIDA Metadata Table. Future versions of this pipeline
plugin will do so. The initial version of this plugin does not compare SNPs and structural varaints against a typing scheme.
Once that functionality has been added, the transposable element type information will be written to the IRIDA metadata table.

| Field Name                                 | Description                                               |
|--------------------------------------------|-----------------------------------------------------------|
|                                            |                                                           |


![plugin-metadata.png][]

# Building

Building and packaging this code is accomplished using [Apache Maven][maven]. However, you will first need to install [IRIDA][] to your local Maven repository. The version of IRIDA you install will have to correspond to the version found in the `irida.version.compiletime` property in the [pom.xml][] file of this project. Right now, this is IRIDA version `19.01.3`.

## Installing IRIDA to local Maven repository

To install IRIDA to your local Maven repository please do the following:

1. Clone the IRIDA project

```bash
git clone https://github.com/phac-nml/irida.git
cd irida
```

2. Checkout appropriate version of IRIDA

```bash
git checkout 19.01.3
```

3. Install IRIDA to local repository

```bash
mvn clean install -DskipTests
```

## Building the plugin

Once you've installed IRIDA as a dependency, you can proceed to building this plugin. Please run the following commands:

```bash
cd irida-plugin-tetyper

mvn clean package
```

Once complete, you should end up with a file `target/irida-plugin-tetyper-0.1.0-SNAPSHOT.jar` which can be installed as a plugin to IRIDA.

# Dependencies

The following dependencies are required in order to make use of this plugin.

* [IRIDA][] >= 0.23.0
* [Java][] >= 1.8 and [Maven][maven] (for building)

[maven]: https://maven.apache.org/
[IRIDA]: http://irida.ca/
[Galaxy]: https://galaxyproject.org/
[Java]: https://www.java.com/
[irida-pipeline]: https://irida.corefacility.ca/documentation/developer/tools/pipelines/
[irida-pipeline-galaxy]: https://irida.corefacility.ca/documentation/developer/tools/pipelines/#galaxy-workflow-development
[irida-wf-ga2xml]: https://github.com/phac-nml/irida-wf-ga2xml
[pom.xml]: pom.xml
[workflows-dir]: src/main/resources/workflows
[workflow-structure]: src/main/resources/workflows/0.1.0/irida_workflow_structure.ga
[example-plugin-java]: src/main/java/ca/corefacility/bioinformatics/irida/plugins/ExamplePlugin.java
[irida-plugin-java]: https://github.com/phac-nml/irida/tree/development/src/main/java/ca/corefacility/bioinformatics/irida/plugins/IridaPlugin.java
[irida-updater]: src/main/java/ca/corefacility/bioinformatics/irida/plugins/ExamplePluginUpdater.java
[irida-setup]: https://irida.corefacility.ca/documentation/administrator/index.html
[properties]: https://en.wikipedia.org/wiki/.properties
[messages]: src/main/resources/workflows/0.1.0/messages_en.properties
[maven-min-pom]: https://maven.apache.org/guides/introduction/introduction-to-the-pom.html#Minimal_POM
[pf4j-start]: https://pf4j.org/doc/getting-started.html
[plugin-results-1.png]: doc/images/plugin-results-1.png
[plugin-results-2.png]: doc/images/plugin-results-2.png
[plugin-results-3.png]: doc/images/plugin-results-3.png
[plugin-pipeline.png]: doc/images/plugin-pipeline.png
[plugin-metadata.png]: doc/images/plugin-metadata.png
[pipeline-parameters.png]: doc/images/pipeline-parameters.png
[example-plugin-save-results.png]: doc/images/example-plugin-save-results.png
[galaxy-workflow-diagram.png]: doc/images/galaxy-workflow-diagram.png
