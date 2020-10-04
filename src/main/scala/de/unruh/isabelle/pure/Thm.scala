package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{FutureValue, MLFunction, MLFunction2, MLValue}
import de.unruh.isabelle.pure.Thm.Ops

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._


/** Represents a theorem (ML type `thm`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 */
final class Thm private [Thm](val mlValue : MLValue[Thm])
                             (implicit ec: ExecutionContext, isabelle: Isabelle) extends FutureValue {
  /** A string representation. Does not contain the actual proposition of the theorem, use [[pretty]]
   * for that. */
  override def toString: String = s"thm${mlValue.stateString}"

  /** Returns the proposition of this theorem (a term of Isabelle type `prop`). */
  lazy val proposition : Cterm = Cterm(Ops.cpropOf(mlValue))

  /** Produces a string representation of this theorem.
   * Uses the Isabelle pretty printer.
   * @param ctxt The Isabelle proof context to use (this contains syntax declarations etc.) */
  def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfThm(MLValue(ctxt, this)).retrieveNow

  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
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

  /** Retrieves a theorem from the Isabelle process by name. The theorem needs to be available in the given context.
   * Both short and fully qualified names work. (I.e., `Thm(context, "TrueI")` and `Thm(context, "HOL.TrueI)`
   * return the same theorem.)
   **/
  def apply(context: Context, name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Thm = {
    val mlThm : MLValue[Thm] = Ops.getThm(MLValue((context, name)))
    new Thm(mlThm)
  }

  /** Representation of theorems in ML. (See the general discussion of [[Context]], the same things apply to [[Thm]].)
   *
   *  - ML type: `thm`
   *  - Representation of theorem `th` as an exception: `E_Thm th`
   *
   * (`E_Thm` is automatically declared when needed by the ML code in this package.
   * If you need to ensure that it is defined for compiling own ML code, invoke [[Thm.init]].)
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   * */
  object ThmConverter extends Converter[Thm] {
    override def retrieve(value: MLValue[Thm])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Thm] =
      for (_ <- value.id) // TODO: this is not needed by current convention (check also Context, ...)
        yield new Thm(mlValue = value)
    override def store(value: Thm)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Thm] =
      value.mlValue
    override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "fn E_Thm thm => thm"
    override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Thm"

    override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "thm"
  }
}
