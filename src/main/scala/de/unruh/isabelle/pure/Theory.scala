package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction3, MLValue}
import de.unruh.isabelle.pure.Theory.Ops

import scala.concurrent.{ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.MLValue.Implicits._
import de.unruh.isabelle.pure.Theory.Implicits.theoryConverter

final class Theory private [Theory](val name: String, val mlValue : MLValue[Theory]) {
  override def toString: String = s"theory $name${mlValue.stateString}"
  def importMLStructure(name: String, newName: String)
                       (implicit isabelle: Isabelle, executionContext: ExecutionContext): Unit =
    Ops.importMLStructure(this, name, newName).retrieveNow
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

  def apply(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = {
    val mlName = MLValue(name)
    val mlThy : MLValue[Theory] = Ops.loadTheory(mlName)
    new Theory(name, mlThy)
  }


  object TheoryConverter extends Converter[Theory] {
    override def retrieve(value: MLValue[Theory])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Theory] =
      for (_ <- value.id)
        yield new Theory(mlValue = value, name="‹theory›")
    override def store(value: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Theory] =
      value.mlValue
    override lazy val exnToValue: String = "fn E_Theory thy => thy"
    override lazy val valueToExn: String = "E_Theory"
  }

  object Implicits {
    implicit val theoryConverter: TheoryConverter.type = TheoryConverter
  }
}
