package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, IsabelleMiscException}
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper, Version}
import de.unruh.isabelle.mlvalue.MLValue.{compileFunction, compileValue}
import de.unruh.isabelle.mlvalue.Implicits._
import Position.Ops

/** Represents a position (ML type `Position.T`) in the Isabelle process.
 *
 * Positions represent locations in source files: line number, beginning offset (inclusive), end offset (exclusive).
 * A position may also have an associated file name or an id.
 * Line numbers and offsets start from 1.
 * Offsets count Isabelle symbols (not UTF8 or UTF16 characters) in the whole text (not just the current line).
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a position `pos` as an ML exception is `E_Position pos`.
 */
final class Position private [Position](val mlValue : MLValue[Position]) extends MLValueWrapper[Position] {
  override def toString: String = "position" + mlValue.stateString

  def line(implicit isabelle: Isabelle): Option[Int] = Ops.lineOf(this).retrieveNow
  def offset(implicit isabelle: Isabelle): Option[Int] = Ops.offsetOf(this).retrieveNow
  def endOffset(implicit isabelle: Isabelle): Option[Int] = Ops.endOffsetOf(this).retrieveNow
  def file(implicit isabelle: Isabelle): Option[String] = Ops.fileOf(this).retrieveNow
  /** Returns the id of the position.
   * Supported only since Isabelle2022 */
  def id(implicit isabelle: Isabelle): Option[String] = Ops.idOf(this).retrieveNow

  /** Returns the substring at this position, given the complete source text. */
  def extract(text: String)(implicit isabelle: Isabelle): String = Ops.extract(this, text).retrieveNow
  /** Returns the substring from this position's offset (inclusive) to another's offset (exclusive). */
  def extractUntil(end: Position, text: String)(implicit isabelle: Isabelle): String = Ops.extractRange(this, end, text).retrieveNow
}

object Position extends MLValueWrapper.Companion[Position] {
  override protected val mlType = "Position.T"
  override protected val predefinedException: String = "E_Position"

  override protected def instantiate(mlValue: MLValue[Position]): Position = new Position(mlValue)

  protected class Ops(implicit isabelle: Isabelle) extends super.Ops {
    lazy val none: Position = compileValue[Position]("Position.none").retrieveNow
    lazy val start: Position = compileValue[Position]("Position.start").retrieveNow
    lazy val startWithFileName = compileFunction[String, Position]("Position.file")
    lazy val startWithId = compileFunction[String, Position]("Position.id")

    lazy val lineOf = compileFunction[Position, Option[Int]]("Position.line_of")
    lazy val offsetOf = compileFunction[Position, Option[Int]]("Position.offset_of")
    lazy val endOffsetOf = compileFunction[Position, Option[Int]]("Position.end_offset_of")
    lazy val fileOf = compileFunction[Position, Option[String]]("Position.file_of")
    lazy val idOf =
      if (Version.from2020)
        compileFunction[Position, Option[String]]("Position.id_of")
      else
        throw IsabelleMiscException("Position.id_of not available before Isabelle2020")

    lazy val extract = compileFunction[Position, String, String](
      """fn (pos, s) =>
        | case (Position.offset_of pos, Position.end_offset_of pos) of
        |   (SOME istart, SOME iend) => Symbol.explode s |> Library.drop (istart - 1) |> Library.take (iend - istart) |> String.concat
        | | (SOME istart, NONE) => Symbol.explode s |> Library.drop (istart - 1) |> String.concat
        | | _ => raise Fail "Position.extract: no offset"
        |""".stripMargin
    )
    lazy val extractRange = compileFunction[Position, Position, String, String](
      """fn (start_pos, end_pos, s) =>
        | case (Position.offset_of start_pos, Position.offset_of end_pos) of
        |   (SOME istart, SOME iend) => Symbol.explode s |> Library.drop (istart - 1) |> Library.take (iend - istart) |> String.concat
        | | (SOME istart, NONE) => Symbol.explode s |> Library.drop (istart - 1) |> String.concat
        | | _ => raise Fail "Position.extract: no offset"
        |""".stripMargin
    )
  }

  /** Represents an unspecified position (`Position.none` in ML). */
  def none(implicit isabelle: Isabelle): Position = Ops.none
  /** Represents a starting position with no associated filename or ID (`Position.start` in ML). */
  def start(implicit isabelle: Isabelle): Position = Ops.start
  /** Represents the starting position in a given file (`Position.file` in ML). */
  def startWithFileName(fileName: String)(implicit isabelle: Isabelle): Position = Ops.startWithFileName(fileName).retrieveNow
  /** Represents the starting position in a given text (`Position.id` in ML). */
  def startWithId(id: String)(implicit isabelle: Isabelle): Position = Ops.startWithId(id).retrieveNow

  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops
}
