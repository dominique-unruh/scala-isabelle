package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString}
import de.unruh.isabelle.control.{Isabelle, IsabelleMLException, IsabelleMiscException, OperationCollection}
import de.unruh.isabelle.misc.{FutureValue, Symbols}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLRetrieveFunction, MLValue}
import de.unruh.isabelle.pure.Typ.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.control.Isabelle.executionContext

/**
 * This class represents a typ (ML type `typ`) in Isabelle. It can be transferred to and from the Isabelle process
 * transparently by internally using [[mlvalue.MLValue MLValue]]s (see below).
 *
 * In most respects, [[Typ]] behaves as if it was an algebraic datatype defined as follows:
 * {{{
 *   sealed abstract class Typ
 *   final case class Type(name: String, args: List[Typ])                // Corresponds to ML constructor 'Type'
 *   final case class TFree(name: String, sort: List[String])            // Corresponds to ML constructor 'TFree'
 *   final case class TVar(name: String, index: Int, sort: List[String]) // Corresponds to ML constructor 'TVar'
 * }}}
 *
 * tl;dr for the explanation below: Types can be treated as if they were the case classes above (even though
 * there actually are more classes), both in object
 * creation and in pattern matching, with the exception that
 * one should not use type patterns (e.g., `case _ : Const =>`).
 *
 * Having [[Typ]]s defined in terms of those case classes would mean that when retrieving a type from the Isabelle
 * process, the whole type
 * needs to be retrieved. Since types can be large and might be transferred back and forth a lot
 * (with minor modifications), we choose an approach where types may be partially stored in Scala, and partially in
 * the Isabelle process. That is, an instance of [[Typ]] can be any of the above classes, or a reference to a type
 * in the object store in the Isabelle process, or both at the same time. And the same applies to subterms of the type, too.
 * So for example, if we retrieve types `t`,`u` from Isabelle to Scala, and then in Scala construct `Type("fun",List(t,u))`,
 * and then transfer `Type("fun",List(t,u))` back to Isabelle, the types `t`,`u` will never be serialized, and only the
 * constructor `Type` and the string `"fun"` will need to be transferred.
 *
 * In order to faciliate this, the classes [[Type]], [[TFree]], [[TVar]]
 * (collectively referred to as a [[ConcreteTyp]]) additionally
 * store an reference [[Typ.mlValue]] to the Isabelle object store (this reference is initialized lazily, thus
 * accessing it can force the type to be transferred to the Isabelle process). And furthermore, there is an additional
 * subclass [[MLValueTyp]] of [[Typ]] that represents a type that is stored in Isabelle but not available in
 * Scala at class creation time. Instances of [[MLValueTyp]] never need to be created manually, though. You
 * just have to be aware that some types might not be instances of the three [[ConcreteTyp]] "case classes".
 * (But if a [[ConcreteTyp]] is required, any term can be converted using `typ.`[[Typ.concrete concrete]].)
 *
 * Pattern matching works as expected, that is, when an [[MLValueTyp]] `t`, e.g., refers to an Isabelle type of the form
 * `TFree (name,sort)`, then `t` will match the pattern `case TFree(name,sort) =>`. (Necessary information will be
 * transferred from the Isabelle process on demand.) Because of this, one can almost
 * completely ignore the existence of [[MLValueTyp]]. The only caveat is that one should not do a pattern match on the
 * Scala-type of the [[Typ]]. That is `case _ : TFree =>` will not match a term `TFree(name,sort)` represented by
 * an [[MLValueTyp]].
 *
 * Two types are equal (w.r.t.~the `equals` method) iff they represent the same Isabelle types. I.e.,
 * an [[MLValueTyp]] and a [[TFree]] can be equal. (Equality tests try to transfer as little data as possible when
 * determining equality.)
 *
 * Furthermore, there is a subclass [[Ctyp]] of [[Typ]] that represents an ML value of type `ctyp`. This is
 * logically a type with additional that is certified to be well-formed with respect to some Isabelle context.
 * In this implementation, a [[Ctyp]] is also a [[Typ]], so it is possible to do, e.g., equality tests between
 * [[Ctyp]]s and regular terms (such as [[TFree]]) without explicit conversions. Similarly, patterns such as
 * `case TFree(name,sort) =>` also match [[Ctyp]]s.
 */
sealed abstract class Typ(
                           /** Contain an [[MLValue]] containing this type, or `null`.
                            * This is mutable, but it will only be replaced by other [[MLValue]]s containing equal types (or `null`).
                            * This variable is updated without synchronization.
                            * This is safe because it will only be replaced by equivalent values.
                            * However, there is a potential of unnecessarily duplicating computations when a new MLValue is loaded.
                            * This is mitigated by the fact that invoking an Isabelle function ([[MLFunction.apply]] is ansynchronous and
                            * returns and [[MLValue]] very fast. Race conditions can still lead to duplicated loading (but this is just an
                            * issue of wasted efficiency, not of safety).
                            * */
                           private var mlValueVariable: MLValue[Typ]
                         ) extends FutureValue with PrettyPrintable {

  protected def computeMlValue: MLValue[Typ]

  /** Same as [[mlValue]] but may return `None` if no MLValue is currently available.
   * (Does not trigger any computation.) */
  final def peekMlValue: Option[MLValue[Typ]] = Option(mlValueVariable)

  /** Is an [[mlValue]] currently available without computation? */
  final def mlValueLoaded: Boolean = mlValueVariable != null

  /** Forgets the [[MLValue]] associated with this term.
   * Note that the method [[mlValue]] will automatically create a new one when invoked.
   * Will be ignored for [[MLValueTerm]]s (because those cannot recover the term structure without the MLValue)
   * */
  def disconnectFromIsabelle(): Unit = mlValueVariable = null

  /** Transforms this type into an [[mlvalue.MLValue MLValue]] containing this type. This causes transfer of
   * the type to Isabelle only the first time it is accessed (and not at all if the type
   * came from the Isabelle process in the first place).
   *
   * The MLValue can change over time but will always be an MLValue for an equal type.
   * */
  final def mlValue: MLValue[Typ] = {
    val val1 = mlValueVariable
    if (val1 == null) {
      val val2 = computeMlValue
      mlValueVariable = val2
      val2
    } else
      val1
  }

  /** [[control.Isabelle Isabelle]] instance relative to which this type was constructed. */
  implicit val isabelle : Isabelle

  override def prettyRaw(ctxt: Context): String =
    Ops.stringOfType(MLValue((ctxt, this))).retrieveNow

  /** Transforms this term into a [[ConcreteTyp]]. A [[ConcreteTyp]] guarantees
   * that the Scala-type of the [[Typ]] ([[Type]],[[TFree]],[[TVar]]) corresponds to the top-level
   * constructor on Isabelle side (`Type`, `TFree`, `TVar`). */
  val concrete : ConcreteTyp

  /** Transforms this typ into a [[ConcreteTyp]] (see [[concrete]]).
   * In contrast to [[concrete]], it also replaces all subtypes by concrete subterms. */
  def concreteRecursive(implicit isabelle: Isabelle) : ConcreteTyp

  /** Indicates whether [[concrete]] has already been initialized. (I.e.,
   * whether it can be accessed without delay and without incurring communication with
   * the Isabelle process. */
  def concreteComputed: Boolean

  /** `t -->: u` is shorthand for `Type("fun", t, u)`, i.e., for a function from `t` to `u`. */
  def -->:(that: Typ): Type = Type("fun", that, this)

  /** Hash code compatible with [[equals]]. May fail with an exception, see [[equals]]. */
  override def hashCode(): Int = throw new NotImplementedError("Should be overridden")

  /** Makes this and that have the same [[MLValue]] if `condition` is true.
   * `condition` must not be true if the two types are not equal!
   *
   * @return `condition` */
  @inline final private def checkAndMerge(that: Typ, condition: Boolean): Boolean = if (condition) {
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

  final private def sameId(that: Typ): Boolean = {
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

  /** Equality of types. Returns true iff the two [[Typ]] instances represent the same type in
   * the Isabelle process. (E.g., a [[Ctyp]] and a [[TFree]] can be equal.) May throw an exception
   * if the computation of the terms fails. (But will not fail if [[await]] or a related
   * [[misc.FutureValue FutureValue]] method has returned successfully on both terms.)
   *
   * As a side effect, comparing two types makes their [[mlValue]]s equal (if the equality test returned true).
   * This means that comparing terms can reduce memory use on the Isabelle side (because duplicate types are released),
   * and future equality checks will be faster.
   * Note: if both compared values already have ML Values, then the one from `this` will be copied to `that` (so the order matters).
   */
  final override def equals(that: Any): Boolean = (this, that) match {
    case (_, t2: AnyRef) if this eq t2 => true
    case (_, t2: Typ) if sameId(t2) => true
    case (t1: Type, t2: Type) => checkAndMerge(t2, t1.name == t2.name && t1.args == t2.args)
    case (t1: TVar, t2: TVar) => checkAndMerge(t2, t1.name == t2.name && t1.index == t2.index && t1.sort == t2.sort)
    case (t1: TFree, t2: TFree) => checkAndMerge(t2, t1.name == t2.name && t1.sort == t2.sort)
    case (t1: Ctyp, t2: Ctyp) =>
      Await.result(for (t1id <- t1.ctypMlValue.id;
                        t2id <- t2.ctypMlValue.id)
      yield
        if (t1id == t2id) true
        else checkAndMerge(t2, t1.mlValueTyp == t2.mlValueTyp),
        Duration.Inf)
    case (t1: Ctyp, t2: Typ) => checkAndMerge(t2, t1.mlValueTyp == t2)
    case (t1: Typ, t2: Ctyp) => checkAndMerge(t2, t1 == t2.mlValueTyp)
    case (t1: MLValueTyp, t2: MLValueTyp) =>
      checkAndMerge(t2,
        if (t1.concreteComputed && t2.concreteComputed) t1.concrete == t2.concrete
        else Ops.equalsTyp(t1, t2).retrieveNow)
    case (t1: MLValueTyp, t2: Typ) =>
      checkAndMerge(t2,
        if (t1.concreteComputed) t1.concrete == t2
        else Ops.equalsTyp(t1,t2).retrieveNow)
    case (t1: Typ, t2: MLValueTyp) =>
      checkAndMerge(t2,
        if (t2.concreteComputed) t1 == t2.concrete
        else Ops.equalsTyp(t1,t2).retrieveNow)
    case _ => false
  }


  /** Produces a string representation of this type.
   *
   * This is not a "pretty" representation, it does not use Isabelle syntax, and subterms that are stored only
   * in the Isabelle process are replaced with
   * a placeholder (thus this method does not invoke any potentially communication with the Isabelle process).
   *
   * @see pretty for pretty printed type
   **/
  override def toString: String
}

/** Base class for all concrete types.
 * A [[ConcreteTyp]] guarantees
 * that the Scala-type of the [[Typ]] ([[Type]],[[TFree]],[[TVar]]...) corresponds to the top-level
 * constructor on Isabelle side (`Type`, `TFree`, `TVar`).
 */
sealed abstract class ConcreteTyp(initialMlValue: MLValue[Typ]) extends Typ(initialMlValue) {
  /** @return this */
  override val concrete: this.type = this

  /** @return true */
  override def concreteComputed: Boolean = true
}

/** A [[Typ]] that is stored in the Isabelle process's object store
 * and may or may not be known in Scala. Use [[concrete]] to
 * get a representation of the same type as a [[ConcreteTyp]]. */
final class MLValueTyp private[pure] (initialMLValue: MLValue[Typ])(implicit val isabelle: Isabelle) extends Typ(initialMLValue) {
  override protected def computeMlValue: MLValue[Typ] = throw new IllegalStateException("MLValueTyp.computeMLValue should never be called")

  /** Does not do anything. */
  override def disconnectFromIsabelle(): Unit = {}

  @inline override def concreteComputed: Boolean = concreteLoaded
  @volatile private var concreteLoaded = false

  lazy val concrete : ConcreteTyp = {
    val DList(DInt(constructor), data @_*) = Await.result(Ops.destTyp(mlValue), Duration.Inf)
    val typ = (constructor,data) match {
      case (1,List(DString(name), args@_*)) =>
        val args2 = args.map {
          case DObject(id) => MLValue.unsafeFromId[Typ](id).retrieveNow
          case data => throw IsabelleMiscException(s"Internal error: expected DObject, not $data") }.toList
        new Type(name, args2, mlValue)
      case (2,List(DString(name), sort@_*)) =>
        val sort2 = sort.map {
          case DString(clazz) => clazz
          case data => throw IsabelleMiscException(s"Internal error: expected DString, not $data") }.toList
        new TFree(name,sort2,mlValue)
      case (3,List(DString(name), DInt(index), sort@_*)) =>
        val sort2 = sort.map {
          case DString(clazz) => clazz
          case data => throw IsabelleMiscException(s"Internal error: expected DObject, not $data")
        }.toList
        new TVar(name,index.toInt,sort2,mlValue)
    }
    concreteLoaded = true
    typ
  }

  override def concreteRecursive(implicit isabelle: Isabelle): ConcreteTyp = concrete.concreteRecursive

  override def hashCode(): Int = concrete.hashCode()

  override def toString: String =
    if (concreteLoaded) concrete.toString
    else s"‹typ${mlValue.stateString}›"

  override def someFuture: Future[Any] = mlValue.someFuture
  override def await: Unit = mlValue.await
}

object MLValueTyp {
  def apply(mlValue: MLValue[Typ])(implicit isabelle: Isabelle): MLValueTyp = new MLValueTyp(mlValue)
}

/** Represents a `ctyp` in Isabelle. In Isabelle, a `ctyp` must be explicitly converted into a `typ`. In contrast,
 * this class inherits from [[Typ]], so no explicit conversions are needed. (They happen automatically on
 * demand.)
 * A [[Ctyp]] is always well-formed relative to the context for which it was
 * created (this is ensured by the Isabelle trusted core).
 **/
final class Ctyp private(val ctypMlValue: MLValue[Ctyp])(implicit val isabelle: Isabelle) extends Typ(null) {
  /** Returns this term as an `MLValue[Typ]` (not `MLValue[Ctyp]`). The difference is crucial
   * because `MLValue[_]` is not covariant. So for invoking ML functions that expect an argument of type `typ`, you
   * need to get an `MLValue[Typ]`. In contrast, [[ctypMlValue]] returns this type as an `MLValue[Ctyp]`. */
  override protected def computeMlValue: MLValue[Typ] = Ops.typOfCtyp(ctypMlValue)
  /** Transforms this [[Ctyp]] into an [[MLValueTyp]]. */
  private [pure] lazy val mlValueTyp = new MLValueTyp(mlValue)

  override def prettyRaw(ctxt: Context): String =
    Ops.stringOfCtyp(MLValue((ctxt, this))).retrieveNow

  override lazy val concrete: ConcreteTyp = new MLValueTyp(mlValue).concrete

  override def concreteComputed: Boolean =
    if (mlValueLoaded) mlValueTyp.concreteComputed
    else false

  def concreteRecursive(implicit isabelle: Isabelle): ConcreteTyp = mlValueTyp.concreteRecursive

  override def hashCode(): Int = concrete.hashCode()

  override def await: Unit = ctypMlValue.await
  override def someFuture: Future[Any] = ctypMlValue.someFuture

  override def toString: String =
    if (mlValueLoaded) "cterm:"+mlValue.toString
    else "cterm"+stateString
}

object Ctyp {
  /** Creates a [[Ctyp]] from an [[mlvalue.MLValue MLValue]][[[Ctyp]]]. Since a [[Ctyp]]
   * is just a wrapper around an [[mlvalue.MLValue MLValue]][[[Ctyp]]], this operation does not
   * require any communication with the Isabelle process. */
  def apply(mlValue: MLValue[Ctyp])
           (implicit isabelle: Isabelle) =
    new Ctyp(mlValue)

  /** Converts a [[Typ]] into a [[Ctyp]]. This involves type-checking (relative to the
   * context `ctxt`). The resulting [[Ctyp]] is then certified to be correctly formed. 
   * 
   * If `typ` is already a [[Ctyp]], then `typ` is transferred to the context `ctxt`.
   * (Which guarantees that `typ` is also a valid typ w.r.t. `ctxt`.)
   * If this is not possible, `typ` is re-checked to create a ctyp.
   */
  def apply(ctxt: Context, typ: Typ)(implicit isabelle: Isabelle): Ctyp = typ match {
    case ctyp : Ctyp =>
      // We cannot just return `ctyp` because it may be a ctyp w.r.t. the wrong context.
      // But re-checking the typ is wasteful if the typ was already checked w.r.t. this context.
      new Ctyp(Ops.ctypOfCtyp(ctxt, ctyp))
    case typ => new Ctyp(Ops.ctypOfTyp(MLValue((ctxt, typ))))
  }

  /** Parses `string` as a typ and returns the result as a [[Ctyp]]. */
  def apply(ctxt: Context, string: String)(implicit isabelle: Isabelle) : Ctyp =
    Ctyp(ctxt, Typ(ctxt, string))

  /** Representation of ctyps in ML.
   *
   *  - ML type: `ctyp`
   *  - Representation of term `t` as an exception: `E_Ctyp t`
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object CtypConverter extends Converter[Ctyp] {
    override def store(value: Ctyp)(implicit isabelle: Isabelle): MLValue[Ctyp] =
      value.ctypMlValue
    override def retrieve(value: MLValue[Ctyp])(implicit isabelle: Isabelle): Future[Ctyp] =
      Future.successful(new Ctyp(ctypMlValue = value))
    override def exnToValue(implicit isabelle: Isabelle): String = "fn (E_Ctyp t) => t"
    override def valueToExn(implicit isabelle: Isabelle): String = "E_Ctyp"

    override def mlType(implicit isabelle: Isabelle): String = "ctyp"
  }
}

/** A type constructor (ML constructor `Type`). [[name]] is the fully qualified name of the type constructor (e.g.,
 * `"List.list"`) and [[args]] its type parameters. */
final class Type private[pure](val name: String, val args: List[Typ], initialMlValue: MLValue[Typ]=null)
                              (implicit val isabelle: Isabelle) extends ConcreteTyp(initialMlValue) {
  override protected def computeMlValue : MLValue[Typ] = Ops.makeType(MLValue(name,args))
  override def toString: String =
    if (args.isEmpty) name
    else s"$name(${args.mkString(", ")})"

  override def hashCode(): Int = new HashCodeBuilder(342534543,34774653)
    .append(name).toHashCode

  override def concreteRecursive(implicit isabelle: Isabelle): Type = {
    var changed = false
    val args = for (a <- this.args) yield {
      val a2 = a.concreteRecursive
      if (a ne a2) changed = true
      a2
    }
    if (changed)
      new Type(name, args, peekMlValue.orNull)
    else
      this
  }

  override def await: Unit = Await.ready(someFuture, Duration.Inf)
  override lazy val someFuture: Future[Any] = {
    implicit val executionContext: ExecutionContext = isabelle.executionContext
    Future.traverse(args : Seq[Typ])(_.someFuture).map(_ => ())
  }
}

object Type {
  /** Create a type with type constructor `name` and type parameters `args`. */
  def apply(name: String, args: Typ*)(implicit isabelle: Isabelle) = new Type(name, args.toList)

  /** Allows to pattern match types. E.g.,
   * {{{
   *   typ match {
   *     case Type("Nat.nat") => println(s"Type nat found")
   *     case Type("List.list", arg) => println(s"List of \$arg found")
   *     case Type(name, args @ _* => println(s"Type \$name found (with \${args.length} parameters)")
   *   }
   * }}}
   * Note that this will also match a [[Ctyp]] and an [[MLValueTyp]] that represent a `Type` in ML.
   **/
  @tailrec
  def unapplySeq(typ: Typ): Option[(String, Seq[Typ])] = typ match {
    case typ : Type => Some((typ.name,typ.args))
    case _ : MLValueTyp | _ : Ctyp => unapplySeq(typ.concrete)
    case _ => None
  }
}

/** A free type variable (ML constructor `TFree`). [[name]] is the name of the type variable (e.g.,
 * `"'a'"`) and [[sort]] its sort. (The sort is a list of fully qualified type class names.)
 * Note that type variables whose names do not start with ' are not legal in Isabelle. */
final class TFree private[pure] (val name: String, val sort: List[String], initialMlValue: MLValue[Typ]=null)
                                (implicit val isabelle: Isabelle) extends ConcreteTyp(initialMlValue) {
  override protected def computeMlValue : MLValue[Typ] = Ops.makeTFree(name, sort)
  override def toString: String = sort match {
    case List(clazz) => s"$name::$clazz"
    case _ => s"$name::{${sort.mkString(",")}}"
  }

  override def concreteRecursive(implicit isabelle: Isabelle): this.type = this

  override def hashCode(): Int = new HashCodeBuilder(335434265,34255633)
    .append(name).append(sort).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit isabelle: Isabelle): Future[this.type] = Future.successful(this)
}

object TFree {
  /** Create a free type variable with name `name` and sort `sort`. */
  def apply(name: String, sort: Seq[String])
           (implicit isabelle: Isabelle) = new TFree(name, sort.toList)

  /** Allows to pattern match free type variables. E.g.,
   * {{{
   *   typ match {
   *     case TFree(name,sort) => println(s"Free type variable \$name found")
   *   }
   * }}}
   * Note that this will also match a [[Ctyp]] and an [[MLValueTyp]] that represent a `TFree` in ML.
   **/
  @tailrec
  def unapply(typ: Typ): Option[(String, List[String])] = typ match {
    case typ : TFree => Some((typ.name,typ.sort))
    case _ : MLValueTyp | _ : Ctyp => unapply(typ.concrete)
    case _ => None
  }
}

/** A schematic type variable (ML constructor `TVar`). [[name]] is the name of the type variable (e.g.,
 * `"'a'"`) and [[sort]] its sort. (The sort is a list of fully qualified type class names.)
 *
 * Schematic type variables are the ones that are represented with a leading question mark in
 * Isabelle's parsing and pretty printing. E.g., `?'a` is a [[TVar]] with [[name]]`="'a"`
 * and [[index]]`=0`. And `?'b1` or `?'b.1` is a [[TVar]] with [[name]]`="'b"` and [[index]]`=1`.
 *
 * Note that type variables whose names do not start with ' are not legal in Isabelle. */
final class TVar private[pure] (val name: String, val index: Int, val sort: List[String], initialMlValue: MLValue[Typ]=null)
                               (implicit val isabelle: Isabelle) extends ConcreteTyp(initialMlValue) {
  override protected def computeMlValue : MLValue[Typ] = Ops.makeTVar(name,index,sort)
  override def toString: String = sort match {
    case List(clazz) => s"?$name$index::$clazz"
    case _ => s"?$name$index::{${sort.mkString(",")}}"
  }

  override def concreteRecursive(implicit isabelle: Isabelle): this.type = this

  override def hashCode(): Int = new HashCodeBuilder(342524363,354523249)
    .append(name).append(index).append(sort).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit isabelle: Isabelle): Future[this.type] = Future.successful(this)
}

object TVar {
  /** Create a schematic type variable with name `name`, index `index`, and sort `sort`. */
  def apply(name: String, index: Int, sort: Seq[String])
           (implicit isabelle: Isabelle) = new TVar(name, index, sort.toList)

  /** Allows to pattern match schematic type variables. E.g.,
   * {{{
   *   typ match {
   *     case TVar(name,index,sort) => println(s"Schematic type variable ?\$name\$index found")
   *   }
   * }}}
   * Note that this will also match a [[Ctyp]] and an [[MLValueTyp]] that represent a `TVar` in ML.
   **/
  @tailrec
  def unapply(typ: Typ): Option[(String, Int, List[String])] = typ match {
    case typ : TVar => Some((typ.name,typ.index,typ.sort))
    case _ : MLValueTyp | _ : Ctyp => unapply(typ.concrete)
    case _ => None
  }
}

object Typ extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle): Ops = new Ops()
  protected[pure] class Ops(implicit val isabelle: Isabelle) {
    import MLValue.compileFunction
//    Context.init()
//    isabelle.executeMLCodeNow("exception E_Typ of typ;; exception E_Ctyp of ctyp") // ;; exception E_TypList of typ list

    val makeType: MLFunction2[String, List[Typ], Typ] =
      compileFunction("Term.Type")
    val makeTFree: MLFunction2[String, List[String], Typ] =
      compileFunction("Term.TFree")
    val makeTVar: MLFunction3[String, Int, List[String], Typ] =
      compileFunction("fn (n,i,s) => TVar ((n,i),s)")


    val readType: MLFunction2[Context, String, Typ] =
      compileFunction("fn (ctxt, str) => Syntax.read_typ ctxt str")
    val stringOfType: MLFunction2[Context, Typ, String] =
      compileFunction("fn (ctxt, typ) => Syntax.pretty_typ ctxt typ |> Pretty.unformatted_string_of |> YXML.parse_body |> XML.content_of")
    val stringOfCtyp: MLFunction2[Context, Ctyp, String] =
      compileFunction("fn (ctxt, ctyp) => Thm.typ_of ctyp |> Syntax.pretty_typ ctxt |> Pretty.unformatted_string_of |> YXML.parse_body |> XML.content_of")
    val typOfCtyp : MLFunction[Ctyp, Typ] =
      compileFunction("Thm.typ_of")
    val ctypOfTyp : MLFunction2[Context, Typ, Ctyp] =
      compileFunction("fn (ctxt, typ) => Thm.ctyp_of ctxt typ")
    val ctypOfCtyp: MLFunction2[Context, Ctyp, Ctyp] =
      compileFunction("""fn (ctxt, ctyp) => Thm.transfer_ctyp (Proof_Context.theory_of ctxt) ctyp
          handle Thm.CONTEXT _ => Thm.ctyp_of ctxt (Thm.typ_of ctyp)""")

    val destTyp : MLRetrieveFunction[Typ] =
      MLRetrieveFunction(
        """fn Type(name,args) => DList (DInt 1 :: DString name :: map (DObject o E_Typ) args)
            | TFree(name,sort) => DList (DInt 2 :: DString name :: map DString sort)
            | TVar((name,index),sort) => DList (DInt 3 :: DString name :: DInt index :: map DString sort)""")

    var equalsTyp: MLFunction2[Typ, Typ, Boolean] =
      compileFunction("op=")
  }

  /** Creates an Isabelle type from a string (using the parser from Isabelle).
   * E.g., `Typ(context, "nat list")`.
   *
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   * @param symbols Instance of [[misc.Symbols Symbols]] to convert `string` to Isabelle's internal encoding
   **/
  def apply(context: Context, string: String, symbols : Symbols = Symbols.globalInstance)(implicit isabelle: Isabelle): MLValueTyp = {
    new MLValueTyp(Ops.readType(context, symbols.unicodeToSymbols(string)))
  }

  /** Representation of types in ML.
   *
   *  - ML type: `typ`
   *  - Representation of typ `t` as an exception: `E_Typ t`
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object TypConverter extends Converter[Typ] {
    override def retrieve(value: MLValue[Typ])(implicit isabelle: Isabelle): Future[Typ] =
      Future.successful(new MLValueTyp(initialMLValue = value))
    override def store(value: Typ)(implicit isabelle: Isabelle): MLValue[Typ] =
      value.mlValue
    override def exnToValue(implicit isabelle: Isabelle): String = "fn E_Typ typ => typ"
    override def valueToExn(implicit isabelle: Isabelle): String = "E_Typ"

    override def mlType(implicit isabelle: Isabelle): String = "typ"
  }
}

