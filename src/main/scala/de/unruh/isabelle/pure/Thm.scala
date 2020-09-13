package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLValue}
import de.unruh.isabelle.pure.Thm.Ops

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.MLValue.Implicits._
import de.unruh.isabelle.pure.Context.Implicits._
import de.unruh.isabelle.pure.Cterm.Implicits._
import de.unruh.isabelle.pure.Thm.Implicits.thmConverter

final class Thm private [Thm](val mlValue : MLValue[Thm])(implicit ec: ExecutionContext, isabelle: Isabelle) {
  override def toString: String = s"thm${mlValue.stateString}"
  lazy val cterm : Cterm = Cterm(Ops.cpropOf(mlValue))
  def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfThm(MLValue(ctxt, this)).retrieveNow
}

object Thm extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  protected[isabelle] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.compileFunction
    Term.init()
    isabelle.executeMLCodeNow("exception E_Thm of thm")
    val getThm: MLFunction2[Context, String, Thm] =
      compileFunction("fn (ctxt, name) => Proof_Context.get_thm ctxt name")
    val cpropOf: MLFunction[Thm, Cterm] =
      compileFunction[Thm, Cterm]("Thm.cprop_of")
    val stringOfThm: MLFunction2[Context, Thm, String] =
      compileFunction("fn (ctxt, thm) => Thm.pretty_thm ctxt thm |> Pretty.unformatted_string_of |> YXML.content_of")
  }

  def apply(context: Context, name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Thm = {
    val mlThm : MLValue[Thm] = Ops.getThm(MLValue((context, name)))
    new Thm(mlThm)
  }

  object ThmConverter extends Converter[Thm] {
    override def retrieve(value: MLValue[Thm])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Thm] =
      for (_ <- value.id)
        yield new Thm(mlValue = value)
    override def store(value: Thm)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Thm] =
      value.mlValue
    override val exnToValue: String = "fn E_Thm thm => thm"
    override val valueToExn: String = "E_Thm"
  }

  object Implicits {
    implicit val thmConverter: ThmConverter.type = ThmConverter
  }
}

