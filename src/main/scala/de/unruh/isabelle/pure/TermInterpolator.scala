package de.unruh.isabelle.pure

import com.google.common.cache
import com.google.common.cache.{Cache, CacheBuilder, CacheLoader}
import com.google.common.collect.MapMaker
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Utils}
import de.unruh.isabelle.mlvalue.MLValue

import scala.annotation.compileTimeOnly
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.language.experimental.macros
import scala.reflect
import scala.reflect.macros.whitebox
import scala.reflect.runtime
import scala.util.Random

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

// TODO: Add an interpolator for types
// TODO: Think about what user should import to get the implicit
// DOCUMENT
object TermInterpolator extends OperationCollection {
  private case class Hole(varName: String, isTerm: Boolean)

  /** Best approximation to the union type of [[Term]] and [[Typ]] that we can easily manage. */
  type TermOrTyp = FutureValue with PrettyPrintable

  @compileTimeOnly("Macro implementation for TermInterpolator.Interpolator.term.apply")
  private class MacroImpl(val c: whitebox.Context) {
    import c.universe._

    private def stringOfLiteral(literal: c.Tree) = literal match {
      case Literal(Constant(string: String)) => string
    }

    private val prefix = s"VAR_${Utils.randomString()}_"

    private val parts: List[String] = c.prefix.tree match {
      case Select(Apply(_, List(Apply(_, parts))), _) =>
        parts.map(stringOfLiteral)
    }

    private val (templateString, holes) = {
      // TODO: buffer regexes
      val templateString = new StringBuilder
      val holes = new ListBuffer[Hole]
      var index = -1
      var nextHoleIsType = false
      for (part_ <- parts) {
        var part = part_
        if (index >= 0) {
          val isTerm =
            if (raw"%term\b.*".r.matches(part)) {
              part = part.stripPrefix("%term")
              true
            } else if (raw"%type\b.*".r.matches(part)) {
              part = part.stripPrefix("%type")
              false
            } else
              !nextHoleIsType

          val varName = (if (isTerm) "" else "'") + prefix + index.toString
          holes += Hole(varName = varName, isTerm = isTerm)
          templateString ++= " ?" ++= varName ++= ".0" += ' '
        }

        nextHoleIsType = raw".*(\b|\s)::\s*".r.matches(part)
        templateString ++= part
        index += 1
      }
      (templateString.toString, holes.toList)
    }

    c.info(c.enclosingPosition, s"For StringContext ${parts.mkString("•")}, template is $templateString and holes are $holes", force = true)

    def applyImpl(args: c.Expr[Any]*)
                 (context: c.Expr[Context], isabelle: c.Expr[Isabelle], executionContext: c.Expr[ExecutionContext]): c.Expr[Term] = {
      if (args.length != holes.length)
        c.abort(c.enclosingPosition, s"Expecting ${holes.length} arguments")

      val termInstantiations = for ((hole,term) <- holes.zip(args) if hole.isTerm)
        yield (hole.varName, q"$term : Term")
      val typInstantiations = for ((hole,typ) <- holes.zip(args) if !hole.isTerm)
        yield (hole.varName, q"$typ : Typ")

      val uniqueId = Random.nextLong()

      c.Expr(
        q"""
          _root_.de.unruh.isabelle.pure.TermInterpolator.Impl.applyImplRuntime(
             $uniqueId, $context, $templateString, List(..$typInstantiations), List(..$termInstantiations))($isabelle,$executionContext)
          """)
    }

    def unapplyImpl(term: c.Expr[Term])
                      (context: c.Expr[Context], isabelle: c.Expr[Isabelle], executionContext: c.Expr[ExecutionContext]):
    c.Expr[Option[Product]] = {
      val returnType = tq"(..${holes.map(h => if (h.isTerm) tq"Term" else tq"Typ")})"
      val vars = for (h <- holes) yield (c.universe.TermName(c.freshName("v")), h)

      val uniqueId = Random.nextLong()

      c.Expr(q"""
          new {
            import _root_.de.unruh.isabelle.pure._
            def unapply(term : Term) : Option[$returnType] = {
              val listOption = TermInterpolator.Impl.unapplyImplRuntime($uniqueId, $context, $templateString,
                   List(..${holes collect { case h if !h.isTerm => h.varName }}),
                   List(..${holes collect { case h if h.isTerm => h.varName }}),
                   term)
                   ($isabelle,$executionContext)
              listOption match {
                case None => None
                case Some((List(..${vars collect { case (v,h) if !h.isTerm => pq"$v" }}),
                           List(..${vars collect { case (v,h) if h.isTerm => pq"$v" }}))) => Some((..${vars.map(_._1)}))
                case _ => throw new AssertionError("Unexpected result in macro implementation of term-interpolation: " + listOption)
              }
            }
          }.unapply($term)
          """)
    }
  }


  /** This object should be considered private. (It is only visible to be accessible in
   * macro code.) */
  object Impl {
    private val cache: Cache[Long, (Context,Term)] = CacheBuilder.newBuilder().weakValues().build[Long, (Context,Term)]()

    private def parseTerm(uniqueId: Long, context: Context, string: String)
                         (implicit isabelle: Isabelle, executionContext: ExecutionContext): Term = {
      def parse() = Term(Ops.setModePattern(context).retrieveNow, string)

      val (prevContext, term) = cache.get(uniqueId, { () => (context, parse()) })

      if (prevContext ne context) {
        val term2 = parse()
        cache.put(uniqueId, (context, term2))
        term2
      } else
        term
    }

    def unapplyImplRuntime(uniqueId: Long, context: Context, string: String, typVars: List[String], termVars: List[String], term: Term)
                          (implicit isabelle: Isabelle, executionContext: ExecutionContext) : Option[(List[Typ], List[Term])] = {
      val template = parseTerm(uniqueId, context, string)
      Ops.patternMatch(context, template, term, typVars, termVars).retrieveNow
    }

    def applyImplRuntime(uniqueId: Long, context: Context, string: String, typeInstantiation: List[(String,Typ)], termInstantiation: List[(String,Term)])
                        (implicit isabelle: Isabelle, executionContext: ExecutionContext): Cterm = {
      val template = parseTerm(uniqueId, context, string)
      val typeInstantiation2 = for ((v,typ) <- typeInstantiation) yield ((v,0), Ctyp(context, typ))
      val termInstantiation2 = for ((v,term) <- termInstantiation) yield ((v,0), Cterm(context, term))
//      val instantiation = for ((varname, term) <- vars.zip(terms))
//        yield ((varname, 0), Cterm(context, term))
      Ops.inferInstantiateTerm(context, typeInstantiation2, termInstantiation2, template).retrieveNow
    }
  }

  implicit final class Interpolator(val stringContext: StringContext) {
    object term {
      def apply(args: TermOrTyp*)(implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext) : Term =
        macro MacroImpl.applyImpl

      def unapply(term: Term)
                 (implicit context: Context, isabelle: Isabelle, executionContext: ExecutionContext): Option[Any] =
        macro MacroImpl.unapplyImpl
    }
  }

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle, executionContext: ExecutionContext) {
    val setModePattern = MLValue.compileFunction[Context, Context]("Proof_Context.set_mode Proof_Context.mode_pattern")
    val inferInstantiateTerm = MLValue.compileFunction[Context, List[((String, Int), Typ)], List[((String, Int), Cterm)], Term, Cterm](
      """fn (ctxt, typInst, termInst, term) => let
        |  val term2 = Term.map_types (Term.map_atyps (fn v as TVar(ni,_) =>
        |        (case AList.lookup (op=) typInst ni of SOME T => T | NONE => v) | T => T)) term
        |  val thm1 = infer_instantiate ctxt [(("x",0), Thm.cterm_of ctxt term2)] reflexive_thm
        |  val thm2 = infer_instantiate ctxt termInst thm1
        |  val term = Thm.rhs_of thm2
        |  in term end
        |""".stripMargin)
    val patternMatch = MLValue.compileFunction[Context, Term, Term, List[String], List[String], Option[(List[Typ],List[Term])]](
      """fn (ctxt,pattern,term,typVars, termVars) => let
        |  val (tyenv,tenv) = Pattern.match (Proof_Context.theory_of ctxt) (pattern,term)
        |                (Vartab.empty, Vartab.empty)
        |  val typMatch = map (fn x => Vartab.lookup tyenv (x,0) |> the |> snd) typVars
        |  val termMatch = map (fn x => Vartab.lookup tenv (x,0) |> the |> snd) termVars
        |  in SOME (typMatch,termMatch) end
        |  handle Pattern.MATCH => NONE""".stripMargin)
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}
