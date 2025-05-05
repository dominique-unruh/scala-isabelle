package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Symbols, Utils}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLValue}
import de.unruh.isabelle.pure.Thm.Ops
import org.jetbrains.annotations.ApiStatus.Experimental

import scala.concurrent.Future

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._


/** Represents a theorem (ML type `thm`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 */
final class Thm private [Thm](val mlValue : MLValue[Thm])
                             (implicit isabelle: Isabelle) extends FutureValue with PrettyPrintable {
  /** A string representation. Does not contain the actual proposition of the theorem, use [[pretty]]
   * for that. */
  override def toString: String = s"thm${mlValue.stateString}"

  /** Returns the proposition of this theorem (a term of Isabelle type `prop`). */
  lazy val proposition : Cterm = Cterm(Ops.cpropOf(mlValue))

  /** Returns the theory this theorem is part of. */
  def theoryOf: Theory = Ops.theoryOfThm(this).retrieveNow

  override def prettyRaw(ctxt: Context): String =
    Ops.stringOfThm(MLValue(ctxt, this)).retrieveNow

  /** Returns the proofterm of this theorem. */
  @Experimental
  def proofOf: Proofterm = Ops.proofOf(this).retrieveNow

  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
}

object Thm extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops()
  //noinspection TypeAnnotation
  protected[isabelle] class Ops(implicit val isabelle: Isabelle) {
    import MLValue.compileFunction
    //    Term.init()
    //    isabelle.executeMLCodeNow("exception E_Thm of thm")
    val theoryOfThm = compileFunction[Thm, Theory]("Thm.theory_of_thm")
    val getThm: MLFunction2[Context, String, Thm] =
      compileFunction("fn (ctxt, name) => Proof_Context.get_thm ctxt name")
    val cpropOf: MLFunction[Thm, Cterm] =
      compileFunction[Thm, Cterm]("Thm.cprop_of")
    val stringOfThm: MLFunction2[Context, Thm, String] =
      compileFunction("fn (ctxt, thm) => Thm.pretty_thm ctxt thm |> Pretty.unformatted_string_of |> YXML.parse_body |> XML.content_of")
    val proofOf = compileFunction[Thm, Proofterm]("Thm.proof_of")
//    val reconstructProofOf = compileFunction[Thm, Proofterm]("Thm.reconstruct_proof_of")
  }

  /** Retrieves a theorem from the Isabelle process by name. The theorem needs to be available in the given context.
   * Both short and fully qualified names work. (I.e., `Thm(context, "TrueI")` and `Thm(context, "HOL.TrueI)`
   * return the same theorem.)
   **/
  def apply(context: Context, name: String)(implicit isabelle: Isabelle): Thm = {
    val mlThm : MLValue[Thm] = Ops.getThm(MLValue((context, name)))
    new Thm(mlThm)
  }

  /** Representation of theorems in ML. (See the general discussion of [[Context]], the same things apply to [[Thm]].)
   *
   *  - ML type: `thm`
   *  - Representation of theorem `th` as an exception: `E_Thm th`
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   * */
  object ThmConverter extends Converter[Thm] {
    override def retrieve(value: MLValue[Thm])(implicit isabelle: Isabelle): Future[Thm] =
      Future.successful(new Thm(mlValue = value))
    override def store(value: Thm)(implicit isabelle: Isabelle): MLValue[Thm] =
      value.mlValue
    override def exnToValue(implicit isabelle: Isabelle): String = "fn E_Thm thm => thm"
    override def valueToExn(implicit isabelle: Isabelle): String = "E_Thm"

    override def mlType(implicit isabelle: Isabelle): String = "thm"
  }
}
