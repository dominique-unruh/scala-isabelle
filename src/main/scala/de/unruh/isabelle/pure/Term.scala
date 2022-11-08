package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString}
import de.unruh.isabelle.control.{Isabelle, IsabelleMiscException, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Symbols}
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue._
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.pure.Term.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.control.Breaks

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext

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
sealed abstract class Term(
                            /** Contain an [[MLValue]] containing this term, or `null`.
                             * This is mutable, but it will only be replaced by other [[MLValue]]s containing equal terms (or `null`).
                             * This variable is updated without synchronization.
                             * This is safe because it will only be replaced by equivalent values.
                             * However, there is a potential of unnecessarily duplicating computations when a new MLValue is loaded.
                             * This is mitigated by the fact that invoking an Isabelle function ([[MLFunction.apply]] is ansynchronous and
                             * returns and [[MLValue]] very fast. Race conditions can still lead to duplicated loading (but this is just an
                             * issue of wasted efficiency, not of safety).
                             * */
                            private var mlValueVariable: MLValue[Term]
                          ) extends FutureValue with PrettyPrintable {

  protected def computeMlValue: MLValue[Term]

  /** Same as [[mlValue]] but may return `None` if no MLValue is currently available.
   * (Does not trigger any computation.) */
  final def peekMlValue: Option[MLValue[Term]] = Option(mlValueVariable)

  /** Is an [[mlValue]] currently available without computation? */
  final def mlValueLoaded: Boolean = mlValueVariable != null

  /** Forgets the [[MLValue]] associated with this term.
   * Note that the method [[mlValue]] will automatically create a new one when invoked.
   * Will be ignored for [[MLValueTerm]]s (because those cannot recover the term structure without the MLValue)
   * */
  def disconnectFromIsabelle(): Unit = mlValueVariable = null

  /** Transforms this term into an [[mlvalue.MLValue MLValue]] containing this term. This causes transfer of
   * the term to Isabelle only the first time it is accessed (and not at all if the term
   * came from the Isabelle process in the first place).
   *
   * The MLValue can change over time but will always be an MLValue for an equal term.
   * */
  final def mlValue : MLValue[Term] = {
    val val1 = mlValueVariable
    if (val1 == null) {
      val val2 = computeMlValue
      mlValueVariable = val2
      val2
    } else
      val1
  }

  /** [[control.Isabelle Isabelle]] instance relative to which this term was constructed. */
  implicit val isabelle : Isabelle

  // TODO Provide async version
  override def prettyRaw(ctxt: Context): String =
    Ops.stringOfTerm(MLValue((ctxt, this))).retrieveNow

  /** Transforms this term into a [[ConcreteTerm]]. A [[ConcreteTerm]] guarantees
   * that the type of the term ([[App]],[[Const]],[[Abs]]...) corresponds to the top-level
   * constructor on Isabelle side (`$`, `Const`, `Abs`, ...). */
  // TODO Provide async version
  val concrete : ConcreteTerm

  /** Transforms this term into a [[ConcreteTerm]] (see [[concrete]]).
   * In contrast to [[concrete]], it also replaces all subterms by concrete subterms. */
  // TODO Provide async version
  def concreteRecursive : ConcreteTerm

  /** Indicates whether [[concrete]] has already been initialized. (I.e.,
   * whether it can be accessed without delay and without incurring communication with
   * the Isabelle process. */
  def concreteComputed: Boolean

  /** `t $ u` is shorthand for [[App]]`(t,u)` */
  final def $(that: Term): App = App(this, that)

  /** Hash code compatible with [[equals]]. May fail with an exception, see [[equals]]. */
  override def hashCode(): Int = throw new NotImplementedError("Should be overridden")

  /** Makes this and that have the same [[MLValue]] if `condition` is true.
   * `condition` must not be true if the two terms are not equal!
   * @return `condition` */
  @inline final private def checkAndMerge(that: Term, condition: Boolean): Boolean = if (condition) {
    val thisVal = this.mlValueVariable
    if (thisVal != null)
      that.mlValueVariable = thisVal
    else {
      val thatVal = that.mlValueVariable
      if (thatVal != null)
        this.mlValueVariable = thatVal
    }
    true
  } else
    false

  final private def sameId(that: Term): Boolean = {
    val mlValueThis = this.mlValueVariable
    val mlValueThat = that.mlValueVariable
    if (mlValueThis != null && mlValueThat != null) {
      if (mlValueThis eq mlValueThat) true
      else Await.result(for (thisId <- mlValueThis.id;
                             thatId <- mlValueThat.id)
      yield thisId == thatId,
        Duration.Inf)
    } else
      false
  }

  /** Equality of terms. Returns true iff the two [[Term]] instances represent the same term in
   * the Isabelle process. (E.g., a [[Cterm]] and a [[Const]] can be equal.) May throw an exception
   * if the computation of the terms fails. (But will not fail if [[await]] or a related [[misc.FutureValue FutureValue]] method has
   * returned successfully on both terms.)
   *
   * As a side effect, comparing two terms makes their [[mlValue]]s equal (if the equality test returned true).
   * This means that comparing terms can reduce memory use on the Isabelle side (because duplicate terms are released),
   * and future equality checks will be faster.
   * Note: if both compared values already have ML Values, then the one from `this` will be copied to `that` (so the order matters).
   */
  // TODO Provide async version
  final override def equals(that: Any): Boolean = (this, that) match {
    case (_, t2: AnyRef) if this eq t2 => true
    case (_, t2: Term) if sameId(t2) => true
    case (t1: App, t2: App) => checkAndMerge(t2, t1.fun == t2.fun && t1.arg == t2.arg)
    case (t1: Bound, t2: Bound) => checkAndMerge(t2, t1.index == t2.index)
    case (t1: Var, t2: Var) => checkAndMerge(t2, t1.name == t2.name && t1.index == t2.index && t1.typ == t2.typ)
    case (t1: Free, t2: Free) => checkAndMerge(t2, t1.name == t2.name && t1.typ == t2.typ)
    case (t1: Const, t2: Const) => checkAndMerge(t2, t1.name == t2.name && t1.typ == t2.typ)
    case (t1: Abs, t2: Abs) => checkAndMerge(t2, t1.name == t2.name && t1.typ == t2.typ && t1.body == t2.body)
    case (t1: Cterm, t2: Cterm) =>
      Await.result(for (t1id <- t1.ctermMlValue.id;
                        t2id <- t2.ctermMlValue.id)
      yield
        if (t1id == t2id) true
        else checkAndMerge(t2, t1.mlValueTerm == t2.mlValueTerm),
        Duration.Inf)
    case (t1: Cterm, t2: Term) => checkAndMerge(t2, t1.mlValueTerm == t2)
    case (t1: Term, t2: Cterm) => checkAndMerge(t2, t1 == t2.mlValueTerm)
    case (t1: MLValueTerm, t2: MLValueTerm) =>
      checkAndMerge(t2,
        if (t1.concreteComputed && t2.concreteComputed) t1.concrete == t2.concrete
        else Ops.equalsTerm(t1,t2).retrieveNow)
    case (t1: MLValueTerm, t2: Term) =>
      checkAndMerge(t2,
        if (t1.concreteComputed) t1.concrete == t2
        else Ops.equalsTerm(t1,t2).retrieveNow)
    case (t1: Term, t2: MLValueTerm) =>
      checkAndMerge(t2,
        if (t2.concreteComputed) t1 == t2.concrete
        else Ops.equalsTerm(t1,t2).retrieveNow)
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
  // TODO Provide async version
  final def fastType : Typ = {
    import Breaks._
    tryBreakable {
      def typ(t: Term, env: List[Typ]): Typ = t match {
        case Free(_, t) => t
        case Const(_, t) => t
        case Var(_, _, t) => t
        case Abs(_, t, body) => t -->: typ(body, t::env)
        case Bound(i) => env.lift(i) match {
          case Some(t) => t
          case None => throw IsabelleMiscException("Term.fastType: Term contains loose bound variable")
        }
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
sealed abstract class ConcreteTerm(initialMlValue: MLValue[Term]) extends Term(initialMlValue) {
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
final class Cterm private(val ctermMlValue: MLValue[Cterm])(implicit val isabelle: Isabelle) extends Term(null) {
  /** Returns this term as an `MLValue[Term]` (not `MLValue[Cterm]`). The difference is crucial
   * because `MLValue[_]` is not covariant. So for invoking ML functions that expect an argument of type `term`, you
   * need to get an `MLValue[Term]`. In contrast, [[ctermMlValue]] returns this term as an `MLValue[Cterm]`. */
  override protected def computeMlValue: MLValue[Term] = Ops.termOfCterm(ctermMlValue)
  /** Transforms this [[Cterm]] into an [[MLValueTerm]]. */
  private [pure] lazy val mlValueTerm = new MLValueTerm(mlValue)
  override def prettyRaw(ctxt: Context): String =
    Ops.stringOfCterm(MLValue((ctxt, this))).retrieveNow
  lazy val concrete: ConcreteTerm = mlValueTerm.concrete
  override def concreteRecursive: ConcreteTerm = mlValueTerm.concreteRecursive
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
           (implicit isabelle: Isabelle) =
    new Cterm(mlValue)

  /** Converts a [[Term]] into a [[Cterm]]. This involves type-checking (relative to the
   * context `ctxt`). The resulting [[Cterm]] is then certified to be correctly typed.
   *
   * If `term` is already a [[Cterm]], then `term` is transferred to the context `ctxt`.
   * (Which guarantees that `term` is also a valid term w.r.t. `ctxt`.)
   * If this is not possible, `term` is re-checked to create a cterm.
   * */
  def apply(ctxt: Context, term: Term)(implicit isabelle: Isabelle) : Cterm = term match {
    case cterm : Cterm =>
      // We cannot just return `cterm` because it may be a cterm w.r.t. the wrong context.
      // But re-checking the term is wasteful if the term was already checked w.r.t. this context.
      new Cterm(Ops.ctermOfCterm(ctxt, cterm))
    case term => new Cterm(Ops.ctermOfTerm(ctxt, term))
  }

  /** Parses `string` as a term and returns the result as a [[Cterm]]. */
  def apply(ctxt: Context, string: String)(implicit isabelle: Isabelle) : Cterm =
    Cterm(ctxt, Term(ctxt, string))

  /** Parses `string` as a term of type `typ` and returns the result as a [[Cterm]]. */
  def apply(ctxt: Context, string: String, typ: Typ)(implicit isabelle: Isabelle) : Cterm =
    Cterm(ctxt, Term(ctxt, string, typ))

  /** Representation of cterms in ML.
   *
   *  - ML type: `cterm`
   *  - Representation of term `t` as an exception: `E_Cterm t`
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object CtermConverter extends Converter[Cterm] {
    override def store(value: Cterm)(implicit isabelle: Isabelle): MLValue[Cterm] =
      value.ctermMlValue
    override def retrieve(value: MLValue[Cterm])(implicit isabelle: Isabelle): Future[Cterm] =
        Future.successful(new Cterm(ctermMlValue = value))
    override def exnToValue(implicit isabelle: Isabelle): String = "fn (E_Cterm t) => t"
    override def valueToExn(implicit isabelle: Isabelle): String = "E_Cterm"

    override def mlType(implicit isabelle: Isabelle): String = "cterm"
  }
}

/** A [[Term]] that is stored in the Isabelle process's object store
 * and may or may not be known in Scala. Use [[concrete]] to
 * get a representation of the same term as a [[ConcreteTerm]]. */
final class MLValueTerm private[pure] (initialMlValue: MLValue[Term])(implicit val isabelle: Isabelle) extends Term(initialMlValue) {
  override protected def computeMlValue: MLValue[Term] = throw new IllegalStateException("MLValueTerm.computeMLValue should never be called")

  /** Does not do anything. */
  override def disconnectFromIsabelle(): Unit = {}

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

  override def concreteRecursive: ConcreteTerm = concrete.concreteRecursive

  override def toString: String =
    if (concreteLoaded) concrete.toString
    else s"‹term${mlValue.stateString}›"

}

object MLValueTerm {
  def apply(mlValue: MLValue[Term])(implicit isabelle: Isabelle): MLValueTerm = new MLValueTerm(mlValue)
}

/** A constant (ML constructor `Const`). [[name]] is the fully qualified name of the constant (e.g.,
 * `"HOL.True"`) and [[typ]] its type. */
final class Const private[pure](val name: String, val typ: Typ, initialMlValue: MLValue[Term]=null)
                               (implicit val isabelle: Isabelle) extends ConcreteTerm(initialMlValue) {
  override protected def computeMlValue : MLValue[Term] = Ops.makeConst(MLValue((name,typ)))
  override def toString: String = name

  override def concreteRecursive: Const = {
    val typ = this.typ.concreteRecursive
    if (typ eq this.typ)
      this
    else
      new Const(name, typ, peekMlValue.orNull)
  }

  override def hashCode(): Int = new HashCodeBuilder(162389433,568734237)
    .append(name).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit isabelle: Isabelle): Future[this.type] = Future.successful(this)
}

object Const {
  /** Create a constant with name `name` and type `typ`. */
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle) = new Const(name, typ)

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
                              (implicit val isabelle: Isabelle) extends ConcreteTerm(initialMlValue) {
  override protected def computeMlValue : MLValue[Term] = Ops.makeFree(name, typ)
  override def toString: String = name

  override def hashCode(): Int = new HashCodeBuilder(384673423,678423475)
    .append(name).toHashCode

  override def concreteRecursive: Free = {
    val typ = this.typ.concreteRecursive
    if (typ eq this.typ)
      this
    else
      new Free(name, typ, peekMlValue.orNull)
  }

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit isabelle: Isabelle): Future[this.type] = Future.successful(this)
}

object Free {
  /** Create a free variable with name `name` and type `typ`. */
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle) = new Free(name, typ)

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
                             (implicit val isabelle: Isabelle) extends ConcreteTerm(initialMlValue) {
  override protected def computeMlValue: MLValue[Term] = Ops.makeVar(name, index, typ)
  override def toString: String = s"?$name$index"

  override def hashCode(): Int = new HashCodeBuilder(3474285, 342683425)
    .append(name).append(index).toHashCode

  override def concreteRecursive: Var = {
    val typ = this.typ.concreteRecursive
    if (typ eq this.typ)
      this
    else
      new Var(name, index, typ, peekMlValue.orNull)
  }

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit isabelle: Isabelle): Future[this.type] = Future.successful(this)
}

object Var {
  /** Create a schematic variable with name `name`, index `index`, and type `typ`. */
  def apply(name: String, index: Int, typ: Typ)(implicit isabelle: Isabelle) = new Var(name, index, typ)

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
                              (implicit val isabelle: Isabelle) extends ConcreteTerm(initialMlValue) {
  override protected def computeMlValue: MLValue[Term] = Ops.makeApp(fun,arg)

  override def toString: String = s"($fun $$ $arg)"

  override def hashCode(): Int = new HashCodeBuilder(334234237,465634533)
    .append(arg).toHashCode

  override def concreteRecursive: App = {
    val fun = this.fun.concreteRecursive
    val arg = this.arg.concreteRecursive
    if ((fun eq this.fun) && (arg eq this.arg))
      this
    else
      new App(fun, arg, peekMlValue.orNull)
  }

  override def await: Unit = Await.ready(someFuture, Duration.Inf)
  override lazy val someFuture: Future[Any] = fun.someFuture.flatMap(_ => arg.someFuture)
}

object App {
  /** Create a function application with function `fun` and argument `arg`. */
  def apply(fun: Term, arg: Term)(implicit isabelle: Isabelle) = new App(fun, arg)

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
                              (implicit val isabelle: Isabelle) extends ConcreteTerm(initialMlValue) {
  override protected def computeMlValue: MLValue[Term] = Ops.makeAbs(name,typ,body)
  override def toString: String = s"(λ$name. $body)"

  override def hashCode(): Int = new HashCodeBuilder(342345635,564562379)
    .append(name).append(body).toHashCode

  override def concreteRecursive: Abs = {
    val typ = this.typ.concreteRecursive
    val body = this.body.concreteRecursive
    if ((typ eq this.typ) && (body eq this.body))
      this
    else
      new Abs(name, typ, body, peekMlValue.orNull)
  }

  override def await: Unit = body.await
  override lazy val someFuture: Future[Any] = body.someFuture
}

object Abs {
  /** Create a lambda abstraction with bound variable name `name`, bound variable type `typ`,
   * and body `body`. */
  def apply(name: String, typ: Typ, body: Term)(implicit isabelle: Isabelle) = new Abs(name,typ,body)

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
                                (implicit val isabelle: Isabelle) extends ConcreteTerm(initialMlValue) {
  override protected def computeMlValue: MLValue[Term] = Ops.makeBound(index)
  override def toString: String = s"Bound $index"

  override def hashCode(): Int = new HashCodeBuilder(442344345,423645769)
    .append(index).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit isabelle: Isabelle): Future[this.type] = Future.successful(this)

  override def concreteRecursive: this.type = this
}

object Bound {
  /** Create a bound variable with index `index`. */
  def apply(index: Int)(implicit isabelle: Isabelle) = new Bound(index)

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
  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops()
  //noinspection TypeAnnotation
  protected[pure] class Ops(implicit val isabelle: Isabelle) {
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
    val ctermOfCterm: MLFunction2[Context, Cterm, Cterm] =
      compileFunction("""fn (ctxt, ct) => Thm.transfer_cterm (Proof_Context.theory_of ctxt) ct
          handle Thm.CONTEXT _ => Thm.cterm_of ctxt (Thm.term_of ct)""")

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
   *
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   * @param symbols Instance of [[misc.Symbols Symbols]] to convert `string` to Isabelle's internal encoding
   **/
  def apply(context: Context, string: String, symbols : Symbols = Symbols.globalInstance)(implicit isabelle: Isabelle): MLValueTerm = {
    new MLValueTerm(Ops.readTerm(context, symbols.unicodeToSymbols(string)))
  }

  /** Creates a term from a string (using the parser from Isabelle), subject to a type constraint.
   * E.g., `Term(context, "1+2", Type("Nat.nat"))` will infer that 1 and 2 are natural numbers.
   *
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   * @param typ The type constraint. I.e., the returned term will have type `typ`
   **/
  def apply(context: Context, string: String, typ: Typ)(implicit isabelle: Isabelle): MLValueTerm =
    Term(context, string, typ, Symbols.globalInstance)

  /** Same as [[de.unruh.isabelle.pure.Term#apply(context:de\.unruh\.isabelle\.pure\.Context,string:String,typ:de\.unruh\.isabelle\.pure\.Typ,symbols:de\.unruh\.isabelle\.misc\.Symbols)* apply(Context,String,Typ)]] but allows to specify the [[misc.Symbols Symbols]] translation table.
   *
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   * @param typ The type constraint. I.e., the returned term will have type `typ`
   * @param symbols Instance of [[misc.Symbols Symbols]] to convert `string` to Isabelle's internal encoding
   **/
  def apply(context: Context, string: String, typ: Typ, symbols : Symbols)(implicit isabelle: Isabelle): MLValueTerm =
    new MLValueTerm(Ops.readTermConstrained(MLValue((context, symbols.unicodeToSymbols(string), typ))))

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
    override def store(value: Term)(implicit isabelle: Isabelle): MLValue[Term] =
      value.mlValue
    override def retrieve(value: MLValue[Term])(implicit isabelle: Isabelle): Future[Term] =
        Future.successful(new MLValueTerm(initialMlValue = value))
    override def exnToValue(implicit isabelle: Isabelle): String = "fn (E_Term t) => t"
    override def valueToExn(implicit isabelle: Isabelle): String = "E_Term"

    override def mlType(implicit isabelle: Isabelle): String = "term"
  }
}
