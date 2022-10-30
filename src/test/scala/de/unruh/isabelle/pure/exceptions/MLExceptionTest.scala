package de.unruh.isabelle.pure.exceptions

import de.unruh.isabelle.control.{Isabelle, IsabelleMLException, IsabelleTest}
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.pure.{Context, Term}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

// Implicits
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class MLExceptionTest extends AnyFunSuite {
  implicit lazy val isabelle: Isabelle = IsabelleTest.isabelle
  val ctxt: Context = Context("Pure")

  def recognizeExceptionTest[T <: IsabelleMLException](code: String)(implicit classTag: ClassTag[T]) : T = {
    val id = Await.result(isabelle.storeValue(code), Duration.Inf)
    val exn = IsabelleMLException.unsafeFromId(isabelle, id)
    println(exn)
    val exn2 = Await.result(MLException.recognizeException(exn), Duration.Inf)
    println(exn2)
    assert(classTag.runtimeClass.isInstance(exn2))
    exn2.asInstanceOf[T]
  }

  test("recognizeException ERROR") {
    val exn = recognizeExceptionTest[ErrorMLException]("""ERROR "test"""")
    assert(exn.msg == "test")
  }

  test("recognizeException Fail") {
    val exn = recognizeExceptionTest[FailMLException]("""Fail "test"""")
    assert(exn.msg == "test")
  }

  test("recognizeException THEORY") {
    val exn = recognizeExceptionTest[TheoryMLException]("""THEORY ("test", [\<^theory>])""")
    assert(exn.msg == "test")
    assert(exn.theories.length == 1)
  }

  test("recognizeException TERM") {
    val exn = recognizeExceptionTest[TermMLException]("""TERM ("test", [@{term x}])""")
    assert(exn.msg == "test")
    assert(exn.terms.map(_.prettyRaw(ctxt)) == List("x"))
  }

  test("recognizeException TYPE") {
    val exn = recognizeExceptionTest[TypeMLException]("""TYPE ("test", [@{typ prop}], [@{term x}])""")
    assert(exn.msg == "test")
    assert(exn.typs.map(_.prettyRaw(ctxt)) == List("prop"))
    assert(exn.terms.map(_.prettyRaw(ctxt)) == List("x"))
  }

  test("recognizeException CTERM") {
    val exn = recognizeExceptionTest[CtermMLException]("""CTERM ("test", [@{cterm x}])""")
    assert(exn.msg == "test")
    assert(exn.cterms.map(_.prettyRaw(ctxt)) == List("x"))
  }

  test("recognizeException THM") {
    val exn = recognizeExceptionTest[ThmMLException]("""THM ("test", 3, @{thms reflexive})""")
    assert(exn.msg == "test")
    assert(exn.theorems.map(_.prettyRaw(ctxt)) == List("?x \\<equiv> ?x"))
  }

  test("recognizeException - unknown") {
    val id = Await.result(isabelle.storeValue("let exception E in E end"), Duration.Inf)
    val exn = IsabelleMLException.unsafeFromId(isabelle, id)
    println(exn)
    val exn2 = Await.result(MLException.recognizeException(exn, fallback = null), Duration.Inf)
    println(exn2)
    assert(exn2 == null)
  }

  test("ExceptionManager") {
    implicit val isabelle: Isabelle = new Isabelle(IsabelleTest.setup.copy(exceptionManager = new MLException.ExceptionManager(_)))
    val manager = isabelle.exceptionManager.asInstanceOf[MLException.ExceptionManager]
    val fun = MLValue.compileFunction[Term, Unit]("""fn t => raise TERM ("test", [t])""")
    val ctxt = Context("Main")
    val term = Term(ctxt, "1+(1::nat)")

    val exn1 = intercept[TermMLException] { fun(term).retrieveNow }
    val msg1 = exn1.toString
    println(msg1)
    assert(msg1.contains("test"))
    assert(msg1.contains("??.Groups.plus_class.plus ??.Groups.one_class.one ??.Groups.one_class.one"))

    manager.setContext(ctxt)

    val exn2 = intercept[TermMLException] { fun(term).retrieveNow }
    val msg2 = exn2.toString
    println(msg2)
    assert(msg2.contains("test"))
    assert(msg2.contains("1 + 1"))
  }
}
