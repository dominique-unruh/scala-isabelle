package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue

import scala.annotation.compileTimeOnly
import scala.concurrent.ExecutionContext
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

// TODO: Support capturing types
// TODO: Add an interpolator for types
// TODO: Implement some caching
// TODO: Think about what user should import to get the implicit
// DOCUMENT
object TermInterpolator extends OperationCollection {
  // TODO hide
  @compileTimeOnly("Macro implementation for TermInterpolator.Interpolator.term.apply")
  private class MacroImpl(val c: whitebox.Context) {

    import c.universe._

    private def stringOfLiteral(literal: c.Tree) = literal match {
      case Literal(Constant(string: String)) => string
    }

    val prefix = s"VAR_${Utils.randomString()}_"

    val parts: List[String] = c.prefix.tree match {
      case Select(Apply(_, List(Apply(_, parts))), _) =>
        parts.map(stringOfLiteral)
    }

    val templateString: String = {
      val templateString = new StringBuilder
      var index = -1
      for (part <- parts) {
        if (index >= 0) templateString ++= " ?" ++= prefix ++= index.toString ++= ".0" += ' '
        templateString ++= part
        index += 1
      }
      templateString.toString
    }

    val vars : Seq[String] = for (i <- 0 until parts.length - 1)
      yield prefix + i.toString

    def applyImpl(terms: c.Expr[Term]*)
                 (context: c.Expr[Context], isabelle: c.Expr[Isabelle], executionContext: c.Expr[ExecutionContext]): c.Expr[Term] = {
      c.Expr(
        q"""
          _root_.de.unruh.isabelle.pure.TermInterpolator.Impl.applyImplRuntime($context,$templateString,List(..$vars),List(..$terms))($isabelle,$executionContext)
          """)
    }

    def unapplySeqImpl(term: c.Expr[Term])
                      (context: c.Expr[Context], isabelle: c.Expr[Isabelle], executionContext: c.Expr[ExecutionContext]):
      c.Expr[Option[Seq[Term]]] = {
      c.Expr(q"""
          {
            new _root_.de.unruh.isabelle.pure.TermInterpolator.Impl.unapplySeqImplRuntime($context,$templateString,List(..$vars))($isabelle,$executionContext)
          }.unapplySeq($term)
          """)
    }
  }

  /** This object should be considered private. (It is only visible to be accessible in
   * macro code.) */
  object Impl {

    class unapplySeqImplRuntime(context: Context, string: String, vars: List[String])
                               (implicit isabelle: Isabelle, executionContext: ExecutionContext) {
      def unapplySeq(term: Term): Option[List[Term]] = {
        val template = Term(Ops.setModePattern(context).retrieveNow, string)
        Ops.patternMatch(context, template, term, vars).retrieveNow
      }
    }

    def applyImplRuntime(context: Context, string: String, vars: List[String], terms: List[Term])
                        (implicit isabelle: Isabelle, executionContext: ExecutionContext): Cterm = {
      val template = Term(Ops.setModePattern(context).retrieveNow, string)
      val instantiation = for ((varname, term) <- vars.zip(terms))
        yield ((varname, 0), Cterm(context, term))
      Ops.inferInstantiateTerm(context, instantiation.toList, template).retrieveNow
    }
  }

  implicit final class Interpolator(val stringContext: StringContext) {
    object term {
      def apply(terms: Term*)(implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext) : Term =
        macro MacroImpl.applyImpl

      def unapplySeq(term: Term)
                    (implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext): Option[Seq[Term]] =
        macro MacroImpl.unapplySeqImpl
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
