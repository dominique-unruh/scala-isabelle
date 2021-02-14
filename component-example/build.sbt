name := "isabelle-example-component"

Global / onChangedBuildSource := ReloadOnSourceChanges

//version := "snapshot"

scalaVersion := "2.13.4"

val isabelleHome = file("/opt/Isabelle2021-RC6")

Compile / packageBin / artifactPath :=
  baseDirectory.value / "isabelle-example-component.jar"

unmanagedJars in Compile :=
  ((isabelleHome / "lib" / "classes" +++ isabelleHome / "contrib") ** "*.jar").classpath
