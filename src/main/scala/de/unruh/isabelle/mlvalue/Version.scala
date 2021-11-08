package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, IsabelleException, OperationCollection}
import org.log4s
import org.log4s.Logger

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex.{Groups, Match}

// Implicits
import Implicits._

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

  override protected def newOps(implicit isabelle: Isabelle, ec:  ExecutionContext): Ops = new Ops
  protected class Ops(implicit isabelle: Isabelle, ec:  ExecutionContext) {
//    MLValue.init()
    val versionString: String =
      try
        MLValue.compileValue[Option[String]]("Isabelle_System.isabelle_identifier()").retrieveNow.get
      catch {
        case _ : IsabelleException => MLValue.compileValue[String]("Distribution.version").retrieveNow
      }

    val (year, step, rc) = {
      val regex = """^Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?(:.*)?$""".r
      regex.findFirstMatchIn(versionString) match {
        case None => (INVALID_YEAR, 0, NOT_RC)
        case Some(matcher) =>
          val year = matcher.group("year").toInt
          val step = matcher.group("step") match { case null => 0; case step => step.toInt }
          val rc = matcher.group("rc") match { case null => NOT_RC; case rc => rc.toInt }
          (year, step, rc)
      }
    }
  }

  /** The Isabelle version string (e.g., `"Isabelle2020: April 2020"`) */
  def versionString(implicit isabelle: Isabelle, ec:  ExecutionContext): String = Ops.versionString
  /** The year of this Isabelle version (e.g., 2020).
   *
   * If the version string could not be parsed, returns [[INVALID_YEAR]]. */
  def year(implicit isabelle: Isabelle, ec:  ExecutionContext): Int = Ops.year
  /** The version within the current year.
   *
   * E.g. `Isabelle2020` would have `step=0`, and `Isabelle2020-1` would have `step=1`. */
  def step(implicit isabelle: Isabelle, ec:  ExecutionContext): Int = Ops.step
  /** Number of the release candidate.
   *
   * E.g., `Isabelle2020-RC4` would have `rc=4`.
   * If this is not a release candidate, `rc=`[[NOT_RC]]. */
  def rc(implicit isabelle: Isabelle, ec:  ExecutionContext): Int = Ops.rc

  /** True, if the current version is at least Isabelle2020 (including the release candidates).
   *
   * Unspecified behavior on development versions. */
  def from2020(implicit isabelle: Isabelle, ec:  ExecutionContext): Boolean = year >= 2020

  /** True, if the current version is at least Isabelle2020 (including the release candidates).
   *
   * Unspecified behavior on development versions. */
  def from2021(implicit isabelle: Isabelle, ec:  ExecutionContext): Boolean = year >= 2021

  private val logger = log4s.getLogger
}
