package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/**
 * [[MLValue.Converter]] for type `(A,B,C,D,E,F,G)`.
 *
 *  - ML type: `a * b * c * d * e * f * g` (if `a,b,c,d,e,f,g` are the ML types corresponding to `A`,`B`,`C`,`D`,`E`,`F`,`G`).
 *  - Encoding of a pair (x_A,x_B,x_C,x_D,x_E,x_F,x_G) as an exception: `E_Pair e_A (E_Pair e_B (E_Pair e_C (E_Pair e_D (E_Pair e_E (E_Pair e_F e_G)))))` where `e_T`
 *    is the encoding of `x_T` as an exception (according to the converter for type `T`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class Tuple7Converter[A, B, C, D, E, F, G](converterA: Converter[A], converterB: Converter[B], converterC: Converter[C],
                                                   converterD: Converter[D], converterE: Converter[E], converterF: Converter[F],
                                                   converterG: Converter[G]) extends Converter[(A, B, C, D, E, F, G)] {
  @inline override def retrieve(value: MLValue[(A, B, C, D, E, F, G)])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[(A, B, C, D, E, F, G)] = {
    for (DList(DObject(aID), DObject(bID), DObject(cID), DObject(dID), DObject(eID), DObject(fID), DObject(gID)) <- Ops.retrieveTuple7(value.id);
         a <- converterA.retrieve(MLValue.unsafeFromId[A](Future.successful(aID)));
         b <- converterB.retrieve(MLValue.unsafeFromId[B](Future.successful(bID)));
         c <- converterC.retrieve(MLValue.unsafeFromId[C](Future.successful(cID)));
         d <- converterD.retrieve(MLValue.unsafeFromId[D](Future.successful(dID)));
         e <- converterE.retrieve(MLValue.unsafeFromId[E](Future.successful(eID)));
         f <- converterF.retrieve(MLValue.unsafeFromId[F](Future.successful(fID)));
         g <- converterG.retrieve(MLValue.unsafeFromId[G](Future.successful(gID))))
      yield (a, b, c, d, e, f, g)
  }

  @inline override def store(value: (A, B, C, D, E, F, G))(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[(A, B, C, D, E, F, G)] = {
    val (a, b, c, d, e, f, g) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    val mlC = converterC.store(c)
    val mlD = converterD.store(d)
    val mlE = converterE.store(e)
    val mlF = converterF.store(f)
    val mlG = converterG.store(g)
    Ops.storeTuple7[A, B, C, D, E, F, G](for (idA <- mlA.id; idB <- mlB.id; idC <- mlC.id; idD <- mlD.id; idE <- mlE.id; idF <- mlF.id; idG <- mlG.id)
      yield DList(DObject(idA), DObject(idB), DObject(idC), DObject(idD), DObject(idE), DObject(idF), DObject(idG)))
      .asInstanceOf[MLValue[(A, B, C, D, E, F, G)]]
  }

  override def exnToValue: String = s"fn E_Pair (a, E_Pair (b, E_Pair (c, E_Pair (d, E_Pair (e, E_Pair (f, g)))))) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b, (${converterC.exnToValue}) c, (${converterD.exnToValue}) d, (${converterE.exnToValue}) e, (${converterF.exnToValue}) f, (${converterG.exnToValue}) g)"
  override def valueToExn: String = s"fn (a,b,c,d,e,f,g) => E_Pair ((${converterA.valueToExn}) a, E_Pair ((${converterB.valueToExn}) b, E_Pair ((${converterC.valueToExn}) c, E_Pair ((${converterD.valueToExn}) d, E_Pair ((${converterE.valueToExn}) e, E_Pair ((${converterF.valueToExn}) f, (${converterG.valueToExn}) g))))))"

  override def mlType: String = s"(${converterA.mlType}) * (${converterB.mlType}) * (${converterC.mlType}) * (${converterD.mlType}) * (${converterE.mlType}) * (${converterF.mlType}) * (${converterG.mlType})"
}
