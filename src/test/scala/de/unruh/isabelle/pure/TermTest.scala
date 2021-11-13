package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleException
import de.unruh.isabelle.control.IsabelleTest.{isabelle => isa}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class TermTest extends AnyFunSuite {
  lazy val ctxt: Context = Context("Main")

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
    assertThrows[IsabelleException] {
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

}
