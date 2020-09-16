import scala.sys.process._

name := "scala-isabelle"

version := "0.1.0-SNAPSHOT"

crossScalaVersions := List("2.13.3", "2.12.12")

scalaVersion := "2.13.3"

Global / onChangedBuildSource := ReloadOnSourceChanges

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.2" % "test"
// https://mvnrepository.com/artifact/org.log4s/log4s
libraryDependencies += "org.log4s" %% "log4s" % "1.8.2"
// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30"
libraryDependencies += "commons-io" % "commons-io" % "2.8.0"
// https://mvnrepository.com/artifact/org.scalaz/scalaz-core
// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.3.2"
// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.11"
// https://mvnrepository.com/artifact/com.google.guava/guava
libraryDependencies += "com.google.guava" % "guava" % "29.0-jre"

lazy val travisRandomize = taskKey[Unit]("Randomize which test is run on Travis next time")
travisRandomize := {
  if (Process("git diff --quiet", cwd=baseDirectory.value).! != 0)
    print(Process("scripts/travis-randomize.py", cwd=baseDirectory.value).!!)
}
compile in Compile := (compile in Compile).dependsOn(travisRandomize).value
