/*

 Steps when releasing a release/release candidate:

 - git checkout release-candidate
 - If this is the first RC for a new release, reset release-candidate to master
 - Update CHANGELOG.md if needed
 - Set correct date (today) for this version in CHANGELOG.md (only for releases)
 - Edit version in README.md
 - git commit (to be able to cherry pick those into master)
 - Set version in build.sbt
 - git commit
 - sbt clean
 - sbt +publishSigned (don't forget the +) (should run tests!)
 - gpg -v --keyserver hkp://keys.openpgp.org --send-keys e1f9c7fa4ba66fe2
 - sbt sonatypeBundleRelease
 - git tag vXXX (XXX is the version)
 - git push origin vXXX
 - git push
 - git checkout master
 - Cherry pick commit with edits to CHANGELOG.md and README.md
 - Check (a while later): https://mvnrepository.com/artifact/de.unruh/scala-isabelle

*/

homepage := Some(url("https://dominique-unruh.github.io/scala-isabelle"))

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
    |It allows to execute ML code inside the Isabelle process, and to operate on theories, theorems, terms, etc.
    |See the webpage (https://dominique-unruh.github.io/scala-isabelle) for more information.""".stripMargin

licenses += "MIT" -> url("https://raw.githubusercontent.com/dominique-unruh/scala-isabelle/5f28d8e6248f39dd7a31649d92c9850498e3985c/LICENSE")
licenses += "Isabelle" -> url("https://raw.githubusercontent.com/dominique-unruh/scala-isabelle/5f28d8e6248f39dd7a31649d92c9850498e3985c/COPYRIGHT.Isabelle")

pomIncludeRepository := { _ => false }

publishTo := sonatypePublishToBundle.value

publishMavenStyle := true

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
credentials += Credentials("GnuPG Key ID", "gpg", "B12742E4CC2172D894730C1AE1F9C7FA4BA66FE2", "ignored")

publish := publish.dependsOn(Test / test).value
PgpKeys.publishSigned := PgpKeys.publishSigned.dependsOn(Test / test).value
