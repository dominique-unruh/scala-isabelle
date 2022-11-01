package de.unruh.isabelle

import com.ibm.icu.lang.UCharacter.DecompositionType
import com.ibm.icu.lang.{CharacterProperties, UCharacter, UProperty}
import com.ibm.icu.text.{Normalizer, Normalizer2}
import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.mlvalue.MLValueTest.await
import de.unruh.isabelle.pure.{Context, Cterm, Term, Thm}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.mutable.ListBuffer
import scala.util.Random
import scala.util.control.Breaks
import scala.util.control.Breaks.{break, breakable}

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
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

  }
}
