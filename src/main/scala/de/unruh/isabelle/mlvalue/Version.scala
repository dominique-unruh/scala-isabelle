package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, IsabelleMLException, IsabelleMiscException, OperationCollection}
import org.log4s
import org.log4s.Logger

import java.nio.file.{Files, Path}
import scala.util.matching.Regex.{Groups, Match}

// Implicits
import Implicits._
import scala.collection.JavaConverters._ // Using deprecated class to support Scala 2.12

/** Version information for the Isabelle process.
 *
 * All methods take the Isabelle process as an implicit parameter.
 * The integers [[year]], [[step]], [[rc]] are chosen such
 * that `(year,step,rc)` is increasing lexicographically if the version increases.
 * (Development releases are currently not supported and may break this ordering.)
 **/
object Version extends OperationCollection {
  /** Value for [[rc]] that represents that the version is not a release candidate.
   * Guaranteed to be larger than any release candidate number. */
  final val NOT_RC = 99999
  /** Value for [[year]] that indicates that the version could not be determined.
   * Guaranteed to be larger than any correct year value */
  final val INVALID_YEAR = 99998

  private lazy val isabelleVersionRegex = """^Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?(:.*)?$""".r
  private lazy val isabelleVersionRegexExe = """^Isabelle((?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?)(\.exe|\.plist)?$""".r

  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops
  protected class Ops(implicit isabelle: Isabelle) {
//    MLValue.init()
    val versionString: String =
      try
        MLValue.compileValue[Option[String]]("Isabelle_System.isabelle_identifier()").retrieveNow.getOrElse("dev")
      catch {
        case _ : IsabelleMLException => MLValue.compileValue[String]("Distribution.version").retrieveNow
      }

    val (year, step, rc) = {
      isabelleVersionRegex.findFirstMatchIn(versionString) match {
        case None => (INVALID_YEAR, 0, NOT_RC)
        case Some(matcher) =>
          val year = matcher.group("year").toInt
          val step = matcher.group("step") match { case null => 0; case step => step.toInt }
          val rc = matcher.group("rc") match { case null => NOT_RC; case rc => rc.toInt }
          (year, step, rc)
      }
    }
  }

  /** The Isabelle version string (e.g., `"Isabelle2020: April 2020"`).
   * `"dev"` if the version string cannot be obtained (usually the case with Isabelle running directly from the development repository). */
  def versionString(implicit isabelle: Isabelle): String = Ops.versionString

  /** Guesses the version by inspecting the Isabelle home directory.
   * (A string such as `"2021-1-RC2"`.)
   * Compared from [[versionString]], this does not need the Isabelle process to start up fully.
   * (Starting up may fail if the version is wrong because startup may build an Isabelle heap from theories for a
   * different version first, depending on the setup.)
   *
   * @throws IsabelleMiscException if the version cannot be determined
   * */
  def versionFromIsabelleDirectory(directory: Path): String = {
    val files1 = Files.list(directory).iterator.asScala.toSeq
    val subdir = directory.resolve("Isabelle") // On older Mac Isabelle versions, this contains the actual program directory
    val files2 = if (Files.isDirectory(subdir)) Files.list(subdir).iterator.asScala.toSeq else Nil
    val files = files1 ++ files2

    val matches = files.flatMap { file =>
      val fileName = file.getFileName.toString
      fileName match {
        case `isabelleVersionRegexExe`(version, _*) => Some((fileName, version))
        case _ => None
      }
    }

    if (matches.isEmpty)
      throw IsabelleMiscException(s"Could not determine Isabelle version in $directory: no main executable found")
    else if (matches.length >= 2)
      throw IsabelleMiscException(s"Could not determine Isabelle version in $directory: found ${matches.map(_._1).mkString(", ")}")
    else
      matches.head._2
  }

  /** The year of this Isabelle version (e.g., 2020).
   *
   * If the version string could not be parsed, returns [[INVALID_YEAR]]. */
  def year(implicit isabelle: Isabelle): Int = Ops.year
  /** The version within the current year.
   *
   * E.g. `Isabelle2020` would have `step=0`, and `Isabelle2020-1` would have `step=1`. */
  def step(implicit isabelle: Isabelle): Int = Ops.step
  /** Number of the release candidate.
   *
   * E.g., `Isabelle2020-RC4` would have `rc=4`.
   * If this is not a release candidate, `rc=`[[NOT_RC]]. */
  def rc(implicit isabelle: Isabelle): Int = Ops.rc

  /** True, if the current version is at least Isabelle2020 (including the release candidates).
   *
   * Unspecified behavior on development versions. */
  def from2020(implicit isabelle: Isabelle): Boolean = year >= 2020

  /** True, if the current version is at least Isabelle2021 (including the release candidates).
   *
   * Unspecified behavior on development versions. */
  def from2021(implicit isabelle: Isabelle): Boolean = year >= 2021

  /** True, if the current version is at least Isabelle2021-1 (including the release candidates).
   *
   * Unspecified behavior on development versions. */
  def from2021_1(implicit isabelle: Isabelle): Boolean =
    (year == 2021 && step >= 1) || year > 2021

  /** True, if the current version is at least Isabelle2022 (including the release candidates).
   *
   * Unspecified behavior on development versions. */
  def from2022(implicit isabelle: Isabelle): Boolean = year >= 2022

  private val logger = log4s.getLogger
}
