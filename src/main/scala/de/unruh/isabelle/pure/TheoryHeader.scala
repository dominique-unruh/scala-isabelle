package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.Implicits.listConverter
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import TheoryHeader.Ops

import scala.language.postfixOps

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._

/** Represents a theory header (ML type `Thy_Header.header`) in the Isabelle process.
 *
 * A theory header defines the theory name, its imports (parent theories) and keywords with abbreviations.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a header `header` as an ML exception is `E_TheoryHeader header`.
 */
final class TheoryHeader private[TheoryHeader] (val mlValue: MLValue[TheoryHeader]) extends MLValueWrapper[TheoryHeader] {
  /** The name of the theory. */
  def name(implicit isabelle: Isabelle) : String = Ops.getName(this).retrieveNow
  /** The list of theory imports by the theory with this header.
   * (Corresponds to the `imports` field of the header in ML.) */
  def imports(implicit isabelle: Isabelle) : List[String] = Ops.getImports(this).retrieveNow
}

object TheoryHeader extends MLValueWrapper.Companion[TheoryHeader] {
  override protected val mlType: String = "Thy_Header.header"
  override protected val predefinedException: String = "E_TheoryHeader"

  /**
    * Parses a theory header from text.
    *
    * The text can be a prefix like "theory Foo imports Bar Baz begin" or a full theory file.
    */
  def parse(text: String)(implicit isabelle: Isabelle) : TheoryHeader =
    Ops.parse(text).retrieveNow

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle) extends super.Ops {
    import MLValue.compileFunction
    lazy val getName = compileFunction[TheoryHeader, String](
      "fn {name, ...} => fst name")
    lazy val getImports = compileFunction[TheoryHeader, List[String]](
      "fn {imports, ...} => map fst imports")
    lazy val parse = compileFunction[String, TheoryHeader](
      "fn (text) => Thy_Header.read Position.start text")
  }
  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops

  override protected def instantiate(mlValue: MLValue[TheoryHeader]): TheoryHeader = new TheoryHeader(mlValue)
}

