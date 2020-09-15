package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.mlvalue.MLValue.Converter

import scala.concurrent.{ExecutionContext, Future}

// TODO: Document API
@inline class FunctionConverter[D,R](implicit converterD : Converter[D], converterR: Converter[R]) extends Converter[D => R] {
  override def retrieve(value: MLValue[D => R])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[D => R] = {
    val mlFunction = value.function[D,R]
    def function(d: D): R = mlFunction(d).retrieveNow
    for (_ <- value.id) // Make sure this future completes only after value has been computed on the ML side
      yield function _
  }

  override def store(value: D => R)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[D => R] =
    throw new UnsupportedOperationException("Cannot store a Scala function in the ML process")

  @inline override def exnToValue: String = s"fn E_Function f => ((${converterR.exnToValue}) o f o (${converterD.valueToExn})) | ${MLValue.matchFailExn("FunctionConverter.exnToValue")}"
  @inline override def valueToExn: String = s"fn f => E_Function ((${converterR.valueToExn}) o f o (${converterD.exnToValue}))"
}
