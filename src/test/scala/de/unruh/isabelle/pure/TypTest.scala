package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{IsabelleMLException, IsabelleTest}
import de.unruh.isabelle.control.IsabelleTest.{isabelle => isa}
import de.unruh.isabelle.misc.Symbols
import de.unruh.isabelle.pure.TypTest.assertRecursivelyConcrete
import org.scalatest.Assertions
import org.scalatest.funsuite.AnyFunSuite

class TypTest extends AnyFunSuite {
  lazy val ctxt: Context = Context("Main")

  test("parse: nat") {
    val str = "nat"
    val typ = Typ(ctxt, str)
    println(typ.getClass, typ)
    typ match {
      case Type("Nat.nat") => ()
      case _ => fail()
    }
  }

  test("typ/ctyp compare") {
    val typ = Type("List.list", Type("Nat.nat"))
    val ctyp = Ctyp(ctxt, typ)
    //noinspection ComparingUnrelatedTypes
    assert(typ == ctyp)
    ctyp match {
      case Type("List.list", Type("Nat.nat")) => ()
      case _ => fail()
    }
  }

  test("bad ctyp") {
    val typ = Type("Nat.nat", Type("Nat.nat"))
    assertThrows[IsabelleMLException] {
      Ctyp(ctxt, typ).force
    }
  }

  test("concrete recursive") {
    val t = Typ(ctxt, "nat => string")
    val t2 = t.concreteRecursive
    assertRecursivelyConcrete(t2)
    assert(t2 == t)
    assert(t2.mlValue == t.mlValue)
    val t3 = t2.concreteRecursive // already concrete, so it shouldn't change
    assert(t3 eq t2)
  }

  test("parse from unicode string") {
    val t = Typ(ctxt, "nat ⇒ int")
    val t2 = Typ(ctxt, "(nat,int) fun")
    assert(t == t2)
  }
}

object TypTest {
  def assertRecursivelyConcrete(t: Typ): Unit = t match {
    case t : Type => for (a <- t.args) assertRecursivelyConcrete(a)
    case _ : TVar =>
    case _ : TFree =>
    case _ => Assertions.fail(s"Not a concrete typ: $t")
  }
}
