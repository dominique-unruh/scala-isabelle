package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/**
 * [[MLValue.Converter]] for type `(A,B,C,D)`.
 *
 *  - ML type: `a * b * c * d` (if `a,b,c,d` are the ML types corresponding to `A`,`B`,`C`,`D`).
 *  - Encoding of a pair (x_A,x_B,x_C,x_D) as an exception: `E_Pair e_A (E_Pair e_B (E_Pair e_C e_D))` where `e_T`
 *    is the encoding of `x_T` as an exception (according to the converter for type `T`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class Tuple4Converter[A, B, C, D](converterA: Converter[A], converterB: Converter[B], converterC: Converter[C], converterD: Converter[D]) extends Converter[(A, B, C, D)] {
  override def retrieve(value: MLValue[(A, B, C, D)])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[(A, B, C, D)] = {
    for (DList(DObject(aID), DObject(bID), DObject(cID), DObject(dID)) <- Ops.retrieveTuple4(value.asInstanceOf[MLValue[(MLValue[A], MLValue[B], MLValue[C], MLValue[D])]]);
         a <- converterA.retrieve(MLValue.unsafeFromId[A](Future.successful(aID)));
         b <- converterB.retrieve(MLValue.unsafeFromId[B](Future.successful(bID)));
         c <- converterC.retrieve(MLValue.unsafeFromId[C](Future.successful(cID)));
         d <- converterD.retrieve(MLValue.unsafeFromId[D](Future.successful(dID))))
      yield (a, b, c, d)
  }

  override def store(value: (A, B, C, D))(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[(A, B, C, D)] = {
    val (a, b, c, d) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    val mlC = converterC.store(c)
    val mlD = converterD.store(d)
    Ops.storeTuple4[A, B, C, D](for (idA <- mlA.id; idB <- mlB.id; idC <- mlC.id; idD <- mlD.id) yield (DList(DObject(idA), DObject(idB), DObject(idC), DObject(idD))))
      .asInstanceOf[MLValue[(A, B, C, D)]]
  }

  @inline override def exnToValue: String = s"fn E_Pair (a, E_Pair (b, E_Pair (c, d))) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b, (${converterC.exnToValue}) c, (${converterD.exnToValue}) d)"
  @inline override def valueToExn: String = s"fn (a,b,c,d) => E_Pair ((${converterA.valueToExn}) a, E_Pair ((${converterB.valueToExn}) b, E_Pair ((${converterC.valueToExn}) c, (${converterD.valueToExn}) d)))"

  override def mlType: String = s"(${converterA.mlType}) * (${converterB.mlType}) * (${converterC.mlType}) * (${converterD.mlType})"
}
