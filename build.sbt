import java.io.PrintWriter

import org.apache.commons.lang3.SystemUtils
import sbt.io
import sbt.io.Path.relativeTo

import scala.sys.process._

name := "scala-isabelle"
version := "master-SNAPSHOT"

crossScalaVersions := List("2.13.3", "2.12.12")

scalaVersion := "2.13.3"
//scalaVersion := "2.12.12"

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

lazy val circleCIRandomize = taskKey[Unit]("Randomize which test is run on CircleCI next time")
circleCIRandomize := {
    if (!SystemUtils.IS_OS_WINDOWS) // On my machine, Windows doesn't have enough tools installed.
        if (Process("git diff --quiet", cwd=baseDirectory.value).! != 0) {
            print(Process("scripts/circleci-randomize.py", cwd=baseDirectory.value).!!)
        }
}
compile in Compile := (compile in Compile).dependsOn(circleCIRandomize).value
doc in Compile := (doc in Compile).dependsOn(circleCIRandomize).value

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
