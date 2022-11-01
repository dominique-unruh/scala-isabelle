package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.Future

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext
import Implicits._

/**
 * [[MLValue.Converter]] for type `(A,B,C)`.
 *
 *  - ML type: `a * b * c` (if `a,b,c` are the ML types corresponding to `A`,`B`,`C`).
 *  - Encoding of a pair (x_A,x_B,x_C) as an exception: `E_Pair e_A (E_Pair e_B e_C)` where `e_T`
 *    is the encoding of `x_T` as an exception (according to the converter for type `T`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class Tuple3Converter[A, B, C](converterA: Converter[A], converterB: Converter[B], converterC: Converter[C]) extends Converter[(A, B, C)] {
  @inline override def retrieve(value: MLValue[(A, B, C)])(implicit isabelle: Isabelle): Future[(A, B, C)] = {
    for (DList(DObject(aID), DObject(bID), DObject(cID)) <- Ops.retrieveTuple3(value.asInstanceOf[MLValue[(MLValue[A], MLValue[B], MLValue[C])]]);
         a <- converterA.retrieve(MLValue.unsafeFromId[A](Future.successful(aID)));
         b <- converterB.retrieve(MLValue.unsafeFromId[B](Future.successful(bID)));
         c <- converterC.retrieve(MLValue.unsafeFromId[C](Future.successful(cID))))
      yield (a, b, c)
  }

  @inline override def store(value: (A, B, C))(implicit isabelle: Isabelle): MLValue[(A, B, C)] = {
    val (a, b, c) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    val mlC = converterC.store(c)
    Ops.storeTuple3[A, B, C](for (idA <- mlA.id; idB <- mlB.id; idC <- mlC.id) yield (DList(DObject(idA), DObject(idB), DObject(idC))))
      .asInstanceOf[MLValue[(A, B, C)]]
  }

  @inline override def exnToValue(implicit isabelle: Isabelle): String = s"fn E_Pair (a, E_Pair (b, c)) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b, (${converterC.exnToValue}) c)"
  @inline override def valueToExn(implicit isabelle: Isabelle): String = s"fn (a,b,c) => E_Pair ((${converterA.valueToExn}) a, E_Pair ((${converterB.valueToExn}) b, (${converterC.valueToExn}) c))"

  override def mlType(implicit isabelle: Isabelle): String = s"(${converterA.mlType}) * (${converterB.mlType}) * (${converterC.mlType})"
}
