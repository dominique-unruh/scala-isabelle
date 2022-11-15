import java.io.PrintWriter

import org.apache.commons.lang3.SystemUtils
import sbt.io
import sbt.io.Path.relativeTo

import scala.sys.process._

lazy val component = RootProject(file("component"))

name := "scala-isabelle"
version := "master-SNAPSHOT"

crossScalaVersions := List("2.13.10", "2.12.17")

scalaVersion := "2.13.10"
//scalaVersion := "2.12.15"

Global / onChangedBuildSource := ReloadOnSourceChanges

libraryDependencies += "de.unruh" % "java-patterns" % "0.1.0"
//resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "com.vladsch.flexmark" % "flexmark-all" % "0.64.0" % Test // Required by scala-test for HTML output
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % Test

// https://mvnrepository.com/artifact/org.log4s/log4s
libraryDependencies += "org.log4s" %% "log4s" % "1.10.0"
// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.3"
// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.11.0"
// https://mvnrepository.com/artifact/org.scalaz/scalaz-core
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.3.6"
// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.12.0"
// https://mvnrepository.com/artifact/org.apache.commons/commons-text
libraryDependencies += "org.apache.commons" % "commons-text" % "1.10.0"
// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "31.1-jre"
libraryDependencies += "org.jetbrains" % "annotations" % "23.0.0"
libraryDependencies += "com.ibm.icu" % "icu4j" % "72.1"

// See https://stackoverflow.com/a/21516954
val CompileOnly = config("compileonly").hide
ivyConfigurations += CompileOnly
Compile / unmanagedClasspath ++= update.value.select(configurationFilter("compileonly"))

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % CompileOnly

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
