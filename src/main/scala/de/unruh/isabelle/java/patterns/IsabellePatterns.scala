package de.unruh.isabelle.java.patterns

import de.unruh.isabelle.java.patterns.Patterns.Is
import de.unruh.isabelle.pure._

import scala.annotation.varargs

object IsabellePatterns {
  type P[A] = Pattern[_ >: A]

  def Const(name: P[String], typ: P[Typ]): Pattern[Term] = new Pattern[Term]() {
    @throws[PatternMatchReject]
    override def apply(mgr: MatchManager, term: Term): Unit = term match {
      case Const(n, t) =>
        name(mgr, n)
        typ(mgr, t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Const($name,$typ)"
  }
  def Const(name: String, typ: Pattern[_ >: Typ]): Pattern[Term] = Const(Is(name), typ)

  def App(fun: Pattern[_ >: Term], arg: Pattern[_ >: Term]): Pattern[Term] = new Pattern[Term]() {
    @throws[PatternMatchReject]
    override def apply(mgr: MatchManager, term: Term): Unit = term match {
      case App(f,a) =>
        fun(mgr, f)
        arg(mgr, a)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"App($fun,$arg)"
  }

  def Abs(name: Pattern[_ >: String], typ: Pattern[_ >: Typ], body: Pattern[_ >: Term]): Pattern[Term] = new Pattern[Term]() {
    @throws[PatternMatchReject]
    override def apply(mgr: MatchManager, term: Term): Unit = term match {
      case Abs(n, t, b) =>
        name(mgr, n)
        typ(mgr, t)
        body(mgr, b)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Abs($name,$typ,$body)"
  }
  def Abs(name: String, typ: Pattern[_ >: Typ], body: Pattern[_ >: Term]): Pattern[Term] =
    Abs(Is(name), typ, body)

  def Free(name: P[String], typ: P[Typ]): Pattern[Term] = new Pattern[Term] {
    override def apply(mgr: MatchManager, term: Term): Unit = term match {
      case Free(n,t) =>
        name(mgr,n)
        typ(mgr,t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Free($name,$typ)"
  }
  def Free(name: String, typ: P[Typ]): Pattern[Term] = Free(Is(name), typ)

  def Var(name: P[String], index: P[Int], typ: P[Typ]): Pattern[Term] = new Pattern[Term] {
    override def apply(mgr: MatchManager, term: Term): Unit = term match {
      case Var(n,i,t) =>
        name(mgr,n)
        index(mgr,i)
        typ(mgr,t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Var($name,$index,$typ)"
  }

  def Bound(index: P[Int]): Pattern[Term] = new Pattern[Term] {
    override def apply(mgr: MatchManager, term: Term): Unit = term match {
      case Bound(i) =>
        index(mgr,i)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Bound$index"
  }
  def Bound(index: Int): Pattern[Term] = Bound(Is(index))

  @varargs def Type(name: P[String], args: P[Typ]*): Pattern[Typ] = new Pattern[Typ] {
    override def apply(mgr: MatchManager, typ: Typ): Unit = typ match {
      case Type(n, a @_*) =>
        if (a.length != args.length) Pattern.reject()
        name(mgr,n)
        for ((a1,args1) <- a.zip(args))
          args1(mgr,a1)
    }

    override def toString: String = s"Type($name,${args.mkString(",")})"
  }

  @varargs def Type(name: String, args: P[Typ]*): Pattern[Typ] =
    Type(Is(name), args : _*)

  def Type(name: P[String], args: P[Array[Typ]]): Pattern[Typ] = new Pattern[Typ] {
    override def apply(mgr: MatchManager, typ: Typ): Unit = typ match {
      case Type(n, a @_*) =>
        name(mgr,n)
        args(mgr, a.toArray)
    }

    override def toString: String = s"Type($name,$args*)"
  }
}
