package de.unruh.isabelle.pure

import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder}
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Utils}
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.pure.Context.Mode

import scala.annotation.compileTimeOnly
import scala.collection.mutable.ListBuffer
import scala.language.experimental.macros
import scala.reflect.macros.whitebox
import scala.util.Random

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

/** Provides string interpolators for conveniently creating Isabelle terms and types.
 *
 * It allows us to write, e.g., `term"x+y"` to parse the term `x+y`.
 * Or `typ"nat"` to parse the type `nat`
 *
 * In addition, subterms of terms can refer to already existing [[Term]] objects,
 * e.g., `term"x+\$term"` for a Scala variable `term` of type [[Term]].
 *
 * We can also refer to existing [[Typ]] objects, both in terms and types.
 * E.g., `term"x :: \$typ` or `typ"\$typ => \$typ"` for a Scala variable of type [[Typ]].
 *
 * The support for the interpolators is activated by importing the implicits `import de.unruh.isabelle.pure.Implicits._`.
 *
 * To use the interpolators, an implicit [[control.Isabelle Isabelle]] instance,
 * and an implicit [[Context]] must be given.
 * (The latter provides the theory context in which to parse the terms/types.)
 *
 * Parsing happens at runtime, there is no compile-time validation of the terms/types.
 *
 * An example how to use type and term interpolators:
 * {{{
 *   import de.unruh.isabelle.pure.Implicits._
 *   implicit isabelle = new Isabelle(...)
 *   implicit context = Context("Main") // Parsing w.r.t. Isabelle theory Main
 *   val typ1 = typ"nat"               // type nat
 *   val typ2 = typ"\$typ1 => \$typ1"    // type nat => nat
 *   val term1 = term"f :: \$typ2"      // term f :: nat => nat
 *   val term2 = term"\$term1 1"        // term (f :: nat => nat) (1 :: nat)  (by type inference)
 * }}}
 *
 * The interpolators can also be used for pattern matching.
 * That is, `term"..."` and `typ"..."` can appear in the pattern in a Scala pattern match.
 * Example:
 * {{{
 *   // implicits as above
 *   val term = term"1 + (2::nat)"
 *   term match {
 *     case term"\$t + (_::\$u::plus)" => (t,u)  // t : Term is "1::nat", u : Typ is "nat"
 *   }
 *   val typ = typ"nat => nat"
 *   typ match {
 *     case typ"\$t => \$dummy" => t    // t : Typ is "nat"
 *   }
 * }}}
 * (Note: A subtlety is that we write `\$u::plus`` instad of just `\$u` in the pattern above.
 * This is because Isabelle otherwise refuses to parse the term: a schematic type variable (into which `\$u` is converted for
 * parsing purposes) needs to be annotated with a sort that makes the overall term well-typed.)
 *
 * (Note: Another subtlety is that we use `\$dummy` instead of `_` to indicate a wildcard in `typ"\$t => \$dummy"`.
 * This is because Isabelle does not support `_` in pattern matches for types.
 * We could also write `$_` instead of `\$dummy` here because then `_` is a Scala-wildcard, not an Isabelle wildcard.)
 *
 * Invalid (i.e., unparseable) strings raise an [[de.unruh.isabelle.control.IsabelleMLException IsabelleException]].
 * This also applied if `...`` in patterns `term"..."` or `typ"..."` cannot be parsed.
 **/
object StringInterpolators extends OperationCollection {
  private case class Hole(varName: String, isTerm: Boolean)

  /** Best approximation to the union type of [[Term]] and [[Typ]] that we can easily manage. */
  type TermOrTyp = FutureValue with PrettyPrintable

  @compileTimeOnly("Macro implementation")
  private class CommonMacroImpl(protected val c: whitebox.Context) {
    import c.universe._

    protected def stringOfLiteral(literal: c.Tree): String = literal match {
      case Literal(Constant(string: String)) => string
    }
    protected val prefix = s"VAR_${Utils.randomString()}_"
    protected val parts: List[String] = c.prefix.tree match {
      case Select(Apply(_, List(Apply(_, parts))), _) =>
        parts.map(stringOfLiteral)
    }
    protected val uniqueId: Long = Random.nextLong()
  }

  @compileTimeOnly("Macro implementation for StringInterpolators.TermInterpolator.term")
  private final class TermMacroImpl(_c: whitebox.Context) extends CommonMacroImpl(_c) {
    import c.universe._

    private val (templateString, holes) = {
      val templateString = new StringBuilder
      val holes = new ListBuffer[Hole]
      var index = -1
      var nextHoleIsType = false
      for (part_ <- parts) {
        var part = part_
        if (index >= 0) {
          val isTerm =
            if (raw"%term\b.*".r.findFirstIn(part).nonEmpty) {
              part = part.stripPrefix("%term")
              true
            } else if (raw"%type\b.*".r.findFirstIn(part).nonEmpty) {
              part = part.stripPrefix("%type")
              false
            } else
              !nextHoleIsType

          val varName = (if (isTerm) "" else "'") + prefix + index.toString
          holes += Hole(varName = varName, isTerm = isTerm)
          templateString ++= " ?" ++= varName ++= ".0" += ' '
        }

        nextHoleIsType = raw".*(\b|\s)::\s*".r.findFirstIn(part).nonEmpty
        templateString ++= part
        index += 1
      }
      (templateString.toString, holes.toList)
    }

//    c.info(c.enclosingPosition, s"For StringContext ${parts.mkString("•")}, template is $templateString and holes are $holes", force = true)

    def termApplyImpl(args: c.Expr[Any]*)
                     (context: c.Expr[Context], isabelle: c.Expr[Isabelle]): c.Expr[Term] = {
      if (args.length != holes.length)
        c.abort(c.enclosingPosition, s"Expecting ${holes.length} arguments")

      val termInstantiations = for ((hole,term) <- holes.zip(args) if hole.isTerm)
        yield (hole.varName, q"$term : Term")
      val typInstantiations = for ((hole,typ) <- holes.zip(args) if !hole.isTerm)
        yield (hole.varName, q"$typ : Typ")

      c.Expr(
        q"""
          _root_.de.unruh.isabelle.pure.StringInterpolators.PrivateImplementation.termApplyImplRuntime(
             $uniqueId, $context, $templateString, List(..$typInstantiations), List(..$termInstantiations))($isabelle)
          """)
    }

    def termUnapplyImpl(term: c.Expr[Term])
                       (context: c.Expr[Context], isabelle: c.Expr[Isabelle]):
    c.Expr[Option[Product]] = {
      val returnType = tq"(..${holes.map(h => if (h.isTerm) tq"Term" else tq"Typ")})"
      val vars = for (h <- holes) yield (c.universe.TermName(c.freshName("v")), h)

      c.Expr(q"""
          new {
            import _root_.de.unruh.isabelle.pure._
            def unapply(term : Term) : Option[$returnType] = {
              val listOption = StringInterpolators.PrivateImplementation.termUnapplyImplRuntime($uniqueId, $context, $templateString,
                   List(..${holes collect { case h if !h.isTerm => h.varName }}),
                   List(..${holes collect { case h if h.isTerm => h.varName }}),
                   term)
                   ($isabelle)
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

  @compileTimeOnly("Macro implementation for StringInterpolators.TypInterpolator.typ")
  private final class TypMacroImpl(_c: whitebox.Context) extends CommonMacroImpl(_c) {
    import c.universe._

    private val (templateString, varNames) = {
      val templateString = new StringBuilder
      val varNames = new ListBuffer[String]
      var index = -1
      for (part <- parts) {
        if (index >= 0) {
          val varName = "'" + prefix + index.toString
          varNames += varName
          templateString ++= " ?" ++= varName ++= ".0" += ' '
        }

        templateString ++= part
        index += 1
      }
      (templateString.toString, varNames.toList)
    }

//    c.info(c.enclosingPosition, s"For StringContext ${parts.mkString("•")}, template is $templateString and holes are $varNames", force = true)

    def typApplyImpl(args: c.Expr[Typ]*)
                     (context: c.Expr[Context], isabelle: c.Expr[Isabelle]): c.Expr[Typ] = {
      if (args.length != varNames.length)
        c.abort(c.enclosingPosition, s"Expecting ${varNames.length} arguments")

      val typInstantiations = for ((varName,typ) <- varNames.zip(args))
        yield (varName, q"$typ : Typ")

      c.Expr(
        q"""
          _root_.de.unruh.isabelle.pure.StringInterpolators.PrivateImplementation.typApplyImplRuntime(
             $uniqueId, $context, $templateString, List(..$typInstantiations))($isabelle)
          """)
    }

    def typUnapplySeqImpl(typ: c.Expr[Typ])
                         (context: c.Expr[Context], isabelle: c.Expr[Isabelle]):
    c.Expr[Option[Seq[Typ]]] = c.Expr(q"""
        new _root_.de.unruh.isabelle.pure.StringInterpolators.PrivateImplementation.TypExtractorImplRuntime(
        $uniqueId, $context, $templateString, List(..$varNames)).unapplySeq($typ)
        """)
  }


  /** This object should be considered private. (It is only visible to be accessible in
   * macro code.) */
  object PrivateImplementation {
    private val termCache: Cache[(Long,Context), Term] = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).maximumSize(1000).build()
    private val typCache:  Cache[(Long,Context), Typ]  = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.SECONDS).maximumSize(1000).build()

    private def parseTerm(uniqueId: Long, context: Context, string: String)
                         (implicit isabelle: Isabelle): Term = {
      termCache.get((uniqueId,context), () => Term(context.setMode(Mode.pattern), string))
    }

    private def parseTyp(uniqueId: Long, context: Context, string: String)
                        (implicit isabelle: Isabelle) : Typ = {
      typCache.get((uniqueId,context), () => Typ(context.setMode(Mode.pattern), string))
    }

    /** This function should be considered private. (It is only visible to be accessible in
     * macro code.) */
    def termUnapplyImplRuntime(uniqueId: Long, context: Context, string: String, typVars: List[String], termVars: List[String], term: Term)
                              (implicit isabelle: Isabelle) : Option[(List[Typ], List[Term])] = {
      val template = parseTerm(uniqueId, context, string)
      Ops.patternMatch(context, template, term, typVars, termVars).retrieveNow
    }

    /** This class should be considered private. (It is only visible to be accessible in
     * macro code.) */
    final class TypExtractorImplRuntime(uniqueId: Long, context: Context, string: String, varNames: List[String])
                                       (implicit isabelle: Isabelle) {
      def unapplySeq(typ: Typ) : Option[Seq[Typ]] = {
        val template = parseTyp(uniqueId, context, string)
        Ops.patternMatchTyp(context, template, typ, varNames).retrieveNow
      }
    }

    /** This function should be considered private. (It is only visible to be accessible in
     * macro code.) */
    def termApplyImplRuntime(uniqueId: Long, context: Context, string: String, typeInstantiation: List[(String,Typ)], termInstantiation: List[(String,Term)])
                            (implicit isabelle: Isabelle): Cterm = {
      val template = parseTerm(uniqueId, context, string)
      val typeInstantiation2 = for ((v,typ) <- typeInstantiation) yield (v, Ctyp(context, typ))
      val termInstantiation2 = for ((v,term) <- termInstantiation) yield (v, Cterm(context, term))
      Ops.inferInstantiateTerm(context, typeInstantiation2, termInstantiation2, template).retrieveNow
    }

    /** This function should be considered private. (It is only visible to be accessible in
     * macro code.) */
    def typApplyImplRuntime(uniqueId: Long, context: Context, string: String, instantiation: List[(String,Typ)])
                           (implicit isabelle: Isabelle) : Typ = {
      val template = parseTyp(uniqueId, context, string)
      Ops.instantiateTyp(instantiation, template).retrieveNow
    }
  }

  /** See [[StringInterpolators]] for an explanation. */
  implicit final class TermInterpolator(val stringContext: StringContext) {
    object term {
      def apply(args: TermOrTyp*)(implicit context: Context, isabelle: Isabelle) : Term =
      macro TermMacroImpl.termApplyImpl

      def unapply(term: Term)
                 (implicit context: Context, isabelle: Isabelle): Option[Any] =
      macro TermMacroImpl.termUnapplyImpl
    }
  }

  /** See [[StringInterpolators]] for an explanation. */
  implicit final class TypInterpolator(val stringContext: StringContext) {
    object typ {
      def apply(args: Typ*)(implicit context: Context, isabelle: Isabelle) : Typ =
      macro TypMacroImpl.typApplyImpl

      def unapplySeq(typ: Typ)
                    (implicit context: Context, isabelle: Isabelle): Option[Seq[Typ]] =
      macro TypMacroImpl.typUnapplySeqImpl
    }
  }

  //noinspection TypeAnnotation
  protected final class Ops(implicit isabelle: Isabelle) {
    val inferInstantiateTerm = MLValue.compileFunction[Context, List[(String, Typ)], List[(String, Cterm)], Term, Cterm](
      """fn (ctxt, typInst, termInst, term) => let
        |  val term = Term.map_types (Term.map_atyps (fn v as TVar((n,0),_) =>
        |        (case AList.lookup (op=) typInst n of SOME T => T | NONE => v) | T => T)) term
        |  val termInst = map (fn (v,t) => ((v,0),t)) termInst
        |  val thm1 = infer_instantiate ctxt [(("x",0), Thm.cterm_of ctxt term)] reflexive_thm
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
    val instantiateTyp = MLValue.compileFunction[List[(String, Typ)], Typ, Typ](
      """fn (inst, typ) => Term.map_atyps (fn v as TVar((n,0),_) =>
        |        (case AList.lookup (op=) inst n of SOME T => T | NONE => v) | T => T) typ""".stripMargin)
    val patternMatchTyp = MLValue.compileFunction[Context, Typ, Typ, List[String], Option[List[Typ]]](
      """fn (ctxt,pattern,typ,vars) => let
        |  val tyenv = Sign.typ_match (Proof_Context.theory_of ctxt) (pattern,typ) Vartab.empty
        |  val match = map (fn x => Vartab.lookup tyenv (x,0) |> the |> snd) vars
        |  in SOME match end
        |  handle Type.TYPE_MATCH => NONE""".stripMargin
    )
  }

  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops
}
