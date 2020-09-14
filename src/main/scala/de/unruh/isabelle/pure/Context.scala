package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLValue}

import scala.concurrent.{ExecutionContext, Future}

final class Context private [Context](val mlValue : MLValue[Context]) {
  override def toString: String = "context" + mlValue.stateString
}

object Context extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  protected[isabelle] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.compileFunctionRaw
    Theory.init()
    isabelle.executeMLCodeNow("exception E_Context of Proof.context")
    val contextFromTheory : MLFunction[Theory, Context] =
      compileFunctionRaw[Theory, Context]("fn (E_Theory thy) => Proof_Context.init_global thy |> E_Context")
  }

  def apply(theory: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): Context = {
    val mlCtxt : MLValue[Context] = Ops.contextFromTheory(theory.mlValue)
    new Context(mlCtxt)
  }

  def apply(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext) : Context =
    Context(Theory(name))

  object ContextConverter extends Converter[Context] {
    override def retrieve(value: MLValue[Context])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Context] = {
      for (_ <- value.id)
        yield new Context(mlValue = value)
    }

    override def store(value: Context)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Context] =
      value.mlValue
    override lazy val exnToValue: String = "fn E_Context ctxt => ctxt"
    override lazy val valueToExn: String = "E_Context"
  }

  object Implicits {
    implicit val contextConverter: ContextConverter.type = ContextConverter
  }

}
