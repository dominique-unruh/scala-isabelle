package de.unruh.isabelle

import de.unruh.isabelle.control.{Isabelle, IsabelleTest}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import IsabelleTest.{isabelle => isa}
import de.unruh.isabelle.pure.{Context, Typ, Type}

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
