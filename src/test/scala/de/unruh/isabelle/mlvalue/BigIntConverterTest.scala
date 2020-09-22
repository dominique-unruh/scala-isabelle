package de.unruh.isabelle.mlvalue

import org.scalatest.funsuite.AnyFunSuite

import de.unruh.isabelle.mlvalue.Implicits._

class BigIntConverterTest extends AnyFunSuite {
  test("roundtrip") {
    MLValueTest.roundTrip(BigInt("1234342342349173491283749284453453453"))
  }

  test("roundtrip negative") {
    MLValueTest.roundTrip(BigInt("-1234342342349173491283749284453453453"))
  }
}
