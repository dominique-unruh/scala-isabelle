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

  test("MLValue documentation example") {

    // implicit val isabelle: Isabelle = new Isabelle(...)
    import scala.concurrent.ExecutionContext.Implicits._

    // Create an MLValue containing an integer
    val intML : MLValue[Int] = MLValue(123)
    // 123 is now stored in the object store

    // Fetching the integer back
    val int : Int = intML.retrieveNow
    assert(int==123)

    // The type parameter of MLValue ensures that the following does not compile:
    // val string : String = intML.retrieveNow

    // We write an ML function that squares an integer and converts it into a string
    val mlFunction : MLFunction[Int, String] =
      MLValue.compileFunction[Int, String]("fn i => string_of_int (i*i)")

    // We can apply the function to an integer stored in the Isabelle process
    val result : MLValue[String] = mlFunction(intML)
    // The result is still stored in the Isabelle process, but we can retrieve it:
    val resultHere : String = result.retrieveNow
    assert(resultHere == "15129")

  }

  def await[A](x: Awaitable[A]): A = Await.result(x, Duration.Inf)
}