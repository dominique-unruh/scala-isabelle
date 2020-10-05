package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.Implicits.listConverter
import de.unruh.isabelle.mlvalue.MLValue
import TheoryHeader.Ops

import scala.concurrent.ExecutionContext
import scala.language.postfixOps

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

// DOCUMENT
final class TheoryHeader private[TheoryHeader] (val mlValue: MLValue[TheoryHeader]) extends MLValueWrapper[TheoryHeader] {
  def imports(implicit isabelle: Isabelle, ec: ExecutionContext) : List[String] = Ops.getImports(this).retrieveNow
}

object TheoryHeader extends MLValueWrapper.Companion[TheoryHeader] {
  override protected val mlType: String = "Thy_Header.header"
  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) extends super.Ops {
    import MLValue.compileFunction
    lazy val getImports = compileFunction[TheoryHeader, List[String]](
      "fn {imports, ...} => map fst imports")
  }
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops

  override protected def instantiate(mlValue: MLValue[TheoryHeader]): TheoryHeader = new TheoryHeader(mlValue)
}

