package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite

// Implicits
import de.unruh.isabelle.pure.Implicits._


class StringInterpolatorsTest extends AnyFunSuite {
  implicit lazy val context: Context = Context("Main")

  test("interpolate term") {
    val number2 = Term(context, "2 :: nat")
    val term = term"x+$number2"
    assert(term.pretty(context) == "x + 2")
  }

  test("interpolate typ") {
    val nat = Typ(context, "nat")
    val typ = typ"$nat list"
    assert(typ.pretty(context) == "nat list")
  }

  test("pattern match") {
    val term = Term(context, "1+2+3")
    term match {
      case term"1+$num+5" => fail() // wrong pattern
      case term"1+$num+(3::nat)" => fail() // wrong type of "3"
      case term"_+$num+3" => assert(num.pretty(context) == "2::'a")
    }
  }

  test("pattern match of type") {
    val typ = Typ(context, "nat list")
    typ match {
      case typ"$t list" => assert(t.pretty(context) == "nat")
    }
  }

  test("type interpolation in term") {
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

  test("docstring example") {
    val typ1 = typ"nat"               // type nat
    assert(typ1 == Type("Nat.nat"))
    val typ2 = typ"$typ1 => $typ1"    // type nat => nat
    assert(typ2 == Typ(context, "nat => nat"))
    val term1 = term"f :: $typ2"      // term f :: nat => nat
    assert(term1 == Term(context, "f :: nat => nat"))
    val term2 = term"$term1 1"        // term (f :: nat => nat) (1 :: nat)  (by type inference)
    assert(term2 == Term(context, "(f :: nat => nat) (1 :: nat)"))
  }

  test("docstring parse example") {
    val term = term"1 + (2::nat)"
    term match {
      case term"$t + (_::$u::plus)" => (t,u)  // t : Term is "1::nat", u : Typ is "nat"
        assert(t == Term(context, "1 :: nat"))
        assert(u == Typ(context, "nat"))
    }
    val typ = typ"nat => nat"
    print(typ)
    typ match {
      case typ"$t => $dummy" => t    // t : Typ is "nat"
        assert(t == Typ(context, "nat"))
    }
  }
}
