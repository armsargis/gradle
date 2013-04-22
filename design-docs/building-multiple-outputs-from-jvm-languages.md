Currently, the JVM language plugins assume that a given set of source files is assembled into a single
output. For example, the `main` Java source is compiled and assembled into a JAR file. However, this is not
always a reasonable assumption. Here are some examples:

* When building for multiple runtimes, such as Groovy 1.8 and Groovy 2.0.
* When building multiple variants composed from various source sets, such as an Android application.
* When packaging the output in various different ways, such as in a JAR and a fat JAR.

By making this assumption, the language plugins force the build author to implement these cases in ways that
are not understood by other plugins that extend the JVM language plugins, such as the code quality and IDE
plugins.

This problem is also evident in non-JVM languages such as C++, where a given source file may be compiled and
linked into more than one binaries.

This spec defines a number of changes that aim to extend the JVM language models to expose the fact that a
given source file may end up in more than one output. It aims to do so in a way that works well with non-JVM
languages.

# Use cases

## Multiple build types for Android applications

An Android application is assembled in to multiple _build types_, such as 'debug' or 'release'.

## Build a library for multiple Scala or Groovy runtimes

A library is compiled and published for multiple Scala or Groovy runtimes, or for multiple JVM runtimes.

# Build different variants of an application

An application is tailored for various purposes, with each purpose represented as a separate variant. For
each variant, some common source files and some variant specific source files are jointly compiled to
produce the application.

For example, when building against the Java 5 APIs do not include the Java 6 or Java 7 specific source files.

# Compose a library from source files compiled in different ways

For example, some source files are compiled using the aspectj compiler and some source files are
compiled usign the javac compiler. The resulting class files are assembled into the library.

# Implement a library using multiple languages

A library is implemented using a mix of Java, Scala and Groovy and these source files are jointly compiled
to produce the library.

## Package a library in multiple ways

A library may be packaged as a classes directory, or a set of directories, or a single jar file, or a
far jar, or an API jar and an implementation jar.

# Implementation plan

The implementation plan involves introducing some new model elements:

* A _jvm binary_. This is some set of artifacts that can run on the JVM. A packaging carries meta-data with
  it, such as its runtime dependencies.
* A _language source set_. This represents a language specific group of source files that form inputs
  to a packaging that can run on the JVM. Usually there is some compilation or other transformation step
  that can take the input source files and transform them to the output packaging. A language source set
  carries meta-data about the source files, such as their compile and runtime dependencies.
* A _composite source set_. This represents a some grouping of other source sets. This can be used to represent
  a set of source files that play some role in the project, such as production or unit test source files.
* A _classpath_ which describes a set of jvm binaries. A classpath is a collection of things that can be
  resolved either to a set of files or a set of dependency declarations.

We will introduce the concept of a _build item_ to represent these things. Source sets and packagings are both
types of build items.

* Build items may or may not be buildable. For example, a binary may be downloaded from a repository or
  built from some input source sets. Or a source set may be used from the file system or generated from some
  other input source set.
* Build items can take other build items as input when they are built, and often require other things to be
  present when they are used. For example, a binary is typically compiled from source and other binaries.
  At runtime, the binary requires some other binaries to be present.
* Build items that are buildable carry meta-data about what their inputs are. Things that have already been built
  carry meta-data about what their inputs were when they were built.

## Story: Introduce language source sets

This story adds the concept of composite and language source sets, and provides this as an alternate view over
the source sets currently added by the languages plugins. At this stage, it will not be possible to define
source sets except via the existing `sourceSets` container.

1. Add a `LanguageSourceSet` interface that extends `Buildable` and `Named`. This has the following properties:
    - `source` of type `SourceDirectorySet`.
2. Add a `FunctionalSourceSet` interface that is a container of `LanguageSourceSet` instances. Initially, this
   will be publically read-only.
3. Add a `JavaSourceSet` interface that extends `LanguageSourceSet`.
4. Add a `ResourceSet` interface that extends `LanguageSourceSet`.
5. Add a `language-base` plugin that adds a container of `FunctionalSourceSet` instance as a project extension
   called `sources`.
6. Change the `java-base` plugin to apply the `language-base` plugin.
7. Change the `java-base` plugin so that when a source set is added to the `sourceSets` container:
    1. A corresponding `SourceSet` instance is added to the `source` container.
    2. A `JavaSourceSet` instance named `java` is added to this source set, and shares the same Java source
       directory set as the old source set.
    3. A `ResourceSet` instance named `resources` is added to this source set, and shares the same
       resource source directory set as the old source set.

To configure the `main` source set:

    apply plugin: 'java'

    source {
        main {
            java {
                srcDirs 'src/main'
            }
            resources {
                include '**/*.txt'
            }
        }
    }

    assert source.main.java.source.srcDirs == sourceSets.main.java.srcDirs
    assert source.main.resources.source.srcDirs == sourceSets.main.resources.srcDirs

## Story: Introduce Java source set compile classpath

This story introduces the compile classpath for a Java source set, along with the general purpose concept of
a classpath. Initially, a classpath will provide only a read-only view.

1. Add a `Classpath` interface. Initially, this will be a read-only interface. It has the following properties:
    - `files` of type `FileCollection`
2. Add a `compileClasspath` to the `JavaSourceSet` interface.
3. Change the `java-base` plugin so that the Java source set instance it adds has the same compile classpath.

## Story: Introduce class directory binaries

This story adds the concept of class directory binaries and splits the ability to build them from Java
source and resource files out of the `java-base` plugin. At this stage, it will not be possible to define
binaries except via the existing `sourceSets` container.

1. Add a `ClassDirectoryBinary` interface that extends `Named` and `Buildable`. This has the following
   properties:
    - `classesDir` of type `File`.
    - `resourcesDir` of type `File`.
    - `source` of type `DomainObjectCollection<LanguageSourceSet>`.
2. Add a `jvm-lang` plugin
3. The `jvm-lang` plugin to add a container of JVM binaries called `binaries.jvm`.
4. When a `ClassDirectoryBinary` is added to the container:
    - Default `classesDir` to `$buildDir/classes/$name`.
    - Add a lifecycle task, called `classes` for the `main` binary and `${name}Classes` for other binaries.
5. The `jvm-lang` plugin adds a rule that for each `ResourceSet` instance added as source for a
   `ClassDirectoryBinary`, a task of type `ProcessResources` is added which copies the resources into the
    output directory. This task is called `processResources` for the `main` binary and `process${name}Resources`
    for other binaries. It should copy resources into the binary's `resourcesDir`. For this story, it is an error
    to add more than one `ResourcesSet` to a binary.
6. Add a `java-lang` plugin. This applies the `jvm-lang` plugin.
7. The `java-lang` plugin adds a rule that for each `JavaSourceSet` instance added as source for a
   `ClassDirectoryBinary`, a task of type `JavaCompile` is added to compile the source into the output directory.
   This task is called `compileJava` for the `main` binary and `compile${name}Java` for the other binaries.
   It should compile source files into the binary's `classesDir`. For this story, it is an error to add more
   than one `JavaSourceSet` instance to a binary.
8. Change the `java-base` plugin to apply the `java-lang` plugin.
9. Change the `java-base` plugin so that when a source set is added to the `sourceSets` container:
    - Add a `ClassDirectoryBinary` for the source set.
    - No longer adds the lifecycle task, or process resources and Java compile tasks for the source set.
    - Synchronise the source set and binary's classes dir properties.
    - Attach the source set's resources and java source to the binary.

For this story, zero or one `JavaSourceSet` and zero or one `ResourceSet` instances will be supported.
Support for multiple source sets of each language will be added by later stories.

To configure the output of the main Java source and resources:

    apply plugin: 'java'

    binaries {
        jvm {
            main {
                classesDir 'build/main/classes'
            }
        }
    }

    assert sourceSets.main.output.classesDir == jvm.binaries.main.classesDir
    assert compileJava.destinationDir == jvm.binaries.main.classesDir

## Story: Add general purpose polymorphic domain object container

1. Add `NamedDomainObjectContainer` subtypes which allow elements to be added using a name and a type.
2. Allow type -> factory, type -> factory closure and type -> implementation mappings to be specified for a container.
   The type -> implementation mapping should decorate the instances.
3. Change `PublicationContainer` and the publication plugins to use this and remove `GroovyPublicationContainer`.
4. Change `TaskContainer` to extend `PolymorphicDomainObjectContainer` and deprecate the `add()` methods in favour of the inherited
   `create()` methods.
5. Change `SourceSetContainer` and `ConfigurationContainer` to deprecate the `add()` methods in favour of the inherited `create()` methods.

An example of using the container in the DSL:

    publications {
        ivy(IvyPublication) { ... }
        maven(MavenPublication)
    }

### Test coverage

Should be sufficient to use unit tests for the new types and the existing publication integration test coverage.

## Story: Define source sets and binaries without using the `java-base` plugin

This story introduces the ability to define arbitrary source sets and class directory binaries and wire them together.

To build a binary from the main source set:

    apply plugin: 'java'

    binaries {
        jvm {
            release(ClassDirectoryBinary) {
                source sources.main
            }
        }
    }

Running `gradle releaseClasses` will compile the source and copy the resources into the output directory.

To build several binaries from source:

    apply plugin: 'java-lang'

    source {
        api {
            java(JavaSourceSet) { ... }
        }
        impl {
            java(JavaSourceSet) { ... }
            resources(ResourceSet) { ... }
        }
    }

    binaries {
        jvm {
            api(ClassDirectoryBinary) {
                source sources.api.java
            }
            impl(ClassDirectoryBinary) {
                source sources.impl.java
                source sources.impl.resources
            }
        }
    }

Running `gradle implClasses` will compile the impl source.

### Test coverage

- Using the `java-lang` plugin, build two different binaries from the same Java source and resources.
- Using the `jvm-lang` plugin, build a binary from resources only.
- Using the `java-lang` plugin, build an empty binary that has no source.

## Story: Move language and binary support out of the `plugins` project

1. Add a `languages-core` project
2. Move the source set models and implementations to this new project, and to packages `org.gradle.language` and `org.gradle.java.language` and
  corresponding internal packages.
3. Move the binary models and implementation to this new project, and to packages `org.gradle.jvm.binaries` and corresponding internal packages.
4. Move the plugins to this new project, and to packages `org.gradle.language.plugins` and `org.gradle.jvm.plugins` and `org.gradle.java.language.plugins`.
5. Update the default imports to include the new model packages.
6. Update the javadoc includes to include the new public packages.

## Story: Build binaries from multiple source sets

This story adds the ability to build a binary from multiple source sets of different types, where each source set is compiled separately.

1. For each Java source set added to a class directory binary, the `java-lang` plugin adds a Java compile task.
    - When compiling the `main.${lang}` source for the `main` binary, the task should be called `compile${Lang}`.
    - When compiling the `${name}.${lang}` source for the `${name}` binary, the task should be called `compile${Name}${Lang}`.
    - For all other source sets `${function}.${lang}` for the `${binary}` binary, the task should be called `compile${Function}${Lang}For${Binary}Classes`
2. For each resource set added to a class directory binary, the `jvm-lang` plugin adds a process resources task.
    - When processing the `main.${lang}` resources for the `main` binary, the task should be called `process${Lang}`.
    - When processing the `${name}.${lang}` resources for the `${name}` binary, the task should be called `process${Name}${Lang}`.
    - For all other resources sets `${function}.${lang}` for the `${binary}` binary, the task should be called `process${Function}${Lang}For${Binary}Classes`
3. Remove `ClassDirectoryBinary.resourcesTask` property.

For example, to build several binaries from different combinations of source:

    apply plugin: 'java-lang'

    source {
        main {
            java(JavaSourceSet)
            java5(JavaSourceSet)
            resources(ResourceSet)
        }
        test {
            java5(JavaSourceSet)
        }
    }

    binaries {
        jvm {
            main(ClassDirectoryBinary) {
                source source.main.java
                source source.main.java5
                source source.main.resources
            }
            java5(ClassDirectoryBinary) {
                source source.main.java5
                source source.main.resources
            }
            test(ClassDirectoryBinary) {
                source source.test.java5
            }
        }
    }

This defines the following tasks:

* `classes` depends on
    * `compileJava`
    * `compileJava5`
    * `processResources`
* `java5Classes` depends on
    * `compileMainJava5ForJava5Classes`
    * `processMainResourcesForJava5Classes`
* `testClasses` depends on
    * `compileTestJava5`

### Test coverage

- Using the `java-lang` plugin, build a binary from multiple java and resource sets.
- Using the `java` plugin, build a binary from two legacy source sets.
- Using the `java` plugin, build a binary from a legacy source set and a Java source set.

## Story: Build binaries from composite source sets

This story adds the ability to build a binary from a composite source set, where all of the source is jointly compiled.

1. Add a mechanism to ask a source set for each of its language source sets. This is not the same as asking for the source set's children.
2. Adding a source set to a class directory binary is equivalent to adding its language source sets to the binary.
3. Change the composite source set to merge each child of a given language into a single source set.
4. Change the class directory binary to allow a rule to be registered for each source set type, so that when a source set of the
   registered type is added, the rule is invoked to add the appropriate tasks. It should be possible to replace a rule.
   The tasks(s) created by each rule should be added as dependencies of the `classes` task.
5. Change the class directory binary to fail when a source set is added for which there is no rule.
6. Remove `ClassDirectoryBinary.getTaskName()` and `getTaskBaseName()` methods. These should instead be available via the context passed to each rule.

For example, to build a binary from Java, resources and some generated source:

    source {
        main {
            java(JavaSourceSet)
            generatedJava(JavaSourceSet)
            resources(ResourceSet)
        }
    }

    binaries {
        jvm {
            main(ClassDirectoryBinary) {
                source source.main
            }
        }
    }

    compileJava.dependsOn generateJavaSource

TBD - task naming rules

## Story: Build binaries from multiple jointly compiled source sets

This story adds the ability to build a binary from multiple source sets that are jointly compiled. The approach is to allow
multiple source sets to be combined into a single composite source set, which is then added to a class directory binary.

1. Add some conveniences for defining a functional source set.
2. Introduce `CompositeSourceSet`.

    source {
        main {
            java { ... }
            resources { ... }
        }
        free {
            java { ... }
            resources { ... }
        }
        paid {
            java { ... }
            resoures { ... }
        }
        paidMain(CompositeSourceSet) {
            include source.main
            include source.paid
        }
        freeMain(CompositeSourceSet) {
            include source.main
            include source.free
        }
    }

    binaries {
        jvm {
            paidMain(ClassDirectoryBinary) {
                source source.paidMain
            }
            freeMain(ClassDirectoryBinary) {
                source source.freeMain
            }
        }
    }

### Test coverage

- Using the `java-lang` plugin, build a binary from 2 jointly compiled Java source sets.
- Using the `java` plugin, build a binary from 2 jointly compiled legacy source sets.

## Story: Apply conflict resolution to class paths

- For composite source sets, define a `compile` configuration that contains the union of dependencies from the compile
  classpaths of each origin source set.

## Story: Allow custom language source sets to be defined

- Must be able to provide strategy for merging source sets of this type

## Story: Dependencies between source sets

## Story: Java source set runtime classpath

## Story: JAR binaries

This story adds the ability to define JAR binaries and build them from Java source and resource files.

1. Extract `JvmBinary` interface from `ClassDirectoryBinary`.
2. Add a `JarBinary` interface that extends `JvmBinary`.
3. When a JAR binary is added, a `jar` task is added that assembles the JAR.
4. Allow class dir binaries to be added to a JAR packaging.

TBD - integration with the `java-base` plugin.

To assemble a JAR binary:

    apply plugin: 'java-lang'

    source {
        main {
            java { ... }
            resources { ... }
        }
    }

    binaries {
        jvm {
            classes(ClassDirectoryBinary) {
                source source.main
            }
            jar(JarBinary) {
                source jvm.binaries.classes
            }
        }
    }

TBD - naming conventions

## Story: Code quality plugins support arbitrary source sets

This story changes the code quality plugins to analyse the source sets in the `source` container.

Some code quality tasks use the compiled bytecode to perform their analysis, so these plugins will need
to deal with the fact that a given source file may end up in multiple outputs.

## Story: IDE plugins support arbitrary source sets

This story changes the IDE plugins to use the source sets in the `source` container, and to expose all source to the IDE, rather than just the `main` and `test`
source sets.

## Story: Groovy source sets

This story introduces the concept of a Groovy source set and moves the ability to build a jvm binary
from a Groovy source set out of the `groovy-base` plugin.

## Story: Joint compilation of Java and Groovy source sets

## Story: Scala source sets

This story introduces the concept of a Scala source set and moves the ability to build a jvm binary
from a Scala source set out of the `scala-base` plugin.

## Story: Joint compilation of Java and Scala source sets

## Story: ANTLR source sets

This story introduces the concept of generated source, and support for using an ANTRL source set as input
to build a Java source set.

## Story: IDE plugins support generated source

This story changes the IDE plugins to understand that some source is generated and some is not.

## Story: Publish library varants for multiple Groovy or Scala runtimes

## Story: Compile JVM languages against API of dependencies

## Story: C++ source sets

This story introduces the concept of C++ source sets and header source sets, as a refactoring of the
existing C++ plugins.

## Story: Native binaries

This story introduces the concept of native binaries, as a refactoring of the existing C++ plugins.

## Story: JavaScript source sets

This story introduces the concept JavaScript source sets and binaries. A JavaScript binary is simply a
JavaScript source set that is built from some input source sets.

## Story: Sonar-runner plugin supports arbitrary source sets

Using whatever mechanism that the IDE plugins use to distinguish between production and test source.

## Story: Attach binary to Java library component

This story allows a JVM binary to be attached to a Java library component for publishing.

## Story: Deprecate and remove `sourceSets` extension

# Open issues

* Bust up plugins project in some way.
* Wire into `gradle assemble`.
* Add a library convention plugin.
* Consuming vs producing.
* Custom source sets.
* Warn or fail when some source for a class packaging cannot be handled.
* Merge classes and resources?
* Joint compilation.
* Add JavaScript source sets.
* Add more binary types.
* Add more type-specific meta-data on source sets.
* Navigate from a source set to its binaries and from a binary to its source sets.
* Binaries and source sets resolved from a repository.
* Allow a jvm binary to be used as source for another binary.
