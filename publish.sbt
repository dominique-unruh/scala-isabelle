import sbt.coursierint.LMCoursier

scmInfo := Some(ScmInfo(
  url("https://github.com/dominique-unruh/scala-isabelle"),
  "https://github.com/dominique-unruh/scala-isabelle.git"))

developers += Developer(
  id = "unruh",
  name = "Dominique Unruh",
  email = "dominique@unruh.de",
  url = url("https://www.ut.ee/~unruh/")
)

organization := "de.unruh"

description :=
  """This library allows to control an Isabelle process (https://isabelle.in.tum.de/) from a Scala program.
    |It allows to execute ML code inside the Isabelle process, and to operate on theories, theorems, terms, etc.""".stripMargin

licenses += "MIT" -> url("https://raw.githubusercontent.com/dominique-unruh/scala-isabelle/5f28d8e6248f39dd7a31649d92c9850498e3985c/LICENSE")
licenses += "Isabelle" -> url("https://raw.githubusercontent.com/dominique-unruh/scala-isabelle/5f28d8e6248f39dd7a31649d92c9850498e3985c/COPYRIGHT.Isabelle")

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

publish := publish.dependsOn(test in Test).value
