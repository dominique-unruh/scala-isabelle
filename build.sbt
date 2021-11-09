import java.io.{FileNotFoundException, PrintWriter}
import org.apache.commons.lang3.SystemUtils
import sbt.io
import sbt.io.Path.relativeTo

import scala.sys.process._

/** This should be the directory that contains all Isabelle installations (needed for compiling).
 * If support for only some Isabelle versions should be built, set [buildOnlyFor] below.
 * If several directories are given, the build looks in each of them.
 */
val isabelleHomeDirectories = IsabelleHomeDirectories(
  file("/opt"),
  file(System.getProperty("user.home")) / "install" // In CircleCI
)

/** Set to Some(List(version, version, ...)) to select which Isabelle versions to support. */
val buildOnlyFor: Option[List[String]] = None

def pideWrapper(version: String, scala: String) = {
  if (buildOnlyFor.exists(!_.contains(version))) { // version not in buildOnlyFor (and buildOnlyFor != None)
    print(s"Skipping compilation of pidewrapper$version")
    Project(s"pidewrapper$version-dummy", file(s"pidewrappers/$version")).
      settings(Compile / sources := List())
  } else
    Project(s"pidewrapper$version", file(s"pidewrappers/$version")).settings(
      Compile / sourceDirectories += baseDirectory.value,
      scalaVersion := scala,
      Compile / unmanagedJars := isabelleHomeDirectories.getClasspath(version),
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
lazy val pidewrapper2021_1 = pideWrapper("2021-1", scala="2.13.5")

lazy val component = project
  .dependsOn(root)
  .settings(
    scalaVersion := "2.13.4",
    Compile / packageBin / artifactPath := baseDirectory.value / "scala-isabelle-component.jar",
    Compile / unmanagedJars := isabelleHomeDirectories.getClasspath("2021"),
    Compile / packageBin := {
      Build.copyClasspath(dependencyClasspath.in(Runtime).value, baseDirectory.value / "dependencies");
      Build.copyFileInto(packageBin.in(root).in(Compile).value, baseDirectory.value / "dependencies")
      (Compile/packageBin).value }
  )

lazy val root = (project in file("."))
  .withId("scala-isabelle")

Compile / managedResources ++= {
  // This compiles all projects in inProjects(...) and returns the resulting jars
  val jars = (Compile/packageBin).all(ScopeFilter(inProjects(pidewrapper2021, pidewrapper2021_1))).value
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

scalaVersion := "2.13.3"
//scalaVersion := "2.12.12"

Global / onChangedBuildSource := ReloadOnSourceChanges

libraryDependencies ++= Seq(
  "de.unruh" % "java-patterns" % "0.1.0",
  "org.scalatest" %% "scalatest" % "3.2.3" % "test",
  // https://mvnrepository.com/artifact/org.log4s/log4s
  "org.log4s" %% "log4s" % "1.9.0",
  // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
  "org.slf4j" % "slf4j-simple" % "1.7.30",
  // https://mvnrepository.com/artifact/commons-io/commons-io
  "commons-io" % "commons-io" % "2.8.0",
  // https://mvnrepository.com/artifact/org.scalaz/scalaz-core
  "org.scalaz" %% "scalaz-core" % "7.3.2",
  // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
  "org.apache.commons" % "commons-lang3" % "3.11",
  // https://mvnrepository.com/artifact/org.apache.commons/commons-text
  "org.apache.commons" % "commons-text" % "1.9",
  // https://mvnrepository.com/artifact/com.google.guava/guava
  "com.google.guava" % "guava" % "30.0-jre",
  "org.jetbrains" % "annotations" % "20.1.0",

  // TODO: mark as compile time only or something
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,

  "org.scala-lang" % "scala-compiler" % scalaVersion.value % "test",
)

lazy val makeGitrevision = taskKey[File]("Create gitrevision.txt")
Compile / resourceGenerators += makeGitrevision.map(Seq(_))
makeGitrevision := Build.makeGitrevision(baseDirectory.value,
  (Compile / resourceManaged).value / "de" / "unruh" / "isabelle" / "gitrevision.txt")
Compile / packageSrc / mappings ++= makeGitrevision.value pair relativeTo((Compile / resourceManaged).value)
Compile / packageDoc / mappings ++= makeGitrevision.value pair relativeTo((Compile / resourceManaged).value)

Compile / doc / scalacOptions ++=
    Opts.doc.sourceUrl(s"https://github.com/dominique-unruh/scala-isabelle/tree/${"git rev-parse HEAD".!!}€{FILE_PATH_EXT}#L€{FILE_LINE}")
Compile / doc / scalacOptions ++= Seq("-sourcepath", baseDirectory.value.toString)
Compile / doc / scalacOptions ++= Seq("-skip-packages", "scalaz") // Otherwise documentation for scalaz.syntax is included for some reason
