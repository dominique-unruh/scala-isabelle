package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.Implicits.listConverter
import de.unruh.isabelle.mlvalue.{MLValue, MLValueWrapper}
import TheoryHeader.Ops

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._

/** Represents a theory header (ML type `Thy_Header.header`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a header `header` as an ML exception is `E_TheoryHeader header`.
 */
final class TheoryHeader private[TheoryHeader] (val mlValue: MLValue[TheoryHeader]) extends MLValueWrapper[TheoryHeader] {
  /** The list of theory imports by the theory with this header.
   * (Corresponds to the `imports` field of the header in ML.) */
  def imports(implicit isabelle: Isabelle, ec: ExecutionContext) : List[String] = Ops.getImports(this).retrieveNow
}

object TheoryHeader extends MLValueWrapper.Companion[TheoryHeader] {
  override protected val mlType: String = "Thy_Header.header"
  override protected val predefinedException: String = "E_TheoryHeader"
  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
    import MLValue.compileFunction
    lazy val getImports = compileFunction[TheoryHeader, List[String]](
      "fn {imports, ...} => map fst imports")
  }
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops

  override protected def instantiate(mlValue: MLValue[TheoryHeader]): TheoryHeader = new TheoryHeader(mlValue)
}

