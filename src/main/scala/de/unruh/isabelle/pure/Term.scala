package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString}
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Symbols, Utils}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLRetrieveFunction, MLValue}
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.pure.Term.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}
import scala.util.control.Breaks

/**
 * This class represents a term (ML type `term`) in Isabelle. It can be transferred to and from the Isabelle process
 * transparently by internally using [[mlvalue.MLValue MLValue]]s (see below).
 *
 * In most respects, [[Term]] behaves as if it was an algebraic datatype defined as follows:
 * {{{
 *   sealed abstract class Term
 *   final case class Const(name: String, typ: Typ)            // Corresponds to ML constructor 'Const'
 *   final case class Free(name: String, typ: Typ)             // Corresponds to ML constructor 'Free'
 *   final case class Var(name: String, index: Int, typ: Typ)  // Corresponds to ML constructor 'Var'
 *   final case class Abs(name: String, typ: Typ, body: Term)  // Corresponds to ML constructor 'Abs'
 *   final case class Bound private (index: Int)               // Corresponds to ML constructor 'Bound'
 *   final case class App private (fun: Term, arg: Term)       // Corresponds to ML constructor '$'
 * }}}
 *
 * tl;dr for the explanation below: Terms can be treated as if they were the case classes above (even though
 * there actually are more classes), both in object
 * creation and in pattern matching, with the exception that
 * one should not use type patterns (e.g., `case _ : Const =>`).
 *
 * Having [[Term]]s defined in terms of those case classes would mean that when retrieving a term from the Isabelle process, the whole term
 * needs to be retrieved. Since terms can be very large and might be transferred back and forth a lot
 * (with minor modifications), we choose an approach where terms may be partially stored in Scala, and partially in
 * the Isabelle process. That is, an instance of [[Term]] can be any of the above classes, or a reference to a term
 * in the object store in the Isabelle process, or both at the same time. And the same applies to subterms, too.
 * So for example, if we retrieve terms `t`,`u` from Isabelle to Scala, and then in Scala construct `App(t,u)`,
 * and then transfer `App(t,u)` back to Isabelle, the terms `t`,`u` will never be serialized, and only the
 * constructor `App` will need to be transferred.
 *
 * In order to faciliate this, the classes [[Const]], [[Free]], [[Var]], [[Abs]], [[Bound]], [[App]]
 * (collectively referred to as a [[ConcreteTerm]]) additionally
 * store an reference [[Term.mlValue]] to the Isabelle object store (this reference is initialized lazily, thus
 * accessing it can force the term to be transferred to the Isabelle process). And furthermore, there is an additional
 * subclass [[MLValueTerm]] of [[Term]] that represents a term that is stored in Isabelle but not available in
 * Scala at class creation time. Instances of [[MLValueTerm]] never need to be created manually, though. You
 * just have to be aware that some terms might not be instances of the six [[ConcreteTerm]] "case classes".
 * (But if a [[ConcreteTerm]] is required, any term can be converted using `term.`[[Term.concrete concrete]].)
 *
 * Pattern matching works as expected, that is, when an [[MLValueTerm]] `t`, e.g., refers to an Isabelle term of the form
 * `Const (name,typ)`, then `t` will match the pattern `case Const(name,typ) =>`. (Necessary information will be
 * transferred from the Isabelle process on demand.) Because of this, one can almost
 * completely ignore the existence of [[MLValueTerm]]. The only caveat is that one should not do a pattern match on the
 * type of the term. That is `case _ : Const =>` will not match a term `Const(name,typ)` represented by an [[MLValueTerm]].
 *
 * Two terms are equal (w.r.t.~the `equals` method) iff they represent the same Isabelle terms. I.e.,
 * an [[MLValueTerm]] and a [[Const]] can be equal. (Equality tests try to transfer as little data as possible when
 * determining equality.)
 *
 * Furthermore, there is a subclass [[Cterm]] of [[Term]] that represents an ML value of type `cterm`. This is
 * logically a term that is certified to be well-typed with respect to some Isabelle context.
 * In this implementation, a [[Cterm]] is also a [[Term]], so it is possible to do, e.g., equality tests between
 * [[Cterm]]s and regular terms (such as [[Const]]) without explicit conversions. Similarly, patterns such as
 * `case Const(name,typ) =>` also match [[Cterm]]s.
 */
sealed abstract class Term extends FutureValue with PrettyPrintable {
  /** Transforms this term into an [[mlvalue.MLValue MLValue]] containing this term. This causes transfer of
   * the term to Isabelle only the first time it is accessed (and not at all if the term
   * came from the Isabelle process in the first place). */
  val mlValue : MLValue[Term]
  /** [[control.Isabelle Isabelle]] instance relative to which this term was constructed. */
  implicit val isabelle : Isabelle

  override def prettyRaw(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfTerm(MLValue((ctxt, this))).retrieveNow

  /** Transforms this term into a [[ConcreteTerm]]. A [[ConcreteTerm]] guarantees
   * that the type of the term ([[App]],[[Const]],[[Abs]]...) corresponds to the top-level
   * constructor on Isabelle side (`$`, `Const`, `Abs`, ...). */
  val concrete : ConcreteTerm

  /** Transforms this term into a [[ConcreteTerm]] (see [[concrete]]).
   * In contrast to [[concrete]], it also replaces all subterms by concrete subterms. */
  def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext) : ConcreteTerm

  /** Indicates whether [[concrete]] has already been initialized. (I.e.,
   * whether it can be accessed without delay and without incurring communication with
   * the Isabelle process. */
  def concreteComputed: Boolean

  /** `t $ u` is shorthand for [[App]]`(t,u)` */
  def $(that: Term)(implicit ec: ExecutionContext): App = App(this, that)

  /** Hash code compatible with [[equals]]. May fail with an exception, see [[equals]]. */
  override def hashCode(): Int = throw new NotImplementedError("Should be overridden")

  /** Equality of terms. Returns true iff the two [[Term]] instances represent the same term in
   * the Isabelle process. (E.g., a [[Cterm]] and a [[Const]] can be equal.) May throw an exception
   * if the computation of the terms fails. (But will not fail if [[await]] or a related [[mlvalue.FutureValue FutureValue]] method has
   * returned successfully on both terms.)
   */
  override def equals(that: Any): Boolean = (this, that) match {
    case (t1, t2: AnyRef) if t1 eq t2 => true
    case (t1: App, t2: App) => t1.fun == t2.fun && t1.arg == t2.arg
    case (t1: Bound, t2: Bound) => t1.index == t2.index
    case (t1: Var, t2: Var) => t1.name == t2.name && t1.index == t2.index && t1.typ == t2.typ
    case (t1: Free, t2: Free) => t1.name == t2.name && t1.typ == t2.typ
    case (t1: Const, t2: Const) => t1.name == t2.name && t1.typ == t2.typ
    case (t1: Abs, t2: Abs) => t1.name == t2.name && t1.typ == t2.typ && t1.body == t2.body
    case (t1: Cterm, t2: Cterm) =>
      if (Await.result(t1.ctermMlValue.id, Duration.Inf) == Await.result(t2.ctermMlValue.id, Duration.Inf)) true
      else t1.mlValueTerm == t2.mlValueTerm
    case (t1: Cterm, t2: Term) => t1.mlValueTerm == t2
    case (t1: Term, t2: Cterm) => t1 == t2.mlValueTerm
    case (t1: MLValueTerm, t2: MLValueTerm) =>
      import ExecutionContext.Implicits.global
      if (Await.result(t1.mlValue.id, Duration.Inf) == Await.result(t2.mlValue.id, Duration.Inf)) true
      else if (t1.concreteComputed && t2.concreteComputed) t1.concrete == t2.concrete
      else Ops.equalsTerm(t1,t2).retrieveNow
    case (t1: MLValueTerm, t2: Term) =>
      import ExecutionContext.Implicits.global
      if (t1.concreteComputed) t1.concrete == t2
      else Ops.equalsTerm(t1,t2).retrieveNow
    case (t1: Term, t2: MLValueTerm) =>
      import ExecutionContext.Implicits.global
      if (t2.concreteComputed) t1 == t2.concrete
      else Ops.equalsTerm(t1,t2).retrieveNow
    case _ => false
  }

  /** Produces a string representation of this term.
   *
   * This is not a "pretty" representation, it does not use Isabelle syntax, and subterms that are stored only
   * in the Isabelle process are replaced with
   * a placeholder (thus this method does not invoke any potentially communication with the Isabelle process).
   *
   * @see pretty for pretty printed terms
   **/
  override def toString: String

  /** Returns the type of this term, assuming the term is well-typed.
   * (The function does not verify whether the term is indeed well-typed.
   * If it is not, no guarantee is made what type is returned.)
   *
   * This method is analogous to `fastype_of` in Isabelle/ML but avoids transferring the term to/from Isabelle when
   * determining the type.
   * */
  def fastType(implicit executionContext: ExecutionContext) : Typ = {
    import Breaks._
    tryBreakable {
      def typ(t: Term, env: List[Typ]): Typ = t match {
        case Free(_, t) => t
        case Const(_, t) => t
        case Var(_, _, t) => t
        case Abs(_, t, body) => t -->: typ(body, t::env)
        case Bound(i) => env(i)
        case App(f,u) =>
          val fType = typ(f, env)
          if (!fType.concreteComputed) break()
          val Type("fun", _, t) = fType
          t
        case cterm: Cterm =>
          if (cterm.concreteComputed) typ(cterm.concrete, env)
          else break()
        case term: MLValueTerm =>
          if (term.concreteComputed) typ(term.concrete, env)
          else break()
      }
      typ(this, Nil)
    } catchBreak {
      Ops.fastypeOf(this).retrieveNow
    }
  }
}

/** Base class for all concrete terms.
 * A [[ConcreteTerm]] guarantees
 * that the type of the term ([[App]],[[Const]],[[Abs]]...) corresponds to the top-level
 * constructor on Isabelle side (`$`, `Const`, `Abs`, ...).
 */
sealed abstract class ConcreteTerm extends Term {
  /** @return this */
  @inline override val concrete: this.type = this
  /** @return true */
  @inline override def concreteComputed: Boolean = true
}

/** Represents a `cterm` in Isabelle. In Isabelle, a `cterm` must be explicitly converted into a `term`.
 * In contrast, this class inherits from [[Term]], so no explicit conversions are needed. (They happen automatically on
 * demand.)
 * A [[Cterm]] is always well-typed relative to the context for which it was
 * created (this is ensured by the Isabelle trusted core).
 **/
final class Cterm private(val ctermMlValue: MLValue[Cterm])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  /** Returns this term as an `MLValue[Term]` (not `MLValue[Cterm]`). The difference is crucial
   * because `MLValue[_]` is not covariant. So for invoking ML functions that expect an argument of type `term`, you
   * need to get an `MLValue[Term]`. In contrast, [[ctermMlValue]] returns this term as an `MLValue[Cterm]`. */
  override lazy val mlValue: MLValue[Term] = {
    val result = Ops.termOfCterm(ctermMlValue)
    mlValueLoaded = true
    result }
  private var mlValueLoaded = false
  /** Transforms this [[Cterm]] into an [[MLValueTerm]]. */
  private [pure] def mlValueTerm = new MLValueTerm(mlValue)
  override def prettyRaw(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfCterm(MLValue((ctxt, this))).retrieveNow
  lazy val concrete: ConcreteTerm = mlValueTerm.concrete
  def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): ConcreteTerm = mlValueTerm.concreteRecursive
  override def hashCode(): Int = concrete.hashCode()
  override def force : this.type = { ctermMlValue.force; this }
  override def someFuture: Future[Any] = ctermMlValue.someFuture
  override def await: Unit = ctermMlValue.await

  override def toString: String =
    if (mlValueLoaded) "cterm:"+mlValue.toString
    else "cterm"+stateString

  override def concreteComputed: Boolean =
    if (mlValueLoaded) mlValueTerm.concreteComputed
    else false
}

object Cterm {
  /** Creates a [[Cterm]] from an [[mlvalue.MLValue MLValue]][[[Cterm]]]. Since a [[Cterm]]
   * is just a wrapper around an [[mlvalue.MLValue MLValue]][[[Cterm]]], this operation does not
   * require any communication with the Isabelle process. */
  def apply(mlValue: MLValue[Cterm])
           (implicit isabelle: Isabelle, executionContext: ExecutionContext) =
    new Cterm(mlValue)

  /** Converts a [[Term]] into a [[Cterm]]. This involves type-checking (relative to the
   * context `ctxt`). The resulting [[Cterm]] is then certified to be correctly typed. */
  // TODO: This is problematic: if term is already a Cterm, but for a different context, we break context discipline
  def apply(ctxt: Context, term: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) : Cterm = term match {
    case cterm : Cterm => cterm
    case term => new Cterm(Ops.ctermOfTerm(MLValue((ctxt, term))))
  }

  /** Representation of cterms in ML.
   *
   *  - ML type: `cterm`
   *  - Representation of term `t` as an exception: `E_Cterm t`
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object CtermConverter extends Converter[Cterm] {
    override def store(value: Cterm)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Cterm] =
      value.ctermMlValue
    override def retrieve(value: MLValue[Cterm])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Cterm] =
        Future.successful(new Cterm(ctermMlValue = value))
    override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "fn (E_Cterm t) => t"
    override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Cterm"

    override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "cterm"
  }
}

/** A [[Term]] that is stored in the Isabelle process's object store
 * and may or may not be known in Scala. Use [[concrete]] to
 * get a representation of the same term as a [[ConcreteTerm]]. */
final class MLValueTerm(val mlValue: MLValue[Term])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  override def someFuture: Future[Any] = mlValue.someFuture
  override def await: Unit = mlValue.await

  //noinspection EmptyParenMethodAccessedAsParameterless
  override def hashCode(): Int = concrete.hashCode

  def concreteComputed: Boolean = concreteLoaded

  @volatile private var concreteLoaded = false

  override lazy val concrete : ConcreteTerm = {
    val DList(DInt(constructor), data @_*) = Await.result(Ops.destTerm(mlValue), Duration.Inf)
    val term = (constructor,data) match {
      case (1,List(DString(name), DObject(typ))) => // Const
        new Const(name, MLValue.unsafeFromId[Typ](typ).retrieveNow, mlValue)
      case (2,List(DString(name), DObject(typ))) => // Free
        new Free(name, MLValue.unsafeFromId[Typ](typ).retrieveNow, mlValue)
      case (3,List(DString(name), DInt(index), DObject(typ))) =>
        new Var(name, index.toInt, MLValue.unsafeFromId[Typ](typ).retrieveNow, mlValue)
      case (4,List(DInt(index))) =>
        new Bound(index.toInt, mlValue)
      case (5,List(DString(name),DObject(typ), DObject(body))) =>
        new Abs(name, MLValue.unsafeFromId[Typ](typ).retrieveNow,
          MLValue.unsafeFromId[Term](body).retrieveNow,
          mlValue)
      case (6,List(DObject(t1), DObject(t2))) =>
        new App(MLValue.unsafeFromId[Term](t1).retrieveNow,
          MLValue.unsafeFromId[Term](t2).retrieveNow,
          mlValue)
    }
    concreteLoaded = true
    term
  }

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): ConcreteTerm = concrete.concreteRecursive

  override def toString: String =
    if (concreteLoaded) concrete.toString
    else s"‹term${mlValue.stateString}›"

}

/** A constant (ML constructor `Const`). [[name]] is the fully qualified name of the constant (e.g.,
 * `"HOL.True"`) and [[typ]] its type. */
final class Const private[pure](val name: String, val typ: Typ, initialMlValue: MLValue[Term]=null)
                               (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  override lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeConst(MLValue((name,typ)))
  override def toString: String = name

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): Const = {
    val typ = this.typ.concreteRecursive
    if (typ eq this.typ)
      this
    else
      new Const(name, typ, initialMlValue)
  }

  override def hashCode(): Int = new HashCodeBuilder(162389433,568734237)
    .append(name).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit ec: ExecutionContext): Future[this.type] = Future.successful(this)
}

object Const {
  /** Create a constant with name `name` and type `typ`. */
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Const(name, typ)

  /** Allows to pattern match constants. E.g.,
   * {{{
   *   term match {
   *     case Const(name,typ) => println(s"Constant \$name found")
   *   }
   * }}}
   * Note that this will also match a [[Cterm]] and an [[MLValueTerm]] that represent a `Const` in ML.
   **/
  @tailrec
  def unapply(term: Term): Option[(String, Typ)] = term match {
    case const : Const => Some((const.name,const.typ))
    case _ : MLValueTerm | _ : Cterm => unapply(term.concrete)
    case _ => None
  }
}

/** A free variable (ML constructor `Free`). [[name]] is the name of the variable (e.g.,
 * `"x"`) and [[typ]] its type. */
final class Free private[pure](val name: String, val typ: Typ, initialMlValue: MLValue[Term]=null)
                              (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  override lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeFree(name, typ)
  override def toString: String = name

  override def hashCode(): Int = new HashCodeBuilder(384673423,678423475)
    .append(name).toHashCode

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): Free = {
    val typ = this.typ.concreteRecursive
    if (typ eq this.typ)
      this
    else
      new Free(name, typ, initialMlValue)
  }

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit ec: ExecutionContext): Future[this.type] = Future.successful(this)
}

object Free {
  /** Create a free variable with name `name` and type `typ`. */
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Free(name, typ)

  /** Allows to pattern match free variables. E.g.,
   * {{{
   *   term match {
   *     case Free(name,typ) => println(s"Free variable \$name found")
   *   }
   * }}}
   * Note that this will also match a [[Cterm]] and an [[MLValueTerm]] that represent a `Free` in ML.
   **/
  @tailrec
  def unapply(term: Term): Option[(String, Typ)] = term match {
    case free : Free => Some((free.name,free.typ))
    case _ : MLValueTerm | _ : Cterm => unapply(term.concrete)
    case _ => None
  }
}

/** A schematic variable (ML constructor `Var`). [[name]] is the name of the variable (e.g.,
 * `"x"`), [[index]] its index, and [[typ]] its type.
 *
 * Schematic variables are the ones that are represented with a leading question mark in
 * Isabelle's parsing and pretty printing. E.g., `?x` is a [[Var]] with [[name]]`="x"`
 * and [[index]]`=0`. And `?y1` or `?y.1` is a [[Var]] with [[name]]`="y"` and [[index]]`=1`.
 *
 * By convention, schematic variables indicate variables that are can be instantiated/unified.
 **/
final class Var private[pure](val name: String, val index: Int, val typ: Typ, initialMlValue: MLValue[Term]=null)
                       (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  override lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeVar(name, index, typ)
  override def toString: String = s"?$name$index"

  override def hashCode(): Int = new HashCodeBuilder(3474285, 342683425)
    .append(name).append(index).toHashCode

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): Var = {
    val typ = this.typ.concreteRecursive
    if (typ eq this.typ)
      this
    else
      new Var(name, index, typ, initialMlValue)
  }

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit ec: ExecutionContext): Future[this.type] = Future.successful(this)
}

object Var {
  /** Create a schematic variable with name `name`, index `index`, and type `typ`. */
  def apply(name: String, index: Int, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Var(name, index, typ)

  /** Allows to pattern match schematic variables. E.g.,
   * {{{
   *   term match {
   *     case Var(name,index,typ) => println(s"Schematic variable \$name.\$index found")
   *   }
   * }}}
   * Note that this will also match a [[Cterm]] and an [[MLValueTerm]] that represent a `Var` in ML.
   **/
  @tailrec
  def unapply(term: Term): Option[(String, Int, Typ)] = term match {
    case v : Var => Some((v.name,v.index,v.typ))
    case _ : MLValueTerm | _ : Cterm => unapply(term.concrete)
    case _ => None
  }
}

/** A function application (ML constructor `$`). [[fun]] is the function to be applied and [[arg]] its
 * argument. (E.g., `t1 $ t2` in ML would have [[fun]]=t1 and [[arg]]=t2.)
 *
 * Can be constructed both as `App(t1,t2)` or `t1 $ t2` in Isabelle.
 * (Pattern matching only supports the syntax `App(...)`, not `$`.)
 **/
final class App private[pure] (val fun: Term, val arg: Term, initialMlValue: MLValue[Term]=null)
                              (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  override lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeApp(fun,arg)

  override def toString: String = s"($fun $$ $arg)"

  override def hashCode(): Int = new HashCodeBuilder(334234237,465634533)
    .append(arg).toHashCode

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): App = {
    val fun = this.fun.concreteRecursive
    val arg = this.arg.concreteRecursive
    if ((fun eq this.fun) && (arg eq this.arg))
      this
    else
      new App(fun, arg, initialMlValue)
  }

  override def await: Unit = Await.ready(someFuture, Duration.Inf)
  override lazy val someFuture: Future[Any] = fun.someFuture.flatMap(_ => arg.someFuture)
}

object App {
  /** Create a function application with function `fun` and argument `arg`. */
  def apply(fun: Term, arg: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) = new App(fun, arg)

  /** Allows to pattern match function applications. E.g.,
   * {{{
   *   term match {
   *     case App(t1,t2) => println(s"\${t1.pretty(ctxt)} was applied to \${t2.pretty(ctxt)}")
   *   }
   * }}}
   * Note that this will also match a [[Cterm]] and an [[MLValueTerm]] that represent a `$` in ML.
   **/
  @tailrec
  def unapply(term: Term): Option[(Term, Term)] = term match {
    case app : App => Some((app.fun,app.arg))
    case _ : MLValueTerm | _ : Cterm => unapply(term.concrete)
    case _ => None
  }
}

/** A lambda abstraction (ML constructor `Abs`). [[name]] is the name of the bound variable,
 * [[typ]] is the type of the bound variable, and [[body]] is the body of the lambda abstraction.
 *
 * E.g., `λx. x` would be represented as `Abs("x",typ, Bound(0))` for suitable `typ`.
 *
 * Note that [[name]] is for informative purposes only (i.e., it has no logical relevance)
 * since deBrujn indices are used. [[name]] can even be `""`.
 */
final class Abs private[pure] (val name: String, val typ: Typ, val body: Term, initialMlValue: MLValue[Term]=null)
                              (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  override lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeAbs(name,typ,body)
  override def toString: String = s"(λ$name. $body)"

  override def hashCode(): Int = new HashCodeBuilder(342345635,564562379)
    .append(name).append(body).toHashCode

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): Abs = {
    val typ = this.typ.concreteRecursive
    val body = this.body.concreteRecursive
    if ((typ eq this.typ) && (body eq this.body))
      this
    else
      new Abs(name, typ, body, initialMlValue)
  }

  override def await: Unit = body.await
  override lazy val someFuture: Future[Any] = body.someFuture
}

object Abs {
  /** Create a lambda abstraction with bound variable name `name`, bound variable type `typ`,
   * and body `body`. */
  def apply(name: String, typ: Typ, body: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Abs(name,typ,body)

  /** Allows to pattern match lambda abstractions. E.g.,
   * {{{
   *   term match {
   *     case Abs(name,typ,body) => println(s"Lambda abstraction λ\$name found")
   *   }
   * }}}
   * Note that this will also match a [[Cterm]] and an [[MLValueTerm]] that represent an `Abs` in ML.
   **/
  @tailrec
  def unapply(term: Term): Option[(String, Typ, Term)] = term match {
    case abs : Abs => Some((abs.name,abs.typ,abs.body))
    case _ : MLValueTerm | _ : Cterm => unapply(term.concrete)
    case _ => None
  }
}

/** A bound variable (ML constructor `Bound`). [[index]] is the deBrujn index of the variable.
 *
 * In a well-formed term, `Bound(i)` refers to the bound variable from the `i`-th enclosing [[Abs]].
 * (Starting from 0, i.e., `Bound(0)` refers to the directly enclosing [[Abs]].)
 **/
final class Bound private[pure] (val index: Int, initialMlValue: MLValue[Term]=null)
                                (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  override lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeBound(index)
  override def toString: String = s"Bound $index"

  override def hashCode(): Int = new HashCodeBuilder(442344345,423645769)
    .append(index).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit ec: ExecutionContext): Future[this.type] = Future.successful(this)

  override def concreteRecursive(implicit isabelle: Isabelle, ec: ExecutionContext): this.type = this
}

object Bound {
  /** Create a bound variable with index `index`. */
  def apply(index: Int)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Bound(index)

  /** Allows to pattern match bound variables. E.g.,
   * {{{
   *   term match {
   *     case Bound(index) => println(s"Bound variable found, index: \$index")
   *   }
   * }}}
   * Note that this will also match a [[Cterm]] and an [[MLValueTerm]] that represent a `Bound` in ML.
   **/
  @tailrec
  def unapply(term: Term): Option[Int] = term match {
    case bound : Bound => Some(bound.index)
    case _ : MLValueTerm | _ : Cterm => unapply(term.concrete)
    case _ => None
  }
}



object Term extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  //noinspection TypeAnnotation
  protected[pure] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.compileFunction
//    Typ.init()
//    isabelle.executeMLCodeNow("exception E_Term of term;; exception E_Cterm of cterm")

    val readTerm: MLFunction2[Context, String, Term] =
      compileFunction("fn (ctxt, str) => Syntax.read_term ctxt str")
    val readTermConstrained: MLFunction3[Context, String, Typ, Term] =
      compileFunction("fn (ctxt,str,typ) => Syntax.parse_term ctxt str |> Type.constraint typ |> Syntax.check_term ctxt")
    val stringOfTerm: MLFunction2[Context, Term, String] =
      compileFunction("fn (ctxt, term) => Syntax.pretty_term ctxt term |> Pretty.unformatted_string_of |> YXML.content_of")
    val stringOfCterm: MLFunction2[Context, Cterm, String] =
      compileFunction("fn (ctxt, cterm) => Thm.term_of cterm |> Syntax.pretty_term ctxt |> Pretty.unformatted_string_of |> YXML.content_of")
    val termOfCterm: MLFunction[Cterm, Term] =
      compileFunction("Thm.term_of")
    val ctermOfTerm: MLFunction2[Context, Term, Cterm] =
      compileFunction("fn (ctxt, term) => Thm.cterm_of ctxt term")
    val equalsTerm: MLFunction2[Term, Term, Boolean] =
      compileFunction("op=")

    val destTerm : MLRetrieveFunction[Term] =
      MLRetrieveFunction(
        """fn Const (name,typ) => DList[DInt 1, DString name, DObject (E_Typ typ)]
            | Free (name,typ) => DList[DInt 2, DString name, DObject (E_Typ typ)]
            | Var ((name,index),typ) => DList[DInt 3, DString name, DInt index, DObject (E_Typ typ)]
            | Bound i => DList[DInt 4, DInt i]
            | Abs (name, typ, body) => DList[DInt 5, DString name, DObject (E_Typ typ), DObject (E_Term body)]
            | t1 $ t2 => DList[DInt 6, DObject (E_Term t1), DObject (E_Term t2)]""")

    val makeConst : MLFunction2[String, Typ, Term] = MLValue.compileFunction("Const")
    val makeFree : MLFunction2[String, Typ, Term] = MLValue.compileFunction("Free")
    val makeVar : MLFunction3[String, Int, Typ, Term] = MLValue.compileFunction("fn (n,i,s) => Var ((n,i),s)")
    val makeApp : MLFunction2[Term, Term, Term] = MLValue.compileFunction("op$")
    val makeBound : MLFunction[Int, Term] = MLValue.compileFunction("Bound")
    val makeAbs : MLFunction3[String, Typ, Term, Term] = MLValue.compileFunction("Abs")

    val fastypeOf = MLValue.compileFunction[Term,Typ]("Term.fastype_of")
  }

  /** Creates a term from a string (using the parser from Isabelle).
   * E.g., `Term(context, "1+2")`.
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   **/
  def apply(context: Context, string: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValueTerm = {
    new MLValueTerm(Ops.readTerm(MLValue((context, string))))
  }

  /** Creates a term from a string (using the parser from Isabelle), subject to a type constraint.
   * E.g., `Term(context, "1+2", Type("Nat.nat"))` will infer that 1 and 2 are natural numbers.
   *
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   * @param typ The type constraint. I.e., the returned term will have type `typ`
   **/
  def apply(context: Context, string: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValueTerm = {
    new MLValueTerm(Ops.readTermConstrained(MLValue((context, string, typ))))
  }

  /** Representation of terms in ML.
   *
   *  - ML type: `term`
   *  - Representation of term `t` as an exception: `E_Term t`
   *
   * (`E_Term` is automatically declared when needed by the ML code in this package.
   * If you need to ensure that it is defined for compiling own ML code, invoke [[Term.init]].)
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object TermConverter extends Converter[Term] {
    override def store(value: Term)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Term] =
      value.mlValue
    override def retrieve(value: MLValue[Term])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Term] =
        Future.successful(new MLValueTerm(mlValue = value))
    override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "fn (E_Term t) => t"
    override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Term"

    override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "term"
  }
}
