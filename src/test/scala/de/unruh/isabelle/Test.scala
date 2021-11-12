package de.unruh.isabelle

import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.MLValueTest.await
import de.unruh.isabelle.pure.{Context, Cterm, Term, Thm}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ListBuffer
import scala.util.Random

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("Java example") {
    JavaExample.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("temporary experiments") {
    val future = isabelle.storeValue("E_Function (fn DObject x => (I) ((fn E_Data data => data) x))")
    val id = await(future)
    println(id)
//    val mlValue = MLValue(1)
    Thread.sleep(1000)
  }
}
