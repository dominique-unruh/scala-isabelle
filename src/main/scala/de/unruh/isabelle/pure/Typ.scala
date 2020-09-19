package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLValue}
import de.unruh.isabelle.pure.Typ.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

// TODO Add ConcreteTyp
// TODO Add Ctyp

// TODO document
sealed abstract class Typ {
  val mlValue : MLValue[Typ]
  implicit val isabelle : Isabelle
  def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfType(MLValue((ctxt, this))).retrieveNow
  val concrete : Typ

  // Make a trait for force, forceFuture (something like LazyValue). Use it here and for Term, and MLValue
  def force : this.type

  def -->:(that: Typ)(implicit ec: ExecutionContext): Type = Type("fun", that, this)
//  def --->:(thats: List[Typ])(implicit ec: ExecutionContext): Typ = thats.foldRight(this)(_ -->: _)

  override def hashCode(): Int = throw new NotImplementedError("Should be overridden")

  // TODO: add Ctyp to the mix
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
}
sealed abstract class ConcreteTyp extends Typ {
  override val concrete: this.type = this
  override def force: ConcreteTyp.this.type = this
}

final class MLValueTyp(val mlValue: MLValue[Typ])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Typ {
  def concreteComputed: Boolean = concreteLoaded
  @volatile private var concreteLoaded = false

  lazy val concrete : ConcreteTyp = {
    val typ = Ops.whatTyp(mlValue).retrieveNow match {
      case 1 =>
        val (name,args) = Ops.destType(mlValue).retrieveNow
        new Type(name, args, mlValue)
      case 2 =>
        val (name,sort) = Ops.destTFree(mlValue).retrieveNow
        TFree(name,sort :_*)
      case 3 =>
        val (name,index,sort) = Ops.destTVar(mlValue).retrieveNow
        TVar(name,index,sort :_*)
    }
    concreteLoaded = true
    typ
  }

  override def hashCode(): Int = concrete.hashCode()

  override def toString: String =
    if (concreteLoaded) concrete.toString
    else s"‹term${mlValue.stateString}›"

  override def force: this.type = { mlValue.force; this }
}

// TODO document
final class Ctyp private(val ctypMlValue: MLValue[Ctyp])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Typ {
  override lazy val mlValue: MLValue[Typ] = Ops.typOfCtyp(ctypMlValue)
  def mlValueTyp = new MLValueTyp(mlValue)
  override def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfCtyp(MLValue((ctxt, this))).retrieveNow
  lazy val concrete: ConcreteTyp = new MLValueTyp(mlValue).concrete
  override def hashCode(): Int = concrete.hashCode()

  override def force: this.type = { ctypMlValue.force; this }
}

// TODO document
object Ctyp {
  def apply(mlValue: MLValue[Ctyp])
           (implicit isabelle: Isabelle, executionContext: ExecutionContext) =
    new Ctyp(mlValue)

  // TODO: if the Ctyp is constructed this way, then .mlValue should not involve a query to Isabelle because
  // we already have the Typ. (Same for Cterm.)
  def apply(ctxt: Context, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) : Ctyp =
    new Ctyp(Ops.ctypOfTyp(MLValue((ctxt, typ))))

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

final class Type private[isabelle](val name: String, val args: List[Typ], val initialMlValue: MLValue[Typ]=null)
                                  (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTyp {
  lazy val mlValue : MLValue[Typ] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeType(MLValue(name,args))
  override def toString: String =
    if (args.isEmpty) name
    else s"$name(${args.mkString(", ")})"

  override def hashCode(): Int = new HashCodeBuilder(342534543,34774653)
    .append(name).toHashCode
}

object Type {
  def apply(name: String, args: Typ*)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Type(name, args.toList)

  @tailrec
  def unapply(typ: Typ): Option[(String, List[Typ])] = typ match {
    case typ : Type => Some((typ.name,typ.args))
    case _ : MLValueTyp | _ : Ctyp => unapply(typ.concrete)
    case _ => None
  }
}

final class TFree private (val name: String, val sort: List[String], val initialMlValue: MLValue[Typ]=null)
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
}

object TFree {
  def apply(name: String, sort: String*)
           (implicit isabelle: Isabelle, ec: ExecutionContext) = new TFree(name, sort.toList)

  @tailrec
  def unapply(typ: Typ): Option[(String, List[String])] = typ match {
    case typ : TFree => Some((typ.name,typ.sort))
    case _ : MLValueTyp | _ : Ctyp => unapply(typ.concrete)
    case _ => None
  }
}

final class TVar private (val name: String, val index: Int, val sort: List[String], val initialMlValue: MLValue[Typ]=null)
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
}

object TVar {
  def apply(name: String, index: Int, sort: String*)
           (implicit isabelle: Isabelle, ec: ExecutionContext) = new TVar(name, index, sort.toList)

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

    // TODO: use updated Data mechanism instead
    val whatTyp: MLFunction[Typ, Int] =
      compileFunction("fn Type _ => 1 | TFree _ => 2 | TVar _ => 3")
    val destType: MLFunction[Typ, (String, List[Typ])] =
      compileFunction("Term.dest_Type")
    val destTFree: MLFunction[Typ, (String, List[String])] =
      compileFunction("Term.dest_TFree")
    val destTVar: MLFunction[Typ, (String, Int, List[String])] =
      compileFunction("fn TVar ((n,i),s) => (n,i,s)")

    var equalsTyp: MLFunction2[Typ, Typ, Boolean] =
      compileFunction("op=")
  }

  def apply(context: Context, string: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValueTyp = {
    new MLValueTyp(Ops.readType(MLValue((context, string))))
  }

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

