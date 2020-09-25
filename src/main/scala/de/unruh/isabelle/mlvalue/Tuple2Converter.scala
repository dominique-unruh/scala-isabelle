package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/**
 * [[MLValue.Converter]] for type `(A,B)`.
 *
 *  - ML type: `a * b` (if `a,b` are the ML types corresponding to `A`,`B`).
 *  - Encoding of a pair (x_A,x_B) as an exception: `E_Pair e_A e_B` where `e_T`
 *    is the encoding of `x_T` as an exception (according to the converter for type `T`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class Tuple2Converter[A, B](converterA: Converter[A], converterB: Converter[B]) extends Converter[(A, B)] {
  @inline override def retrieve(value: MLValue[(A, B)])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[(A, B)] = {
    for (DList(DObject(aID), DObject(bID)) <- Ops.retrieveTuple2(value.asInstanceOf[MLValue[(MLValue[A], MLValue[B])]]);
         a <- converterA.retrieve(MLValue.unsafeFromId[A](Future.successful(aID)));
         b <- converterB.retrieve(MLValue.unsafeFromId[B](Future.successful(bID))))
      yield (a, b)
  }

  @inline override def store(value: (A, B))(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[(A, B)] = {
    val (a, b) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    Ops.storeTuple2[A, B](for (idA <- mlA.id; idB <- mlB.id) yield (DList(DObject(idA), DObject(idB))))
      .asInstanceOf[MLValue[(A, B)]]
  }

  @inline override def exnToValue: String = s"fn E_Pair (a,b) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b) | ${MLValue.matchFailExn("Tuple2Converter.exnToValue")}"
  @inline override def valueToExn: String = s"fn (a,b) => E_Pair ((${converterA.valueToExn}) a, (${converterB.valueToExn}) b)"

  override def mlType: String = s"(${converterA.mlType}) * (${converterB.mlType})"
}
