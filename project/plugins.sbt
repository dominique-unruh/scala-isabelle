// https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.14.0"

addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "3.0.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
// addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

// Invoke via dependencyUpdates
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.6.0")