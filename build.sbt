import java.io.PrintWriter

import scala.sys.process._

name := "scala-isabelle"
version := "master-SNAPSHOT"

crossScalaVersions := List("2.13.3", "2.12.12")

scalaVersion := "2.13.3"
//scalaVersion := "2.12.12"

Global / onChangedBuildSource := ReloadOnSourceChanges

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.2" % "test"
// https://mvnrepository.com/artifact/org.log4s/log4s
libraryDependencies += "org.log4s" %% "log4s" % "1.8.2"
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
libraryDependencies += "com.google.guava" % "guava" % "29.0-jre"

lazy val travisRandomize = taskKey[Unit]("Randomize which test is run on Travis next time")
travisRandomize := {
  if (Process("git diff --quiet", cwd=baseDirectory.value).! != 0)
    print(Process("scripts/travis-randomize.py", cwd=baseDirectory.value).!!)
}
compile in Compile := (compile in Compile).dependsOn(travisRandomize).value
doc in Compile := (doc in Compile).dependsOn(travisRandomize).value

lazy val makeGitrevision = taskKey[Unit]("Create gitrevision.txt")
makeGitrevision := {
    val file = baseDirectory.value / "src" / "main" / "resources" / "de" / "unruh" / "isabelle" / "gitrevision.txt"
    if ((baseDirectory.value / ".git").exists())
        Process(List("bash","-c",s"( date && git describe --tags --long --always --dirty --broken && git describe --always --all ) > ${file}")).!!
    else {
        val pr = new PrintWriter(file)
        pr.println("Not built from a GIT worktree.")
        pr.close()
    }
}
managedResources in Compile := (managedResources in Compile).dependsOn(makeGitrevision).value

Compile / doc / scalacOptions ++=
    Opts.doc.sourceUrl(s"https://github.com/dominique-unruh/scala-isabelle/tree/${"git rev-parse HEAD".!!}€{FILE_PATH_EXT}#L€{FILE_LINE}")
Compile / doc / scalacOptions ++= Seq("-sourcepath", baseDirectory.value.toString)
Compile / doc / scalacOptions ++= Seq("-skip-packages", "scalaz") // Otherwise documentation for scalaz.syntax is included for some reason
