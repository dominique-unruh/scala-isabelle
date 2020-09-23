package de.unruh.isabelle.mlvalue

import org.scalatest.funsuite.AnyFunSuite

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
import scala.concurrent.ExecutionContext.Implicits.global
import Implicits._

class FunctionConverterTest extends AnyFunSuite {
  MLValue.init()

  test("compileFunction / retrieve") {
    val function = MLValue.compileFunction[Int,String]("string_of_int")
    assert(function(12).retrieveNow == "12")
    val local = function.retrieveNow
    assert(local(12) == "12")
  }

  test("compileValue / retrieve") {
    val function = MLValue.compileValue[Int => String]("string_of_int")
    assert(function.function.apply(12).retrieveNow == "12")
    val local = function.retrieveNow
    assert(local(12) == "12")
  }

  test("higher order") {
    val evalAt5 = MLValue.compileFunction[Int => String, String]("fn f => f 5")
    val stringOfInt = MLValue.compileFunction[Int, String]("string_of_int")
    val result = evalAt5(stringOfInt).retrieveNow
    assert(result == "5")
  }
}
