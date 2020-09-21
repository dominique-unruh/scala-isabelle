# scala-isabelle [![Build Status](https://travis-ci.com/dominique-unruh/scala-isabelle.svg?branch=master)](https://travis-ci.com/dominique-unruh/scala-isabelle)

## What is this library for?

This library allows to control an [Isabelle](https://isabelle.in.tum.de/) process
from a Scala application. It allows to interact with the Isabelle process, 
manipulate objects such as terms and theorems as if they were local Scala objects,
and execute ML code in the Isabelle process. The library serves a similar purpose
as the discontinued [libisabelle](https://github.com/larsrh/libisabelle).

## Prerequisites

* Linux or Windows
* Java 11 or newer (both for compilation and running)
* Scala 2.12 or newer
* [Isabelle 2019 or newer](https://isabelle.in.tum.de/) (needed at runtime)
* sbt

## Installation

With sbt, use one of
```sbt
libraryDependencies += "de.unruh" %% "scala-isabelle" % "0.1.0"  // release

libraryDependencies += "de.unruh" %% "scala-isabelle" % "0.1.1-SNAPSHOT"  // development snapshot
resolvers += Resolver.sonatypeRepo("snapshots")
```
to add scala-isabelle to your build.

Furthermore, you need to download the Isabelle distribution and unpack it somewhere (not needed for compilation,
so your application could also do it at runtime). In the example below, we will assume that you have installed 
Isabelle2020 at `/opt/Isabelle2020`.

##  Example

```Scala
// Initialize the Isabelle process with session HOL.
// We assume an Isabelle installation in /opt/Isabelle2020
val setup = Isabelle.Setup(isabelleHome = Path.of("/opt/Isabelle2020"), logic = "HOL")
implicit val isabelle: Isabelle = new Isabelle(setup)

// Load the Isabelle/HOL theory "Main" and create a context object
val ctxt = Context("Main")

// Create a term by parsing a string
val term = Term(ctxt, "x+0 = (y::nat)*1")

// A function to replace occurrences of X+1 by X (for all X)
def replace(term: Term): Term = term match {
  case App(App(Const("Groups.plus_class.plus", _), x), Const("Groups.zero_class.zero", _)) => replace(x)
  case Abs(name, typ, body) => Abs(name, typ, replace(body))
  case App(t1, t2) => App(replace(t1), replace(t2))
  case _ => term
}

// Replace x+0 by x in the term above
val term2 = replace(term)

// And pretty print the term
println("term2: " + term2.pretty(ctxt))
// ==> term2: x = y * 1

// Compile an ML function that can be executed directly in the Isabelle process
val simplify : MLFunction2[Context, Term, Term] =
  MLValue.compileFunction("fn (ctxt,t) => Thm.cterm_of ctxt t |> Simplifier.asm_full_rewrite ctxt " +
    "|> Thm.rhs_of |> Thm.term_of")

// Simplify term2
val term3 = simplify(ctxt,term2).retrieveNow

println("term3: " + term3.pretty(ctxt))
// ==> term3: x = y
```
The source code for this example can be found in [Example.scala](https://raw.githubusercontent.com/dominique-unruh/scala-isabelle/master/src/test/scala/de/unruh/isabelle/Example.scala).

## Further reading

* [Scaladoc for v0.1.0](https://oss.sonatype.org/service/local/repositories/releases/archive/de/unruh/scala-isabelle_2.13/0.1.0/scala-isabelle_2.13-0.1.0-javadoc.jar/!/de/unruh/isabelle/index.html)
* [Scaladoc for the development snapshot](https://oss.sonatype.org/service/local/repositories/snapshots/archive/de/unruh/scala-isabelle_2.13/0.1.1-SNAPSHOT/scala-isabelle_2.13-0.1.1-SNAPSHOT-javadoc.jar/!/de/unruh/isabelle/index.html)

For an introduction to the most important concepts, start with the API doc for classes `de.unruh.isabelle.control.Isabelle`
and `de.unruh.isabelle.mlvalue.MLValue` and `de.unruh.isabelle.mlvalue.Term`.

## Projects using scala-isabelle

* [qrhl-tool](https://github.com/dominique-unruh/qrhl-tool) – A theorem prover for post-quantum security