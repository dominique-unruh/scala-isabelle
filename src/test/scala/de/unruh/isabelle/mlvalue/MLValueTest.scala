package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.control.{Isabelle, IsabelleTest}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.MLValue.Implicits._
import de.unruh.isabelle.pure.{Context, Thm}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}

class MLValueTest extends AnyFunSuite {
  test ("two instances of Isabelle") {
    val isabelle1 = IsabelleTest.isabelle
    val isabelle2 = new Isabelle(IsabelleTest.setup)
    val ctxt1 = Context("Pure")(isabelle1, implicitly)
    val ctxt2 = Context("Main")(isabelle2, implicitly)
    val thm1 = Thm(ctxt1, "Pure.reflexive")(isabelle1, implicitly)
    val thm2 = Thm(ctxt2, "HOL.refl")(isabelle2, implicitly)
    val str1 = thm1.pretty(ctxt1)
    val str2 = thm2.pretty(ctxt2)
    assert(str1 == "?x \\<equiv> ?x")
    assert(str2 == "?t = ?t")
  }

  private def roundTrip[A](value: A)(implicit converter: Converter[A]) = {
    println(s"Creating MLValue($value)")
    val mlValue = MLValue(value)
    println("Getting ID")
    val id = await(mlValue.id)
    println("Retrieving")
    val future = mlValue.retrieve
    println("Waiting for future")
    val value2 = await(future)
    println("Checking")
    assert(value2==value)
  }

  test ("store/retrieve int") {
    roundTrip(123590)
  }

  test ("store/retrieve tuple2") {
    roundTrip((1,"2"))
  }

  test ("store/retrieve tuple3") {
    roundTrip((1,"2",true))
  }

  test ("store/retrieve tuple4") {
    roundTrip((1,"2",true,4))
  }

  test ("store/retrieve tuple5") {
    roundTrip((1,"2",true,4,"5"))
  }

  test ("store/retrieve tuple6") {
    roundTrip((1,"2",true,4,"5",6))
  }

  test ("store/retrieve tuple7") {
    roundTrip((1,"2",true,4,"5",6,false))
  }

  test("store/retrieve list") {
    roundTrip(List(1,2,3))
  }

  test("store/retrieve option") {
    roundTrip(Some(3) : Option[Int])
    roundTrip(None : Option[Int])
  }

  def await[A](x: Awaitable[A]): A = Await.result(x, Duration.Inf)
}