package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.Converter

import scala.concurrent.{ExecutionContext, Future}

/**
 * [[MLValue.Converter]] for type `D => R`.
 *
 *  - ML type: `d -> r` (if `d,r` are the ML types corresponding to `D`,`R`).
 *  - Encoding of a function `f` as an exception: E_Function (DObject o val2exnR o f o exn2valD o (fn DObject e => e))`
 *    where `exn2valD` is a function that converts an exception back to a value of type `d`, and `val2exnR`
 *    is a function that converts a value of type `r` to an exception (as specified by the converters
 *    for types `d`,`r`).
 *
 * Note that [[store]] is not supported by this converter. That means that `MLValue(f)` for a Scala function `f` will
 * fail (since a Scala function cannot be transferred to the Isabelle process). However, [[retrieve]] works,
 * thus `mlF.retrieveNow` for `mlF : MLValue[D=>R]` will return a function (that calls back to the Isabelle process whenever executed).
 * But the preferred way to invoke `mlF` is to convert it into an [[MLFunction]] by `mlF.function` and invoke the
 * resulting [[MLFunction]]. Another way to generate an `MLValue[D=>R]` is using
 * [[MLValue.compileFunction[D,R]* MLValue.compileFunction]] to
 * compile the function from ML source code.
 *
 * While the existence of this conversion makes it possible to use functions as arguments to other functions
 * (and thus do arbitrary higher order computations), we suspect that the presence of nested conversions (the
 * composition with `val2exnR` and `exn2valD` above) can make this slow in more complex cases. (We have not
 * benchmarked this, though.)
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class FunctionConverter[D,R](implicit converterD : Converter[D], converterR: Converter[R]) extends Converter[D => R] {
  override def retrieve(value: MLValue[D => R])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[D => R] = {
    val mlFunction = value.function[D,R]
    def function(d: D): R = mlFunction(d).retrieveNow
    for (_ <- value.id) // Make sure this future completes only after value has been computed on the ML side
      yield function _
  }

  override def store(value: D => R)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[D => R] =
    throw new UnsupportedOperationException("Cannot store a Scala function in the ML process")

  override def exnToValue: String =
    s"fn E_Function f => ((${converterR.exnToValue}) o (fn DObject e => e) o f o DObject o (${converterD.valueToExn})) | ${MLValue.matchFailExn("FunctionConverter.exnToValue")}"
  override def valueToExn: String =
    s"fn f => E_Function (DObject o (${converterR.valueToExn}) o f o (${converterD.exnToValue}) o (fn DObject e => e))"

  override def mlType: String = s"(${converterD.mlType}) -> (${converterR.mlType})"
}
