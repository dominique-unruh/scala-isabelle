# Using scala-isabelle

## Prerequisites

#### Runtime requirements

* Linux or OS/X
* Java 11 or newer
* [Isabelle 2019 or newer](https://isabelle.in.tum.de/) (needed at runtime)

#### Compile time requirements

* Scala 2.12 or newer
* Java 11 or newer
* [sbt](https://www.scala-sbt.org/) (optional)

(It is also possible to use the library JVM languages other than Scala. See [JavaExample.java](https://github.com/dominique-unruh/scala-isabelle/blob/master/src/test/scala/de/unruh/isabelle/JavaExample.java) for an example.)

## Installation

With sbt, use one of
```sbt
libraryDependencies += "de.unruh" %% "scala-isabelle" % "0.4.1"  // release

libraryDependencies += "de.unruh" %% "scala-isabelle" % "master-SNAPSHOT"  // development snapshot
resolvers += Resolver.sonatypeRepo("snapshots")
```
to add scala-isabelle to your build.

Furthermore, you need to download the Isabelle distribution and unpack it somewhere (not needed for compilation,
so your application could also do it at runtime). In the [example](example.md) below, we will assume that you have installed
Isabelle2022 at `/opt/Isabelle2022`.

