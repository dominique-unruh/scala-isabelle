package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleMLException
import de.unruh.isabelle.control.IsabelleTest.{isabelle => isa}
import de.unruh.isabelle.misc.Symbols
import de.unruh.isabelle.mlvalue.{MLFunction, MLValue}
import de.unruh.isabelle.pure.TermTest.assertRecursivelyConcrete
import org.scalatest.Assertions
import org.scalatest.funsuite.AnyFunSuite
import scalaz.Leibniz.subst

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class TermTest extends AnyFunSuite {
  lazy val ctxt: Context = Context("Main")

  private def await[A](awaitable: Awaitable[A]) = Await.result(awaitable, Duration.Inf)

  test("equals: Const/Const") {
    val const1 = Const("true", Type("bool"))
    val const1b = Const("true", Type("bool"))
    val const2 = Const("false", Type("bool"))
    val const3 = Const("true", Type("xxx"))

    assert(const1==const1)
    assert(const1==const1b)
    assert(const1!=const2)
    assert(const1!=const3)
    assert(const2!=const1)
    assert(const2==const2)
    assert(const2!=const3)
    assert(const3!=const1)
    assert(const3!=const2)
    assert(const3==const3)
  }

  test("parse: True") {
    val str = "True"
    val term = Term(ctxt, str)
    println(term.getClass, term)

    term match {
      case Const("HOL.True", Type("HOL.bool")) =>
    }
  }

  test("term/cterm compare") {
    val term = Const("HOL.True", Type("HOL.bool"))
    val cterm = Cterm(ctxt, term)
    //noinspection ComparingUnrelatedTypes
    assert(term == cterm)

    println(Const.unapply(term))
    println(Const.unapply(cterm))

    cterm match {
      case Const("HOL.True", Type("HOL.bool")) =>
      case _ => fail()
    }
  }

  test("bad cterm") {
    val term = Const("HOL.True", Type("Nat.nat"))
    assertThrows[IsabelleMLException] {
      Cterm(ctxt, term).force
    }
  }

  test("fastype – local") {
    def natT = Type("Nat.nat")
    def natFunT = Type("fun", natT, natT)
    val term = App(Free("x", natFunT), Free("y", natT))
    val typ = term.fastType
    assert(typ == natT)
  }

  test("fastype – remote") {
    def natT = Type("Nat.nat")
    val term = Term(ctxt, "1 + (2::nat)")
    val typ = term.fastType
    assert(typ == natT)
  }

  test("fastype – mixed") {
    def natT = Type("Nat.nat")
    val term1 = Term(ctxt, "%x::nat. 1 + x")
    val term2 = Term(ctxt, "2 :: nat")
    val term = App(term1, term2)
    val typ = term.fastType
    assert(typ == natT)
  }

  test("concrete recursive") {
    val t = Term(ctxt, "1+2 = 3")
    val t2 = t.concreteRecursive
    assertRecursivelyConcrete(t2)
    assert(t2 == t)
    assert(t2.mlValue == t.mlValue)
    val t3 = t2.concreteRecursive // already concrete, so it shouldn't change
    assert(t3 eq t2)
  }

  test("parse from unicode string") {
    val t = Term(ctxt, "1 ≤ 2")
    val t2 = Term(ctxt, "less_eq 1 2")
    assert(t == t2)
  }

  test("equal merge") {
    val t1 = Term(ctxt, "1 = 2")
    val t2 = Term(ctxt, "1 = 2")
    assert(await(t1.mlValue.id) != await(t2.mlValue.id))
    assert(t1 == t2)
    assert(await(t1.mlValue.id) == await(t2.mlValue.id))
  }

  test("equal merge gc") {
    val dummyFn = MLValue.compileFunction[Unit, Unit]("fn () => ()")
    val numObjects = MLValue.compileFunction[Unit, Int]("Control_Isabelle.numObjects")
    val mkTerm = MLValue.compileFunction[Unit, Term]("""fn () => Bound 1 $ Const("bla", Type("HOL.bool", []))""")
    val buffer = ArrayBuffer[Future[Term]]()
    val numTerms = 1000

    def gc(): Unit = {
      for (i <- 1 to 10) {
        System.gc()
        Thread.sleep(100)
        dummyFn().retrieveNow
        Thread.sleep(100)
      }
    }

    val startCount = numObjects().retrieveNow
    println(s"Num objects before creating: ${startCount}")
    println("Creating terms")
    for (_ <- 1 to numTerms)
      buffer.addOne(mkTerm().retrieve)
    println("Waiting for terms")
    // Can try with .force or with .concreteRecursive
    // TODO why does the test fail with .concreteRecursive? ANSWER: because Typ's are created and for those we have not updated equals yet.
    val terms = buffer.toSeq.map(t => await(t).concreteRecursive)
    buffer.clear() // Allow GC
    val count1 = numObjects().retrieveNow
    gc()
    println(s"Num objects after creating: $count1")
    assert(count1 >= startCount + numTerms)
    val termSame = mkTerm().retrieveNow.concreteRecursive
    val termOther = Term(ctxt, "1 = 2")
    println("Comparing terms (false)")
    for (t <- terms)
      assert(termOther != t)
    val count2 = numObjects().retrieveNow
    println(s"Num objects after comparing: $count2")
    assert(count2 >= startCount + numTerms)
    gc()
    val count3 = numObjects().retrieveNow
    println(s"Num objects after gc: $count3")
    assert(count2 >= startCount + numTerms)
    println("Comparing terms (true)")
    for (t <- terms)
      assert(termSame == t)
    // TODO: This should not increase the number of objects!
    val count4 = numObjects().retrieveNow
    println(s"Num objects after comparing: $count4")
    gc()
    val count5 = numObjects().retrieveNow
    println(s"Num objects after gc: $count5")
    assert(count5 <= startCount + 100)
    println("Comparing terms (true), again")
    for (t <- terms)
      assert(termSame == t)
    val count6 = numObjects().retrieveNow
    println(s"Num objects after comparing: $count6")
    assert(count6 <= startCount + 100)
  }
}

object TermTest {
  private def assertRecursivelyConcrete(t: Term): Unit = t match {
    case t : Const => TypTest.assertRecursivelyConcrete(t.typ)
    case t : Free => TypTest.assertRecursivelyConcrete(t.typ)
    case t : Var => TypTest.assertRecursivelyConcrete(t.typ)
    case t : App => assertRecursivelyConcrete(t.fun); assertRecursivelyConcrete(t.arg)
    case _ : Bound =>
    case _ => Assertions.fail(s"Not a concrete term: $t")
  }
}