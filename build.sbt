import java.io.PrintWriter

import org.apache.commons.lang3.SystemUtils
import sbt.io
import sbt.io.Path.relativeTo

import scala.sys.process._

lazy val component = RootProject(file("component"))

/** This should be the directory that contains all Isabelle installations (needed for compiling)
 * If support for only some Isabelle versions should be built, set [buildOnlyFor] below
 */
val isabelleHomeDirectories = file("/opt")
/** Set to Some(List(version, version, ...)) to select which Isabelle versions to support. */
val buildOnlyFor = None

def pideWrapper(version: String, scala: String) = {
  if (buildOnlyFor.exists(!_.contains(version))) // version not in buildOnlyFor (and buildOnlyFor != None)
    Project(s"pidewrapper$version-dummy", file(s"pidewrappers/$version")).
      settings(Compile / sources := List())
  else
    Project(s"pidewrapper$version", file(s"pidewrappers/$version")).settings(
      Compile / sourceDirectories += baseDirectory.value,
      scalaVersion := scala,
      Compile / unmanagedJars := {
        val isabelleHome = isabelleHomeDirectories / s"Isabelle$version"
        assert(isabelleHome.canRead, isabelleHome)
        val cp = ((isabelleHome / "lib" / "classes" +++ isabelleHome / "contrib") ** "*.jar").classpath
        assert(cp.nonEmpty)
        cp
      },
      // Add classes from root project to classpath (need PIDEWrapper.class)
      Compile / managedClasspath += {
        val classes = (Compile/classDirectory).in(root).value
        // This makes sure the directory "classes" contains up-to-date classes from the root project
        (Compile/compile).in(root).value
        classes
      }
    )
}

lazy val pidewrapper2021 = pideWrapper("2021", scala="2.13.4")

lazy val root = (project in file("."))
  .withId("scala-isabelle")

Compile / managedResources ++= {
  // This compiles all projects in inProjects(...) and returns the resulting jars
  val jars = (Compile/packageBin).all(ScopeFilter(inProjects(pidewrapper2021))).value
  for (jar <- jars) yield {
    val version = jar.relativeTo(baseDirectory.value / "pidewrappers").get.toPath.getName(0)
    val target = (Compile/managedResourceDirectories).value.head / "de/unruh/isabelle/control" / s"pidewrapper$version.jar"
    IO.copyFile(jar, target)
    target
  }
}

name := "scala-isabelle"
version := "master-SNAPSHOT"

crossScalaVersions := List("2.13.3", "2.12.12")

//scalaVersion := "2.13.3"
scalaVersion := "2.12.12"

Global / onChangedBuildSource := ReloadOnSourceChanges

libraryDependencies += "de.unruh" % "java-patterns" % "0.1.0"
//resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.3" % "test"
// https://mvnrepository.com/artifact/org.log4s/log4s
libraryDependencies += "org.log4s" %% "log4s" % "1.9.0"
// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30"
// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"
// https://mvnrepository.com/artifact/org.scalaz/scalaz-core
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.3.2"
// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.11"
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
libraryDependencies += "org.apache.commons" % "commons-text" % "1.9"
// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "30.0-jre"
libraryDependencies += "org.jetbrains" % "annotations" % "20.1.0"

// TODO: mark as compile time only or something
libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test"

lazy val makeGitrevision = taskKey[File]("Create gitrevision.txt")
Compile / resourceGenerators += makeGitrevision.map(Seq(_))
makeGitrevision := {
    val file = (Compile / resourceManaged).value / "de" / "unruh" / "isabelle" / "gitrevision.txt"
    file.getParentFile.mkdirs()
    if (SystemUtils.IS_OS_WINDOWS) {
        val pr = new PrintWriter(file)
        pr.println("Built under windows, not adding gitrevision.txt") // On my machine, Windows doesn't have enough tools installed.
        pr.close()
    } else if ((baseDirectory.value / ".git").exists())
        Process(List("bash","-c",s"( date && git describe --tags --long --always --dirty --broken && git describe --always --all ) > ${file}")).!!
    else {
        val pr = new PrintWriter(file)
        pr.println("Not built from a GIT worktree.")
        pr.close()
    }
    file
}
Compile / packageSrc / mappings ++= makeGitrevision.value pair relativeTo((Compile / resourceManaged).value)
Compile / packageDoc / mappings ++= makeGitrevision.value pair relativeTo((Compile / resourceManaged).value)

Compile / doc / scalacOptions ++=
    Opts.doc.sourceUrl(s"https://github.com/dominique-unruh/scala-isabelle/tree/${"git rev-parse HEAD".!!}€{FILE_PATH_EXT}#L€{FILE_LINE}")
Compile / doc / scalacOptions ++= Seq("-sourcepath", baseDirectory.value.toString)
Compile / doc / scalacOptions ++= Seq("-skip-packages", "scalaz") // Otherwise documentation for scalaz.syntax is included for some reason
