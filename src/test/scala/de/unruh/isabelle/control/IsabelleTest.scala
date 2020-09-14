package de.unruh.isabelle.control

import java.nio.file.Paths

import de.unruh.isabelle.control.Isabelle.{DInt, DList, DString, Data, Setup}
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.MLValue
import org.scalatest.concurrent.{Signaler, ThreadSignaler}
import org.scalatest.concurrent.TimeLimits.failAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.time.{Seconds, Span}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Awaitable, Future}
import scala.concurrent.duration.Duration

class IsabelleTest extends AnyFunSuite {
  private def await[A](f: Awaitable[A]) : A = Await.result(f, Duration.Inf)

  test("handle compilation error") {
    assertThrows[IsabelleException] {
      isabelle.executeMLCodeNow("1+true")
    }
  }

  test("initialize Isabelle") {
    isabelle
  }

  test("executeMLCode") {
    println("Sending code")
    val exec = isabelle.executeMLCode("val _ = tracing \"Hello\"")
    println("Waiting for completion")
    await(exec)
  }

  lazy val identityId: Isabelle.ID = await(isabelle.storeValue("E_Function (fn x => x)"))
  def roundTrip(data: Data): Unit = {
    val future = isabelle.applyFunction(identityId, data)
    val returned = await(future)
    assert(returned == data)
  }

  test("roundtrip DInt") {
    roundTrip(DInt(4356))
  }

  test("roundtrip DString") {
    roundTrip(DString("hello"))
  }

  test("roundtrip DInt negative") {
    roundTrip(DInt(-235345))
  }

  test("roundtrip DList") {
    roundTrip(DList(DInt(1), DString("2")))
  }

  test("non ASCII string") {
    val result = await(isabelle.applyFunction(identityId, DString("eĥoŝanĝo ĉiuĵaŭde")))
    assert(result == DString("e?o?an?o ?iu?a?de"))
  }

  test("too long string") {
    val builder = new StringBuilder
    for (i <- 1 to 10000000)
      builder ++= i.toString += '.'
    val str = builder.toString()
    assertThrows[IsabelleException] {
      roundTrip(DString(str))
    }
  }

  // Checks that the protocol doesn't get desynced by too long strings.
  test("too long string & continue") {
    val builder = new StringBuilder
    for (i <- 1 to 10000000)
      builder ++= i.toString += '.'
    val str = builder.toString()
    assertThrows[IsabelleException] {
      roundTrip(DString(str))
    }
    println("Roundtrip of string finished")

    implicit val signaler: ThreadSignaler.type = ThreadSignaler
    failAfter(Span(5, Seconds)) {
      roundTrip(DInt(0))
    }
  }
}

object IsabelleTest {
  val setup: Setup = Setup(
    isabelleHome = Paths.get("/opt/Isabelle2020"),
    sessionRoots = Nil,
    userDir = None
  )

  implicit lazy val isabelle: Isabelle = {
    println("Starting Isabelle")
    val isa = new Isabelle(setup)
    println("Started. Initializing Term/Typ/Context")
    println("Initialized.")
    isa
  }
}
