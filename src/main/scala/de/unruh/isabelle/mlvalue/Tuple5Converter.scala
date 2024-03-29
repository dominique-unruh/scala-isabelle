package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.Future

// Implicits
import de.unruh.isabelle.control.Isabelle.executionContext
import Implicits._

/**
 * [[MLValue.Converter]] for type `(A,B,C,D,E)`.
 *
 *  - ML type: `a * b * c * d * e` (if `a,b,c,d,e` are the ML types corresponding to `A`,`B`,`C`,`D`,`E`).
 *  - Encoding of a pair (x_A,x_B,x_C,x_D,x_E) as an exception: `E_Pair e_A (E_Pair e_B (E_Pair e_C (E_Pair e_D e_E)))` where `e_T`
 *    is the encoding of `x_T` as an exception (according to the converter for type `T`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
final class Tuple5Converter[A, B, C, D, E](converterA: Converter[A], converterB: Converter[B], converterC: Converter[C], converterD: Converter[D], converterE: Converter[E])
  extends Converter[(A, B, C, D, E)] {
  override def retrieve(value: MLValue[(A, B, C, D, E)])(implicit isabelle: Isabelle): Future[(A, B, C, D, E)] = {
    for (DList(DObject(aID), DObject(bID), DObject(cID), DObject(dID), DObject(eID)) <- Ops.retrieveTuple5(value.asInstanceOf[MLValue[(MLValue[A], MLValue[B], MLValue[C], MLValue[D], MLValue[E])]]);
         a <- converterA.retrieve(MLValue.unsafeFromId[A](Future.successful(aID)));
         b <- converterB.retrieve(MLValue.unsafeFromId[B](Future.successful(bID)));
         c <- converterC.retrieve(MLValue.unsafeFromId[C](Future.successful(cID)));
         d <- converterD.retrieve(MLValue.unsafeFromId[D](Future.successful(dID)));
         e <- converterE.retrieve(MLValue.unsafeFromId[E](Future.successful(eID))))
      yield (a, b, c, d, e)
  }

  override def store(value: (A, B, C, D, E))(implicit isabelle: Isabelle): MLValue[(A, B, C, D, E)] = {
    val (a, b, c, d, e) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    val mlC = converterC.store(c)
    val mlD = converterD.store(d)
    val mlE = converterE.store(e)
    Ops.storeTuple5[A, B, C, D, E](for (idA <- mlA.id; idB <- mlB.id; idC <- mlC.id; idD <- mlD.id; idE <- mlE.id)
      yield DList(DObject(idA), DObject(idB), DObject(idC), DObject(idD), DObject(idE)))
      .asInstanceOf[MLValue[(A, B, C, D, E)]]
  }

  @inline override def exnToValue(implicit isabelle: Isabelle): String = s"fn E_Pair (a, E_Pair (b, E_Pair (c, E_Pair (d, e)))) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b, (${converterC.exnToValue}) c, (${converterD.exnToValue}) d, (${converterE.exnToValue}) e)"
  @inline override def valueToExn(implicit isabelle: Isabelle): String = s"fn (a,b,c,d,e) => E_Pair ((${converterA.valueToExn}) a, E_Pair ((${converterB.valueToExn}) b, E_Pair ((${converterC.valueToExn}) c, E_Pair ((${converterD.valueToExn}) d, (${converterE.valueToExn}) e))))"

  override def mlType(implicit isabelle: Isabelle): String = s"(${converterA.mlType}) * (${converterB.mlType}) * (${converterC.mlType}) * (${converterD.mlType}) * (${converterE.mlType})"
}
