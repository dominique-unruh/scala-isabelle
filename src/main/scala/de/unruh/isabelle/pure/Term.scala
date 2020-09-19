package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLValue}
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.pure.Term.Ops
import org.apache.commons.lang3.builder.HashCodeBuilder

import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

// TODO: for occurrences of "private[isabelle]", check what should be written instead

/**
 * This class represents a term (ML type `term`) in Isabelle. It can be transferred to and from the Isabelle process
 * via the [[mlvalue.MLValue MLValue]] mechanism (see below). (TODO write)
 *
 * In most respects, [[Term]] behaves as if it was an algebraic datatype defined as follows:
 * {{{
 *   sealed abstract class Term
 *   final case class Const(name: String, typ: Typ)                        // Corresponds to ML constructor 'Const'
 *   final case class Free(name: String, typ: Typ)                         // Corresponds to ML constructor 'Free'
 *   final case class Var(val name: String, val index: Int, val typ: Typ)  // Corresponds to ML constructor 'Var'
 *   final case class Abs(val name: String, val typ: Typ, val body: Term)  // Corresponds to ML constructor 'Abs'
 *   final case class Bound private (val index: Int)                       // Corresponds to ML constructor 'Bound'
 *   final case class App private (val fun: Term, val arg: Term)           // Corresponds to ML constructor '$'
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
 * and then transfer `App(t,u)` back to Isabelle, the processes `t`,`u` will never be serialized, and only the
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
 * Two terms are equal (w.r.t.~the [[AnyRef.equals equals]] method) iff they represent the same Isabelle terms. I.e.,
 * an [[MLValueTerm]] and a [[Const]] can be equal. (Equality tests try to transfer as little data as possible when
 * determining equality.)
 *
 * Furthermore, there is a subclass [[Cterm]] of [[Term]] that represents an ML value of type `cterm`. This is
 * logically a term with additional that is certified to be well-typed with respect to some Isabelle context.
 * In this implementation, a [[Cterm]] is also a [[Term]], so it is possible to do, e.g., equality tests between
 * [[Cterm]]s and regular terms (such as [[Const]]) without explicit conversions. Similarly, patterns such as
 * `case Const(name,typ) =>` also match [[Cterm]]s.
 */
sealed abstract class Term {
  val mlValue : MLValue[Term]
  implicit val isabelle : Isabelle
  def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfTerm(MLValue((ctxt, this))).retrieveNow
  val concrete : ConcreteTerm
  def $(that: Term)(implicit ec: ExecutionContext): App = App(this, that)

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
sealed abstract class ConcreteTerm extends Term {
  @inline override val concrete: this.type = this
}

// TODO document
final class Cterm private(val ctermMlValue: MLValue[Cterm])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  override lazy val mlValue: MLValue[Term] = Ops.termOfCterm(ctermMlValue)
  def mlValueTerm = new MLValueTerm(mlValue)
  override def pretty(ctxt: Context)(implicit ec: ExecutionContext): String =
    Ops.stringOfCterm(MLValue((ctxt, this))).retrieveNow
  lazy val concrete: ConcreteTerm = new MLValueTerm(mlValue).concrete
  override def hashCode(): Int = concrete.hashCode
}

// TODO document
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

// TODO document
final class MLValueTerm(val mlValue: MLValue[Term])(implicit val isabelle: Isabelle, ec: ExecutionContext) extends Term {
  @inline private def await[A](awaitable: Awaitable[A]) : A = Await.result(awaitable, Duration.Inf)

  //noinspection EmptyParenMethodAccessedAsParameterless
  override def hashCode(): Int = concrete.hashCode

  def concreteComputed: Boolean = concreteLoaded
  @volatile private var concreteLoaded = false
  lazy val concrete : ConcreteTerm = {
    val term : ConcreteTerm = Ops.whatTerm(mlValue).retrieveNow match {
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

// TODO document
final class Const private[isabelle](val name: String, val typ: Typ, initialMlValue: MLValue[Term]=null)
                                   (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeConst(MLValue((name,typ)))
  override def toString: String = name

  override def hashCode(): Int = new HashCodeBuilder(162389433,568734237)
    .append(name).toHashCode
}

// TODO document
object Const {
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Const(name, typ)

  @tailrec
  def unapply(term: Term): Option[(String, Typ)] = term match {
    case const : Const => Some((const.name,const.typ))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

// TODO document
final class Free private[isabelle](val name: String, val typ: Typ, initialMlValue: MLValue[Term]=null)
                                  (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeFree(name, typ)
  override def toString: String = name

  override def hashCode(): Int = new HashCodeBuilder(384673423,678423475)
    .append(name).toHashCode
}

// TODO document
object Free {
  def apply(name: String, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Free(name, typ)

  @tailrec
  def unapply(term: Term): Option[(String, Typ)] = term match {
    case free : Free => Some((free.name,free.typ))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

// TODO document
final class Var private(val name: String, val index: Int, val typ: Typ, initialMlValue: MLValue[Term]=null)
                       (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeVar(name, index, typ)
  override def toString: String = s"?$name$index"

  override def hashCode(): Int = new HashCodeBuilder(3474285, 342683425)
    .append(name).append(index).toHashCode
}

// TODO document
object Var {
  def apply(name: String, index: Int, typ: Typ)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Var(name, index, typ)

  @tailrec
  def unapply(term: Term): Option[(String, Int, Typ)] = term match {
    case v : Var => Some((v.name,v.index,v.typ))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

// TODO document
final class App private (val fun: Term, val arg: Term, initialMlValue: MLValue[Term]=null)
                        (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeApp(fun,arg)

  override def toString: String = s"($fun $$ $arg)"

  override def hashCode(): Int = new HashCodeBuilder(334234237,465634533)
    .append(arg).toHashCode
}

// TODO document
object App {
  def apply(fun: Term, arg: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) = new App(fun, arg)

  @tailrec
  def unapply(term: Term): Option[(Term, Term)] = term match {
    case app : App => Some((app.fun,app.arg))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

// TODO document
final class Abs private (val name: String, val typ: Typ, val body: Term, initialMlValue: MLValue[Term]=null)
                        (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeAbs(name,typ,body)
  override def toString: String = s"(λ$name. $body)"

  override def hashCode(): Int = new HashCodeBuilder(342345635,564562379)
    .append(name).append(body).toHashCode
}

// TODO document
object Abs {
  def apply(name: String, typ: Typ, body: Term)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Abs(name,typ,body)

  @tailrec
  def unapply(term: Term): Option[(String, Typ, Term)] = term match {
    case abs : Abs => Some((abs.name,abs.typ,abs.body))
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}

// TODO document
final class Bound private (val index: Int, initialMlValue: MLValue[Term]=null)
                          (implicit val isabelle: Isabelle, ec: ExecutionContext) extends ConcreteTerm {
  lazy val mlValue : MLValue[Term] =
    if (initialMlValue!=null) initialMlValue
    else Ops.makeBound(index)
  override def toString: String = s"Bound $index"

  override def hashCode(): Int = new HashCodeBuilder(442344345,423645769)
    .append(index).toHashCode
}

// TODO document
object Bound {
  def apply(index: Int)(implicit isabelle: Isabelle, ec: ExecutionContext) = new Bound(index)

  @tailrec
  def unapply(term: Term): Option[Int] = term match {
    case bound : Bound => Some(bound.index)
    case term : MLValueTerm => unapply(term.concrete)
    case _ => None
  }
}



// TODO document
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
