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

  /** Imports an ML structure from a theory into the global ML namespace.
   *
   * WARNING: This has a global effect on the Isabelle process because it modifies the ML name space.
   *
   * In an Isabelle theory `T`, it is possible to include ML source code using the `ML_file` command and related commands.
   * In that ML source code, new symbols (values, types, structures) can be declared. These will be visible
   * to further ML code in the same theory `T` and in theories that import `T`. However, it is not directly possible
   * to use those symbols in ML code on the ML toplevel (i.e., in commands such as [[control.Isabelle.executeMLCode Isabelle.executeMLCode]]
   * or [[mlvalue.MLValue.compileValue MLValue.compileValue]] and friends). Instead, the symbols must be imported using this method. (Only supported
   * for ML structures, not for values or types that are declared outside a structure.) [[importMLStructure]]`(name,newName)`
   * will import the structure called `name` under the new name `newName` into the toplevel.
   *
   * We give an example.
   *
   * File `test.ML`:
   * {{{
   *   structure Test = struct
   *   val num = 123
   *   end
   * }}}
   * This declares a structure `Test` with a value member `num`.
   *
   * File `TestThy.thy`:
   * {{{
   *   theory TestThy imports Main begin
   *
   *   ML_file "test.ML"
   *
   *   end
   * }}}
   * This declares a theory which loads the ML code from "test.ML" (and thus all theories importing `TestThy`
   * have access to the ML structure `Test`).
   *
   * In Scala:
   * {{{
   *   implicit val isabelle = new Isabelle(... suitable setup ...)
   *   val thy : Theory = Theory("Draft.TestThy")                  // load the theory TestThy
   *   val num1 : MLValue[Int] = MLValue.compileValue("Test.num")  // fails
   *   thy.importMLStructure("Test", "Test")                       // import the structure Test into the ML toplevel
   *   val num2 : MLValue[Int] = MLValue.compileValue("Test.num")  // access Test in compiled ML code
   *   println(num2.retrieveNow)                                   // ==> 123
   * }}}
   */
  // TODO: Check if the above example works. (test case)
  // TODO: Is there an alternative for this that does not affect the global namespace? (And then deprecate.)
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

  /** Retrieves a theory by its name. E.g., `Theory("HOL-Analysis.Inner_Product")`. **/
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
