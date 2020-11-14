package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue
import scala.concurrent.ExecutionContext

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

// TODO: Support capturing types
// TODO: Add an interpolator for types
// TODO: Implement some caching
// TODO: Think about what user should import to get the implicit
// DOCUMENT
object TermInterpolator extends OperationCollection {
  implicit final class Interpolator(val stringContext: StringContext) {
    object term {
      private def template(prefix: String)
                          (implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext) = {
        val templateString = new StringBuilder
        var index = -1
        for (part <- stringContext.parts) {
          if (index >= 0) templateString ++= " ?" ++= prefix ++= index.toString ++= ".0" += ' '
          templateString ++= part
          index += 1
        }
        Term(Ops.setModePattern(context).retrieveNow, templateString.toString())
      }

      def apply(terms: Term*)
               (implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext): Cterm = {
        val prefix = s"VAR_${Utils.randomString()}_"
        val instantiation = for ((term, idx) <- terms.zipWithIndex)
          yield ((prefix + idx, 0), Cterm(context, term))
        Ops.inferInstantiateTerm(context, instantiation.toList, template(prefix)).retrieveNow
      }

      def unapplySeq(term: Term)
                    (implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext): Option[Seq[Term]] = {
        val prefix = s"VAR_${Utils.randomString()}_"
        val varnames = for (i <- 0 until stringContext.parts.length - 1) yield prefix + i.toString
        Ops.patternMatch(context, template(prefix), term, varnames.toList).retrieveNow
      }
    }
  }

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, executionContext: ExecutionContext) {
    val setModePattern = MLValue.compileFunction[Context, Context]("Proof_Context.set_mode Proof_Context.mode_pattern")
    val inferInstantiateTerm = MLValue.compileFunction[Context, List[((String, Int), Cterm)], Term, Cterm](
      """fn (ctxt, inst, term) => let
        |  val thm1 = infer_instantiate ctxt [(("x",0), Thm.cterm_of ctxt term)] reflexive_thm
        |  val thm2 = infer_instantiate ctxt inst thm1
        |  val term = Thm.rhs_of thm2
        |  in term end""".stripMargin)
    val patternMatch = MLValue.compileFunction[Context, Term, Term, List[String], Option[List[Term]]](
      """fn (ctxt,pattern,term,vars) => let
        |  val tenv = Pattern.match (Proof_Context.theory_of ctxt) (pattern,term)
        |                (Vartab.empty, Vartab.empty) |> snd
        |  val match = map (fn x => Vartab.lookup tenv (x,0) |> the |> snd) vars
        |  in SOME match end
        |  handle Pattern.MATCH => NONE""".stripMargin)
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}
