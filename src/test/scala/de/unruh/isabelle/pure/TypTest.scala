package de.unruh.isabelle.pure

import de.unruh.isabelle.control.{IsabelleException, IsabelleTest}
import de.unruh.isabelle.control.IsabelleTest.{isabelle => isa}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class TypTest extends AnyFunSuite {
  lazy val ctxt: Context = Context("Main")

  test("parse: nat") {
    val str = "nat"
    val typ = Typ(ctxt, str)
    println(typ.getClass, typ)
    typ match {
      case Type("Nat.nat", List()) => ()
      case _ => fail()
    }
  }

  test("typ/ctyp compare") {
    val typ = Type("List.list", Type("Nat.nat"))
    val ctyp = Ctyp(ctxt, typ)
    //noinspection ComparingUnrelatedTypes
    assert(typ == ctyp)
    ctyp match {
      case Type("List.list", List(Type("Nat.nat", List()))) => ()
      case _ => fail()
    }
  }

  test("bad ctyp") {
    val typ = Type("Nat.nat", Type("Nat.nat"))
    assertThrows[IsabelleException] {
      Ctyp(ctxt, typ).force
    }
  }

}
