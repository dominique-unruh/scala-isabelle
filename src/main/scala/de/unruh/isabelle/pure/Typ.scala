package de.unruh.isabelle.pure

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DObject, DString}
import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{FutureValue, MLFunction, MLFunction2, MLFunction3, MLRetrieveFunction, MLValue}
import de.unruh.isabelle.pure.Typ.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

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
sealed abstract class Typ extends FutureValue {
  /** Transforms this type into an [[mlvalue.MLValue MLValue]] containing this type. This causes transfer of
   * the type to Isabelle only the first time it is accessed (and not at all if the type
   * came from the Isabelle process in the first place). */
  val mlValue : MLValue[Typ]
  /** [[control.Isabelle Isabelle]] instance relative to which this type was constructed. */
  implicit val isabelle : Isabelle
  /** Produces a string representation of this type. Uses the Isabelle pretty printer.
   * @param ctxt The Isabelle proof context to use (this contains syntax declarations etc.) */
  def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfType(MLValue((ctxt, this))).retrieveNow
  /** Transforms this term into a [[ConcreteTyp]]. A [[ConcreteTyp]] guarantees
   * that the Scala-type of the [[Typ]] ([[Type]],[[TFree]],[[TVar]]) corresponds to the top-level
   * constructor on Isabelle side (`Type`, `TFree`, `TVar`). */
  val concrete : Typ

  /** `t -->: u` is shorthand for `Type("fun", t, u)`, i.e., for a function from `t` to `u`. */
  def -->:(that: Typ)(implicit ec: ExecutionContext): Type = Type("fun", that, this)

  /** Hash code compatible with [[equals]]. May fail with an exception, see [[equals]]. */
  override def hashCode(): Int = throw new NotImplementedError("Should be overridden")

  /** Equality of types. Returns true iff the two [[Typ]] instances represent the same type in
   * the Isabelle process. (E.g., a [[Ctyp]] and a [[TFree]] can be equal.) May throw an exception
   * if the computation of the terms fails. (But will not fail if [[await]] or a related
   * [[mlvalue.FutureValue FutureValue]] method has returned successfully on both terms.)
   */
  override def equals(that: Any): Boolean = (this, that) match {
    case (t1, t2: AnyRef) if t1 eq t2 => true
    case (t1: Type, t2: Type) => t1.name == t2.name && t1.args == t2.args
    case (t1: TVar, t2: TVar) => t1.name == t2.name && t1.index == t2.index && t1.sort == t2.sort
    case (t1: TFree, t2: TFree) => t1.name == t2.name && t1.sort == t2.sort
    case (t1: Ctyp, t2: Ctyp) =>
      if (Await.result(t1.ctypMlValue.id, Duration.Inf) == Await.result(t2.ctypMlValue.id, Duration.Inf)) true
      else t1.mlValueTyp == t2.mlValueTyp
    case (t1: Ctyp, t2: Typ) => t1.mlValueTyp == t2
    case (t1: Typ, t2: Ctyp) => t1 == t2.mlValueTyp
    case (t1: MLValueTyp, t2: MLValueTyp) =>
      import ExecutionContext.Implicits.global
      if (Await.result(t1.mlValue.id, Duration.Inf) == Await.result(t2.mlValue.id, Duration.Inf)) true
      else if (t1.concreteComputed && t2.concreteComputed) t1.concrete == t2.concrete
      else Ops.equalsTyp(t1,t2).retrieveNow
    case (t1: MLValueTyp, t2: Typ) =>
      import ExecutionContext.Implicits.global
      if (t1.concreteComputed) t1.concrete == t2
      else Ops.equalsTyp(t1,t2).retrieveNow
    case (t1: Typ, t2: MLValueTyp) =>
      import ExecutionContext.Implicits.global
      if (t2.concreteComputed) t1 == t2.concrete
      else Ops.equalsTyp(t1,t2).retrieveNow
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
sealed abstract class ConcreteTyp extends Typ {
  override val concrete: this.type = this
}

/** A [[Typ]] that is stored in the Isabelle process's object store
 * and may or may not be known in Scala. Use [[concrete]] to
 * get a representation of the same type as a [[ConcreteTyp]]. */
final class MLValueTyp(val mlValue: MLValue[Typ])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Typ {
  def concreteComputed: Boolean = concreteLoaded
  /** Indicates whether [[concrete]] has already been initialized. (I.e.,
   * whether it can be accessed without delay and without incurring communication with
   * the Isabelle process. */
  @volatile private var concreteLoaded = false

  lazy val concrete : ConcreteTyp = {
    val DList(DInt(constructor), data @_*) = Await.result(Ops.destTyp(mlValue), Duration.Inf)
    val typ = (constructor,data) match {
      case (1,List(DString(name), args@_*)) =>
        val args2 = args.map { case DObject(id) => MLValue.unsafeFromId[Typ](id).retrieveNow }.toList
        new Type(name, args2, mlValue)
      case (2,List(DString(name), sort@_*)) =>
        val sort2 = sort.map { case DString(clazz) => clazz }.toList
        new TFree(name,sort2,mlValue)
      case (3,List(DString(name), DInt(index), sort@_*)) =>
        val sort2 = sort.map { case DString(clazz) => clazz }.toList
        new TVar(name,index.toInt,sort2,mlValue)
    }
    concreteLoaded = true
    typ
  }

  override def hashCode(): Int = concrete.hashCode()

  override def toString: String =
    if (concreteLoaded) concrete.toString
    else s"‹term${mlValue.stateString}›"

  override def someFuture: Future[Any] = mlValue.someFuture
  override def await: Unit = mlValue.await
}

/** Represents a `ctyp` in Isabelle. In Isabelle, a `ctyp` must be explicitly converted into a `typ`. In contrast,
 * this class inherits from [[Typ]], so no explicit conversions are needed. (They happen automatically on
 * demand.)
 * A [[Ctyp]] is always well-formed relative to the context for which it was
 * created (this is ensured by the Isabelle trusted core).
 **/final class Ctyp private(val ctypMlValue: MLValue[Ctyp])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Typ {
  /** Returns this term as an `MLValue[Typ]` (not `MLValue[Ctyp]`). The difference is crucial
   * because `MLValue[_]` is not covariant. So for invoking ML functions that expect an argument of type `typ`, you
   * need to get an `MLValue[Typ]`. In contrast, [[ctypMlValue]] returns this type as an `MLValue[Ctyp]`. */
  override lazy val mlValue: MLValue[Typ] = {
    val result = Ops.typOfCtyp(ctypMlValue)
    mlValueLoaded = true
    result
  }
  private var mlValueLoaded = false
  /** Transforms this [[Ctyp]] into an [[MLValueTyp]]. */
  private [pure] def mlValueTyp = new MLValueTyp(mlValue)
  override def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfCtyp(MLValue((ctxt, this))).retrieveNow
  lazy val concrete: ConcreteTyp = new MLValueTyp(mlValue).concrete
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
           (implicit isabelle: Isabelle, executionContext: ExecutionContext) =
    new Ctyp(mlValue)

  /** Converts a [[Typ]] into a [[Ctyp]]. This involves type-checking (relative to the
   * context `ctxt`). The resulting [[Ctyp]] is then certified to be correctly formed. */
  // TODO: if the Ctyp is constructed this way, then .mlValue should not involve a query to Isabelle because we already have the Typ. (Same for Cterm.)
  def apply(ctxt: Context, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) : Ctyp =
    new Ctyp(Ops.ctypOfTyp(MLValue((ctxt, typ))))

  /** Representation of ctyps in ML.
   *
   *  - ML type: `ctyp`
   *  - Representation of term `t` as an exception: `E_Ctyp t`
   *
   * (`E_Ctyp` is automatically declared when needed by the ML code in this package.
   * If you need to ensure that it is defined for compiling own ML code, invoke [[Typ.init]].)
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object CtypConverter extends Converter[Ctyp] {
    override def store(value: Ctyp)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Ctyp] =
      value.ctypMlValue
    override def retrieve(value: MLValue[Ctyp])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Ctyp] =
      for (_ <- value.id)
        yield new Ctyp(ctypMlValue = value)
    override lazy val exnToValue: String = "fn (E_Ctyp t) => t"
    override lazy val valueToExn: String = "E_Ctyp"

    override def mlType: String = "ctyp"
  }
}

/** A type constructor (ML constructor `Type`). [[name]] is the fully qualified name of the type constructor (e.g.,
 * `"List.list"`) and [[args]] its type parameters. */
final class Type private[pure](val name: String, val args: List[Typ], val initialMlValue: MLValue[Typ]=null)
                              (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTyp {
  lazy val mlValue : MLValue[Typ] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeType(MLValue(name,args))
  override def toString: String =
    if (args.isEmpty) name
    else s"$name(${args.mkString(", ")})"

  override def hashCode(): Int = new HashCodeBuilder(342534543,34774653)
    .append(name).toHashCode

  override def await: Unit = Await.ready(someFuture, Duration.Inf)
  override lazy val someFuture: Future[Any] = {
    Future.traverse(args : Seq[Typ])(_.someFuture).map(_ => ())
  }
}

object Type {
  /** Create a type with type constructor `name` and type parameters `args`. */
  def apply(name: String, args: Typ*)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Type(name, args.toList)

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
final class TFree private[pure] (val name: String, val sort: List[String], val initialMlValue: MLValue[Typ]=null)
                                (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTyp {
  lazy val mlValue : MLValue[Typ] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeTFree(name, sort)
  override def toString: String = sort match {
    case List(clazz) => s"$name::$clazz"
    case _ => s"$name::{${sort.mkString(",")}}"
  }

  override def hashCode(): Int = new HashCodeBuilder(335434265,34255633)
    .append(name).append(sort).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit ec: ExecutionContext): Future[this.type] = Future.successful(this)
}

object TFree {
  /** Create a free type variable with name `name` and sort `sort`. */
  def apply(name: String, sort: Seq[String])
           (implicit isabelle: Isabelle, ec: ExecutionContext) = new TFree(name, sort.toList)

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
final class TVar private[pure] (val name: String, val index: Int, val sort: List[String], val initialMlValue: MLValue[Typ]=null)
                               (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTyp {
  lazy val mlValue : MLValue[Typ] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeTVar(name,index,sort)
  override def toString: String = sort match {
    case List(clazz) => s"?$name$index::$clazz"
    case _ => s"?$name$index::{${sort.mkString(",")}}"
  }

  override def hashCode(): Int = new HashCodeBuilder(342524363,354523249)
    .append(name).append(index).append(sort).toHashCode

  override def someFuture: Future[Any] = Future.successful(())
  override def await: Unit = {}
  override def forceFuture(implicit ec: ExecutionContext): Future[this.type] = Future.successful(this)
}

object TVar {
  /** Create a schematic type variable with name `name`, index `index`, and sort `sort`. */
  // TODO Add default 0 for index
  def apply(name: String, index: Int, sort: Seq[String])
           (implicit isabelle: Isabelle, ec: ExecutionContext) = new TVar(name, index, sort.toList)

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
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  protected[pure] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.compileFunction
    Context.init()
    isabelle.executeMLCodeNow("exception E_Typ of typ;; exception E_Ctyp of ctyp") // ;; exception E_TypList of typ list

    val makeType: MLFunction2[String, List[Typ], Typ] =
      compileFunction("Term.Type")
    val makeTFree: MLFunction2[String, List[String], Typ] =
      compileFunction("Term.TFree")
    val makeTVar: MLFunction3[String, Int, List[String], Typ] =
      compileFunction("fn (n,i,s) => TVar ((n,i),s)")


    val readType: MLFunction2[Context, String, Typ] =
      compileFunction("fn (ctxt, str) => Syntax.read_typ ctxt str")
    val stringOfType: MLFunction2[Context, Typ, String] =
      compileFunction("fn (ctxt, typ) => Syntax.pretty_typ ctxt typ |> Pretty.unformatted_string_of |> YXML.content_of")
    val stringOfCtyp: MLFunction2[Context, Ctyp, String] =
      compileFunction("fn (ctxt, ctyp) => Thm.typ_of ctyp |> Syntax.pretty_typ ctxt |> Pretty.unformatted_string_of |> YXML.content_of")
    val typOfCtyp : MLFunction[Ctyp, Typ] =
      compileFunction("Thm.typ_of")
    val ctypOfTyp : MLFunction2[Context, Typ, Ctyp] =
      compileFunction("fn (ctxt, typ) => Thm.ctyp_of ctxt typ")

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
   * @param context The context relative to which parsing takes place (contains syntax declarations etc.)
   * @param string The string to be parsed
   **/
  def apply(context: Context, string: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValueTyp = {
    new MLValueTyp(Ops.readType(MLValue((context, string))))
  }

  /** Representation of types in ML.
   *
   *  - ML type: `typ`
   *  - Representation of typ `t` as an exception: `E_Typ t`
   *
   * (`E_Tyo` is automatically declared when needed by the ML code in this package.
   * If you need to ensure that it is defined for compiling own ML code, invoke [[Typ.init]].)
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object TypConverter extends Converter[Typ] {
    override def retrieve(value: MLValue[Typ])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Typ] =
      Future.successful(new MLValueTyp(mlValue = value))
    override def store(value: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Typ] =
      value.mlValue
    override lazy val exnToValue: String = "fn E_Typ typ => typ"
    override lazy val valueToExn: String = "E_Typ"

    override def mlType: String = "typ"
  }
}

