package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.pure.TermInterpolator.Interpolator
import org.scalactic.PrettyMethods.Prettyizer

class TermInterpolatorTest extends AnyFunSuite {
  implicit lazy val context: Context = Context("Main")

  test("interpolate term") {
    val number2 = Term(context, "2 :: nat")
    val term = term"x+$number2"
    assert(term.pretty(context) == "x + 2")
  }

  test("pattern match") {
    val term = Term(context, "1+2+3")
    term match {
      case term"1+$num+5" => fail() // wrong pattern
      case term"1+$num+(3::nat)" => fail() // wrong type of "3"
      case term"_+$num+3" => assert(num.pretty(context) == "2::'a")
    }
  }

  test("type interpolation") {
    val typ = Typ(context, "nat")
    val term = term"(1 :: $typ :: one) = b"

    assert(term.pretty(context) == "1 = b")

    val App(_,b) = term
    assert(b.fastType == typ)
  }

  test("type extraction") {
    val term = Term(context, "1+2+(3::nat)")
    term match {
      case term"(_ :: $typ :: plus) + _" => assert(typ.pretty(context) == "nat")
    }
  }

  test("%type annotation") {
    val term = Term(context, "1+2")
    term match {
      case term"$x%term + _" => assert(x.pretty(context) == "1::'a")
    }
  }

  test("%term annotation") {
    val term = Term(context, "TYPE(nat)")
    term match {
      case term"TYPE($typ%type)" => assert(typ.pretty(context) == "nat")
    }
  }
}
