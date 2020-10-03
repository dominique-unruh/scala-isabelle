package de.unruh.isabelle.java

import de.unruh.isabelle.pure.{Abs, App, Bound, Const, Free, Term, Typ, Type, Var}
import de.unruh.javapatterns.Patterns.Is
import de.unruh.javapatterns.{MatchManager, Pattern, PatternMatchReject}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.annotation.varargs

// DOCUMENT
object JPatterns {
  type P[A] = Pattern[_ >: A]

  @NotNull def Const(@NotNull name: P[String], @NotNull typ: P[Typ]): Pattern[Term] = new Pattern[Term]() {
    @throws[PatternMatchReject]
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Const(n, t) =>
        name(mgr, n)
        typ(mgr, t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Const($name,$typ)"
  }

  @NotNull def Const(@NotNull name: String, @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] = Const(Is(name), typ)

  @NotNull def App(@NotNull fun: Pattern[_ >: Term], @NotNull arg: Pattern[_ >: Term]): Pattern[Term] = new Pattern[Term]() {
    @throws[PatternMatchReject]
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case App(f, a) =>
        fun(mgr, f)
        arg(mgr, a)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"App($fun,$arg)"
  }

  @NotNull def Abs(@NotNull name: Pattern[_ >: String], @NotNull typ: Pattern[_ >: Typ], @NotNull body: Pattern[_ >: Term]): Pattern[Term] = new Pattern[Term]() {
    @throws[PatternMatchReject]
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Abs(n, t, b) =>
        name(mgr, n)
        typ(mgr, t)
        body(mgr, b)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Abs($name,$typ,$body)"
  }

  @NotNull def Abs(@NotNull name: String, @NotNull typ: Pattern[_ >: Typ], @NotNull body: Pattern[_ >: Term]): Pattern[Term] =
    Abs(Is(name), typ, body)

  @NotNull def Free(@NotNull name: P[String], @NotNull typ: P[Typ]): Pattern[Term] = new Pattern[Term] {
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Free(n, t) =>
        name(mgr, n)
        typ(mgr, t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Free($name,$typ)"
  }

  @NotNull def Free(@NotNull name: String, @NotNull typ: P[Typ]): Pattern[Term] = Free(Is(name), typ)

  @NotNull def Var(@NotNull name: P[String], @NotNull index: P[Int], @NotNull typ: P[Typ]): Pattern[Term] = new Pattern[Term] {
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Var(n, i, t) =>
        name(mgr, n)
        index(mgr, i)
        typ(mgr, t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Var($name,$index,$typ)"
  }

  @NotNull def Bound(@NotNull index: P[Int]): Pattern[Term] = new Pattern[Term] {
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Bound(i) =>
        index(mgr, i)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Bound$index"
  }

  @NotNull def Bound(index: Int): Pattern[Term] = Bound(Is(index))

  @NotNull @varargs def Type(@NotNull name: P[String], @NotNull args: P[Typ]*): Pattern[Typ] = new Pattern[Typ] {
    override def apply(@NotNull mgr: MatchManager, @Nullable typ: Typ): Unit = typ match {
      case Type(n, a@_*) =>
        if (a.length != args.length) Pattern.reject()
        name(mgr, n)
        for ((a1, args1) <- a.zip(args))
          args1(mgr, a1)
    }

    override def toString: String = s"Type($name,${args.mkString(",")})"
  }

  @NotNull @varargs def Type(@NotNull name: String, @NotNull args: P[Typ]*): Pattern[Typ] =
    Type(Is(name), args: _*)

  @NotNull def Type(@NotNull name: P[String], @NotNull args: P[Array[Typ]]): Pattern[Typ] = new Pattern[Typ] {
    override def apply(@NotNull mgr: MatchManager, @Nullable typ: Typ): Unit = typ match {
      case Type(n, a@_*) =>
        name(mgr, n)
        args(mgr, a.toArray)
    }

    override def toString: String = s"Type($name,$args*)"
  }
}
