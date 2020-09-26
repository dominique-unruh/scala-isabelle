package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import org.log4s
import org.log4s.Logger

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex.{Groups, Match}

// Implicits
import Implicits._

// DOCUMENT
object Version extends OperationCollection {
  // DOCUMENT
  final val NOT_RC = 99999
  // DOCUMENT
  final val INVALID_YEAR = 9999
  override protected def newOps(implicit isabelle: Isabelle, ec:  ExecutionContext): Ops = new Ops
  protected class Ops(implicit isabelle: Isabelle, ec:  ExecutionContext) {
    MLValue.init()
    val versionString: String = MLValue.compileValue[String]("Distribution.version").retrieveNow

    val (year, step, rc) = {
      val regex = """Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?:""".r
      regex.findPrefixMatchOf(versionString) match {
        case None => (INVALID_YEAR, 0, NOT_RC)
        case Some(matcher) =>
          val year = matcher.group("year").toInt
          val step = matcher.group("step") match { case null => 0; case step => step.toInt }
          val rc = matcher.group("rc") match { case null => NOT_RC; case rc => rc.toInt }
          (year, step, rc)
      }
    }
  }

  // DOCUMENT
  def versionString(implicit isabelle: Isabelle, ec:  ExecutionContext): String = Ops.versionString
  // DOCUMENT
  def year(implicit isabelle: Isabelle, ec:  ExecutionContext): Int = Ops.year
  // DOCUMENT
  def step(implicit isabelle: Isabelle, ec:  ExecutionContext): Int = Ops.step
  // DOCUMENT
  def rc(implicit isabelle: Isabelle, ec:  ExecutionContext): Int = Ops.rc

  def from2020(implicit isabelle: Isabelle, ec:  ExecutionContext): Boolean = year >= 2020

  val logger: Logger = log4s.getLogger
}
