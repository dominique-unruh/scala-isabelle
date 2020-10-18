package de.unruh.isabelle.java

import de.unruh.isabelle.pure.{Abs, App, Bound, Const, Free, TFree, TVar, Term, Typ, Type, Var}
import de.unruh.javapatterns.Patterns.Is
import de.unruh.javapatterns.{MatchManager, Pattern, PatternMatchReject}
import org.jetbrains.annotations.{NotNull, Nullable}

import scala.annotation.varargs

/** Java support for pattern matching terms and types using the
 * [[https://search.maven.org/artifact/de.unruh/java-patterns java-patterns]] library.
 *
 * E.g., the following Java code will assign the name of the constant/free variable/schematic variable `term` to `name`:
 * {{{
 * import de.unruh.javapatterns.Capture;
 * import static de.unruh.javapatterns.Pattern.capture;
 * import static de.unruh.javapatterns.Patterns.Any;
 * import static de.unruh.javapatterns.Match.match;
 * import static de.unruh.isabelle.java.JPatterns.*;
 *
 * Capture<String> x = capture("x");
 * String name = match(term,
 *   Const(x,Any), () -> x,
 *   Free(x,Any), () -> x,
 *   Var(x,Any,Any), () -> x);
 * }}}
 *
 * See [[de.unruh.javapatterns.Match]] for general instructions how to do pattern matching using
 * the java-patterns library.
 *
 * Patterns for terms are [[JPatterns.Const(name:d* Const]], [[JPatterns.App App]],
 * [[JPatterns.Free(name:d* Free]],
 * [[JPatterns.Var(name:d* Var]], [[JPatterns.Abs(name:d* Abs]], [[JPatterns.Bound(index:d* Bound]]. Patterns for types
 * are [[JPatterns.Type(name:de\.unruh\.javapatterns\.Pattern[_>:String],args:de\.unruh\.javapatterns\.Pattern[_>:de\.unruh\.isabelle\.pure\.Typ]* Type]], [[JPatterns.TFree(name:d* TFree]], [[JPatterns.TVar(name:d* TVar]].
 * */
object JPatterns {
  /** Pattern matching a constant ([[pure.Const Const]]) `c`. Subpattern `name` will be applied to `c.`[[pure.Const.name name]],
   * and `typ` will be applied to `c.`[[pure.Const.typ typ]].
   **/
  @NotNull def Const(@NotNull name: Pattern[_ >: String], @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] = new Pattern[Term]() {
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

  /** Short for `Const(Is(name),typ)`. That is, matches a constant with name `name`. */
  @NotNull def Const(@NotNull name: String, @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] = Const(Is(name), typ)

  /** Pattern matching a function application term ([[pure.App App]]) `a`. Subpattern `fun` will be applied to
   * `a.`[[pure.App.fun fun]], and `arg` will be applied to `a.`[[pure.App.arg arg]].
   **/
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

  /** Pattern matching a lambda abstraction ([[pure.Abs Abs]]) `a`. Subpattern `name` will be applied to `a.`[[pure.Abs.name name]],
   * `typ` will be applied to `a.`[[pure.Abs.typ typ]],
   * and `body` will be applied to `a.`[[pure.Abs.body body]].
   **/
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

  /** Pattern matching a free variable ([[pure.Free Free]]) `v`. Subpattern `name` will be applied to `v.`[[pure.Free.name name]],
   * and `typ` will be applied to `v.`[[pure.Free.typ typ]].
   **/
  @NotNull def Free(@NotNull name: Pattern[_ >: String], @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] = new Pattern[Term] {
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Free(n, t) =>
        name(mgr, n)
        typ(mgr, t)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Free($name,$typ)"
  }

  /** Short for `Free(Is(name),typ)`. That is, matches a free variable with name `name`. */
  @NotNull def Free(@NotNull name: String, @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] = Free(Is(name), typ)

  /** Pattern matching a schematic variable ([[pure.Var Var]]) `v`. Subpattern `name` will be applied to `v.`[[pure.Var.name name]],
   * `index` will be applied to `v.`[[pure.Var.index index]],
   * and `typ` will be applied to `v.`[[pure.Free.typ typ]].
   **/
  @NotNull def Var(@NotNull name: Pattern[_ >: String], @NotNull index: Pattern[_ >: Int], @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] = new Pattern[Term] {
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

  /** Short for `Var(Is(name),Is(index),typ)`. That is, matches a schematic variable with name `name` and index `index`. */
  @NotNull def Var(@NotNull name: String, index: Int, @NotNull typ: Pattern[_ >: Typ]): Pattern[Term] =
    Var(Is(name), Is(index), typ)


  /** Pattern matching a bound (de Bruijn) variable ([[pure.Bound Bound]]) `v`. Subpattern `index` will be applied to
   * `v.`[[pure.Bound.index index]].
   **/
  @NotNull def Bound(@NotNull index: Pattern[_ >: Int]): Pattern[Term] = new Pattern[Term] {
    override def apply(@NotNull mgr: MatchManager, @Nullable term: Term): Unit = term match {
      case Bound(i) =>
        index(mgr, i)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"Bound$index"
  }

  /** Short for `Bound(Is(index))`. That is, matches a constant with index `index`. */
  @NotNull def Bound(index: Int): Pattern[Term] = Bound(Is(index))

  /** Pattern matching a type constructor ([[pure.Type Type]]) `t`. Subpattern `name` will be applied to
   * `t.`[[pure.Type.name name]], and the ''n'' subpatterns `args` will be applied to the ''n'' type
   * arguments `t.`[[pure.Type.args]]. (If `t` does not have ''n'' type arguments, the pattern does not match.)
   **/
  @NotNull @varargs def Type(@NotNull name: Pattern[_ >: String], @NotNull args: Pattern[_ >: Typ]*): Pattern[Typ] = new Pattern[Typ] {
    override def apply(@NotNull mgr: MatchManager, @Nullable typ: Typ): Unit = typ match {
      case Type(n, a@_*) =>
        if (a.length != args.length) Pattern.reject()
        name(mgr, n)
        for ((a1, args1) <- a.zip(args))
          args1(mgr, a1)
    }

    override def toString: String = s"Type($name,${args.mkString(",")})"
  }

  /** Short for `Type(Is(name),args)`. That is, matches a type with type constructor `name` and
   * exactly `args.length` type arguments. */
  @NotNull @varargs def Type(@NotNull name: String, @NotNull args: Pattern[_ >: Typ]*): Pattern[Typ] =
    Type(Is(name), args: _*)

  /** Pattern matching a type constructor ([[pure.Type Type]]) `t`. Subpattern `name` will be applied to
   * `t.`[[pure.Type.name name]], and `args` will be applied to `t.`[[pure.Type.args args]].
   **/
  @NotNull def Type(@NotNull name: Pattern[_ >: String], @NotNull args: Pattern[_ >: Array[Typ]]): Pattern[Typ] = new Pattern[Typ] {
    override def apply(@NotNull mgr: MatchManager, @Nullable typ: Typ): Unit = typ match {
      case Type(n, a@_*) =>
        name(mgr, n)
        args(mgr, a.toArray)
    }

    override def toString: String = s"Type($name,$args*)"
  }

  /** Pattern matching a free type variable ([[pure.TFree TFree]]) `v`. Subpattern `name` will be applied to `v.`[[pure.TFree.name name]],
   * and `sort` will be applied to `v.`[[pure.TFree.sort sort]]
   **/
  @NotNull def TFree(@NotNull name: Pattern[_ >: String], @NotNull sort: Pattern[_ >: Array[String]]): Pattern[Typ] = new Pattern[Typ] {
    override def apply(@NotNull mgr: MatchManager, @Nullable typ: Typ): Unit = typ match {
      case TFree(n, s) =>
        name(mgr, n)
        sort(mgr, s.toArray)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"TFree($name,$sort)"
  }

  /** Short for `TFree(Is(name),sort)`. That is, matches a free type variable with name `name`. */
  @NotNull def TFree(@NotNull name: String, @NotNull sort: Pattern[_ >: Array[String]]): Pattern[Typ] = TFree(Is(name), sort)

  /** Pattern matching a schematic type variable ([[pure.TVar TVar]]) `v`. Subpattern `name` will be applied to `v.`[[pure.TVar.name name]],
   * `index` will be applied to `v.`[[pure.TVar.index index]],
   * and `sort` will be applied to `v.`[[pure.TVar.sort sort]]
   **/
  @NotNull def TVar(@NotNull name: Pattern[_ >: String], @NotNull index: Pattern[_ >: Int], @NotNull sort: Pattern[_ >: Array[String]]): Pattern[Typ] = new Pattern[Typ] {
    override def apply(@NotNull mgr: MatchManager, @Nullable typ: Typ): Unit = typ match {
      case TVar(n, i, s) =>
        name(mgr, n)
        index(mgr, i)
        sort(mgr, s.toArray)
      case _ =>
        Pattern.reject()
    }

    override def toString: String = s"TVar($name,$index,$sort)"
  }

  /** Short for `Var(Is(name),Is(index),typ)`. That is, matches a schematic variable with name `name` and index `index`. */
  @NotNull def TVar(@NotNull name: String, index: Int, @NotNull sort: Pattern[_ >: Array[String]]): Pattern[Typ] =
    TVar(Is(name), Is(index), sort)
}
