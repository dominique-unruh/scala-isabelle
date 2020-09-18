package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.MLValue.Implicits._
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLValue}
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.pure.Term.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

// TODO document
sealed abstract class Term {
  val mlValue : MLValue[Term]
  implicit val isabelle : Isabelle
  def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfTerm(MLValue((ctxt, this))).retrieveNow
  val concrete : Term
  def $(that: Term)(implicit ec: ExecutionContext): Term = App(this, that)

  override def hashCode(): Int = throw new NotImplementedError("Should be overridden")

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
}

final class Cterm private(val ctermMlValue: MLValue[Cterm])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  override lazy val mlValue: MLValue[Term] = Ops.termOfCterm(ctermMlValue)
  def mlValueTerm = new MLValueTerm(mlValue)
  override def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfCterm(MLValue((ctxt, this))).retrieveNow
  lazy val concrete: Term = new MLValueTerm(mlValue).concrete
  override def hashCode(): Int = concrete.hashCode
}

object Cterm {
  def apply(mlValue: MLValue[Cterm])
           (implicit isabelle: Isabelle, executionContext: ExecutionContext) =
    new Cterm(mlValue)

  def apply(ctxt: Context, term: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) : Cterm =
    new Cterm(Ops.ctermOfTerm(MLValue((ctxt, term))))

  object CtermConverter extends Converter[Cterm] {
    override def store(value: Cterm)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Cterm] =
      value.ctermMlValue
    override def retrieve(value: MLValue[Cterm])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Cterm] =
      for (_ <- value.id)
        yield new Cterm(ctermMlValue = value)
    override lazy val exnToValue: String = "fn (E_Cterm t) => t"
    override lazy val valueToExn: String = "E_Cterm"

    override def mlType: String = "cterm"
  }
}

final class MLValueTerm(val mlValue: MLValue[Term])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  @inline private def await[A](awaitable: Awaitable[A]) : A = Await.result(awaitable, Duration.Inf)

  //noinspection EmptyParenMethodAccessedAsParameterless
  override def hashCode(): Int = concrete.hashCode

  def concreteComputed: Boolean = concreteLoaded
  @volatile private var concreteLoaded = false
  lazy val concrete : Term = {
    val term = Ops.whatTerm(mlValue).retrieveNow match {
      case 1 => // Const
        val (name,typ) = Ops.destConst(mlValue).retrieveNow
        Const(name, typ)
      case 2 => // Free
        val (name,typ) = Ops.destFree(mlValue).retrieveNow
        Free(name, typ)
      case 3 =>
        val (name, index, typ) = Ops.destVar(mlValue).retrieveNow
        Var(name, index, typ)
      case 4 =>
        val index = Ops.destBound(mlValue).retrieveNow
        Bound(index)
      case 5 =>
        val (name,typ,body) = Ops.destAbs(mlValue).retrieveNow
        Abs(name,typ,body)
      case 6 =>
        val (t1,t2) = Ops.destApp(this.mlValue).retrieveNow
        t1 $ t2
    }
    concreteLoaded = true
    term
  }

  override def toString: String =
    if (concreteLoaded) concrete.toString
    else s"‹term${mlValue.stateString}›"
}

final class Const private[isabelle](val name: String, val typ: Typ, val initialMlValue: MLValue[Term]=null)
                                   (implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeConst(MLValue((name,typ)))
  @inline override val concrete: Const = this
  override def toString: String = name

  override def hashCode(): Int = new HashCodeBuilder(162389433,568734237)
    .append(name).toHashCode
}

object Const {
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Const(name, typ)

  @tailrec
  def unapply(term: Term): Option[(String, Typ)] = term match {
    case const : Const => Some((const.name,const.typ))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

final class Free private[isabelle](val name: String, val typ: Typ, val initialMlValue: MLValue[Term]=null)
                                  (implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeFree(name, typ)
  @inline override val concrete: Free = this
  override def toString: String = name

  override def hashCode(): Int = new HashCodeBuilder(384673423,678423475)
    .append(name).toHashCode
}

object Free {
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Free(name, typ)

  @tailrec
  def unapply(term: Term): Option[(String, Typ)] = term match {
    case free : Free => Some((free.name,free.typ))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

final class Var private(val name: String, val index: Int, val typ: Typ, val initialMlValue: MLValue[Term]=null)
                       (implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeVar(name, index, typ)
  @inline override val concrete: Var = this
  override def toString: String = s"?$name$index"

  override def hashCode(): Int = new HashCodeBuilder(3474285, 342683425)
    .append(name).append(index).toHashCode
}

object Var {
  def apply(name: String, index: Int, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Var(name, index, typ)

  @tailrec
  def unapply(term: Term): Option[(String, Int, Typ)] = term match {
    case v : Var => Some((v.name,v.index,v.typ))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

final class App private (val fun: Term, val arg: Term, val initialMlValue: MLValue[Term]=null)
                        (implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeApp(fun,arg)

  @inline override val concrete: App = this
  override def toString: String = s"($fun $$ $arg)"

  override def hashCode(): Int = new HashCodeBuilder(334234237,465634533)
    .append(arg).toHashCode
}

object App {
  def apply(fun: Term, arg: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) = new App(fun, arg)

  @tailrec
  def unapply(term: Term): Option[(Term, Term)] = term match {
    case app : App => Some((app.fun,app.arg))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

final class Abs private (val name: String, val typ: Typ, val body: Term, val initialMlValue: MLValue[Term]=null)
                        (implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeAbs(name,typ,body)
  @inline override val concrete: Abs = this
  override def toString: String = s"(λ$name. $body)"

  override def hashCode(): Int = new HashCodeBuilder(342345635,564562379)
    .append(name).append(body).toHashCode
}

object Abs {
  def apply(name: String, typ: Typ, body: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Abs(name,typ,body)

  @tailrec
  def unapply(term: Term): Option[(String, Typ, Term)] = term match {
    case abs : Abs => Some((abs.name,abs.typ,abs.body))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}


final class Bound private (val index: Int, val initialMlValue: MLValue[Term]=null)
                          (implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeBound(index)
  @inline override val concrete: Bound = this
  override def toString: String = s"Bound $index"

  override def hashCode(): Int = new HashCodeBuilder(442344345,423645769)
    .append(index).toHashCode
}

object Bound {
  def apply(index: Int)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Bound(index)

  @tailrec
  def unapply(term: Term): Option[Int] = term match {
    case bound : Bound => Some(bound.index)
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}



object Term extends OperationCollection {
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  protected[isabelle] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.{compileFunction, compileFunctionRaw}
    Typ.init()
    isabelle.executeMLCodeNow("exception E_Term of term;; exception E_Cterm of cterm")

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
    val whatTerm : MLFunction[Term, Int] =
      compileFunctionRaw("fn (E_Term term) => (case term of Const _ => 1 | Free _ => 2 | Var _ => 3 | Bound _ => 4 | Abs _ => 5 | _ $ _ => 6) |> E_Int")
    val destConst : MLFunction[Term, (String,Typ)] = MLValue.compileFunction("fn Const x => x")
    val destFree : MLFunction[Term, (String,Typ)] = MLValue.compileFunction("fn Free x => x")
    val destVar : MLFunction[Term, (String,Int,Typ)] = MLValue.compileFunction("fn Var ((n,i),s) => (n,i,s)")
    val destBound : MLFunction[Term, Int] = MLValue.compileFunction("fn Bound x => x")
    val destAbs : MLFunction[Term, (String,Typ,Term)] = MLValue.compileFunction("fn Abs x => x")
    val destApp: MLFunction[Term, (Term,Term)] = MLValue.compileFunction("Term.dest_comb")
    val makeConst : MLFunction2[String, Typ, Term] = MLValue.compileFunction("Const")
    val makeFree : MLFunction2[String, Typ, Term] = MLValue.compileFunction("Free")
    val makeVar : MLFunction3[String, Int, Typ, Term] = MLValue.compileFunction("fn (n,i,s) => Var ((n,i),s)")
    val makeApp : MLFunction2[Term, Term, Term] = MLValue.compileFunction("op$")
    val makeBound : MLFunction[Int, Term] = MLValue.compileFunction("Bound")
    val makeAbs : MLFunction3[String, Typ, Term, Term] = MLValue.compileFunction("Abs")
  }

  def apply(context: Context, string: String)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValueTerm = {
    new MLValueTerm(Ops.readTerm(MLValue((context, string))))
  }

  def apply(context: Context, string: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValueTerm = {
    new MLValueTerm(Ops.readTermConstrained(MLValue((context, string, typ))))
  }

  object TermConverter extends Converter[Term] {
    override def store(value: Term)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Term] =
      value.mlValue
    override def retrieve(value: MLValue[Term])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Term] =
      for (_ <- value.id)
        yield new MLValueTerm(mlValue = value)
    override lazy val exnToValue: String = "fn (E_Term t) => t"
    override lazy val valueToExn: String = "E_Term"

    override def mlType: String = "term"
  }
}
