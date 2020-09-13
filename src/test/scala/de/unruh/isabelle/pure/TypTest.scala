package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest
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
}
