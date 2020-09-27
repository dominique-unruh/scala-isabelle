import sbt.coursierint.LMCoursier

homepage := Some(url("https://github.com/dominique-unruh/scala-isabelle"))

scmInfo := Some(ScmInfo(
  homepage.value.get,
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

publishTo := sonatypePublishToBundle.value

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
credentials += Credentials("GnuPG Key ID", "gpg", "B12742E4CC2172D894730C1AE1F9C7FA4BA66FE2", "ignored")

publish := publish.dependsOn(test in Test).value
publish := PgpKeys.publishSigned.dependsOn(test in Test).value
