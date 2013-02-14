
This feature covers the initial setup of a Gradle build, either from an existing source tree or from scratch. The result of using
this feature is a Gradle build that is usable in some form. In some cases, the build will be fully functional, and in other
cases, the build will require some additional manual work to be completed before it is ready to be used.

When used with an existing build, this feature is intended to integrate closely with the build comparison feature, so that the
initial build is created using the build initialization feature, and then the build comparison feature can be used to drive
manual changes to the Gradle build, and to inform when the Gradle build is ready to replace the existing build.

# Use cases

1. I have a multi-module Maven build that I want to migrate to Gradle.
2. I have an Ant based build that I want to migrate to Gradle.
3. I have an Ant + Ivy based build that I want to migrate to Gradle.
4. I have a Make based build that I want to migrate to Gradle.
5. I have an Eclipse based build that I want to migrate to Gradle.
6. I have an IDEA based build that I want to migrate to Gradle.
7. I want to create a Java library project from scratch.
8. I want to create a {Groovy, Scala, C++, Javascript, Android} library from scratch.
9. I want to create a {Java, native, Android, web} application from scratch.
10. I want to create a project that follows my organisation's conventions from scratch.
11. I want to create an organisation specific project type from scratch.

# User visible changes

* A new plugin, `init-build`
* The plugin adds task `initGradleBuild` that depends the appropriate tasks based on the contents of the project directory.

# Migrating from Maven to Gradle

When the `pom.xml` packaging is `pom`:
* Generate a multi-project build, with a separate Gradle project for each Maven module referenced in the `pom.xml`, and a root project for the parent module.
* Generate a `build.gradle` for each Maven module based on the contents of the corresponding `pom.xml`.

For all other packagings:
* Generate a single-project build.

For all builds:
* Generate a `settings.gradle`
* Generate the wrapper files.
* Preconfigure the build comparison plugin, as appropriate.
* Inform the user about the build comparison plugin.

## User interaction

From the command-line:

1. User downloads and installs a Gradle distribution.
2. User runs `gradle initGradleBuild` from the root directory of the Maven build.
3. User runs the appropriate build comparison task from the root directory.
4. User modifies Gradle build, if required, directed by the build comparison report.

From the IDE:

1. User runs `initialize Gradle build` action from UI and selects the Maven build to initialize from.
2. User runs the appropriate build comparison task from the root directory.
3. User modifies Gradle build, if required, directed by the build comparison report.

## Sad day cases

* Maven project does not build
* bad `pom.xml`
* missing `pom.xml`

## Integration test coverage

* convert a multi-module maven project and run Gradle build with generated Gradle scripts
* convert a single-module maven project and run with Gradle.
* include a sad day case(s)

## Implementation approach

* Add some basic unit and integration test coverage.
* Use the maven libraries to determine the effective pom in process, rather than forking 'mvn'.
* Reuse the import and maven->gradle mapping that the importer uses.
 We cannot have the converter using one mapping and the importer using a different mapping.
 Plus this means the converter can make use of any type of import (see below).

## Other potential stories

* Better handle the case where there's already some Gradle build scripts (i.e. don't overwrite an existing Gradle build).
* Add support for auto-applying a plugin, so that I can run `gradle initGradleBuild' in a directory that contains a `pom.xml` and no Gradle stuff.

# Migrating from Ant to Gradle

* Infer the project model from the contents of the source tree (see below).
* Generate a `build.gradle` that applies the appropriate plugin for the project type. It does _not_ import the `build.xml`.
* Generate a `settings.gradle`.
* Generate the wrapper files.
* Preconfigure the build comparison plugin, as appropriate.
* Inform the user about the build comparison plugin.

## User interaction

From the command-line:

1. User downloads and installs a Gradle distribution.
2. User runs `gradle initGradleBuild` from the root directory of the Ant build.
3. User runs the appropriate build comparison task from the root directory.
4. User modifies Gradle build, directed by the build comparison report.

From the IDE:

1. User runs `initialize Gradle build` action from UI and selects the Ant build to initialize from.
2. User runs the appropriate build comparison task from the root directory.
3. User modifies Gradle build, directed by the build comparison report.

# Migrating from Ant+Ivy to Gradle

* Infer the project model from the contents of the source tree.
* Generate a `build.gradle` that applies the appropriate plugin for the project type.
* Convert the `ivysettings.xml` and `ivy.xml` to build.gradle DSL.
* Generate a `settings.gradle`.
* Generate the wrapper files.
* Preconfigure the build comparison plugin, as appropriate.
* Inform the user about the build comparison plugin.

# Migrating from Make to Gradle

As for the Ant to Gradle case.

# Migrating from Eclipse to Gradle

* Infer the project layout, type and dependencies from the Eclipse project files.
* Generate a multi-project build, with a Gradle project per Eclipse project.
* Generate a `settings.gradle`.
* Generate the wrapper files.
* Preconfigure the build comparison plugin, as appropriate.
* Inform the user about the build comparison plugin.

# Migrating from IDEA to Gradle

As for the Eclipse to Gradle case.

# Create a Java library project from scratch

* Generate a `build.gradle` that applies the Java plugin, adds `mavenCentral()` and the dependencies to allow testing with JUnit.
* Generate a `settings.gradle`.
* Generate the wrapper files.
* Create the appropriate source directories.
* Possibly add a class and a unit test.

## User interaction

From the command-line:

1. User downloads and installs a Gradle distribution.
2. User runs `gradle initGradleBuild` from an empty directory.
3. User modifies generated build scripts and source, as appropriate.

From the IDE:

1. User runs `initialize Gradle build` action from the UI.
2. User modifies generated build scripts and source, as appropriate.

# Create a library project from scratch

* The user specifies the type of library project to create
* As for Java library project creation

## User interaction

1. User downloads and installs a Gradle distribution.
2. User runs `gradle initGradleBuild` from an empty directory.
3. The user is prompted for the type of project they would like to create. Alternatively,
   the user can specify the project type as a command-line option.
4. User modifies generated build scripts and source, as appropriate.

# Create an application project from scratch

As for creating a library project.

# Create a project with custom convention from scratch

TBD

# Create an organisation specific project from scratch

TBD

# Inferring the project model

This can start off pretty basic: if there is a source file with extension `.java`, then the Java plugin is required, if there is a
source file with extension `.groovy`, then the Groovy plugin is required, and so on.

The inference can evolve over time:
* if the source file path contains an element called `test`, then assume it is part of the test source set.
* parse the source to extract package declarations, and infer the source directory roots from this.
* parse the source import statements and infer the test frameworks involved.
* parse the source import statements and infer the project dependencies.
* infer that the project is a web app from the presence of a `web.xml`.
* And so on.

The result of the inference can potentially be presented to the user to confirm (or they can just edit the generated build file). When nothing
useful can be inferred, the user can select from a list or assemble the model interactively.

# Open issues

None yet.
