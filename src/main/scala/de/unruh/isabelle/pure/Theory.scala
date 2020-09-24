package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{FutureValue, MLFunction, MLFunction3, MLValue}
import de.unruh.isabelle.pure.Theory.Ops

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._


/** Represents a theory (ML type `theory`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * The name of the theory can be retrieved via the member [[name]] if the theory was created
 * by [[Theory.apply]]`(ctxt, name)`. Otherwise, [[name]] returns a placeholder.
 */
final class Theory private [Theory](val name: String, val mlValue : MLValue[Theory]) extends FutureValue {
  override def toString: String = s"theory $name${mlValue.stateString}"
  // DOCUMENT
  def importMLStructure(name: String, newName: String)
                       (implicit isabelle: Isabelle, executionContext: ExecutionContext): Unit =
    Ops.importMLStructure(this, name, newName).retrieveNow

  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
}

object Theory extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  protected[isabelle] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.compileFunction
    MLValue.init()
    isabelle.executeMLCodeNow("exception E_Theory of theory")
    val loadTheory : MLFunction[String, Theory] =
      MLValue.compileFunction[String, Theory]("fn name => (Thy_Info.use_thy name; Thy_Info.get_theory name)")
    val importMLStructure : MLFunction3[Theory, String, String, Unit] = compileFunction(
      """fn (thy,theirName,hereStruct) => let
                  val theirAllStruct = Context.setmp_generic_context (SOME (Context.Theory thy))
                                       (#allStruct ML_Env.name_space) ()
                  val theirStruct = case List.find (fn (n,_) => n=theirName) theirAllStruct of
                           NONE => error ("Structure " ^ theirName ^ " not declared in given context")
        | SOME (_,s) => s
                  val _ = #enterStruct ML_Env.name_space (hereStruct, theirStruct)
                  in () end""")
  }

  /** Retrieves a theory by its name. E.g., `Theory("HOL-Analysis.Inner_Product")`.
   **/
  // TODO: Find out whether short names are possible.
  // TODO: Find out whether theories that aren't in the heap can be loaded
  // TODO: Make test case to find that out
  def apply(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = {
    val mlName = MLValue(name)
    val mlThy : MLValue[Theory] = Ops.loadTheory(mlName)
    new Theory(name, mlThy)
  }

  /** Representation of theories in ML. (See the general discussion of [[Context]], the same things apply to [[Theory]].)
   *
   *  - ML type: `theory`
   *  - Representation of theory `thy` as an exception: `E_Theory thy`
   *
   * (`E_Theory` is automatically declared when needed by the ML code in this package.
   * If you need to ensure that it is defined for compiling own ML code, invoke [[Theory.init]].)
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object TheoryConverter extends Converter[Theory] {
    override def retrieve(value: MLValue[Theory])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Theory] =
      for (_ <- value.id)
        yield new Theory(mlValue = value, name="‹theory›")
    override def store(value: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Theory] =
      value.mlValue
    override lazy val exnToValue: String = "fn E_Theory thy => thy"
    override lazy val valueToExn: String = "E_Theory"

    override def mlType: String = "theory"
  }
}
