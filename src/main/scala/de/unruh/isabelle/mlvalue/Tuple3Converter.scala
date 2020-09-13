package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.{ExecutionContext, Future}

import MLValue.Implicits._

@inline class Tuple3Converter[A, B, C](converterA: Converter[A], converterB: Converter[B], converterC: Converter[C]) extends Converter[(A, B, C)] {
  @inline override def retrieve(value: MLValue[(A, B, C)])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[(A, B, C)] = {
    for (DList(DObject(aID), DObject(bID), DObject(cID)) <- Ops.retrieveTuple3(value.id);
         a <- converterA.retrieve(new MLValue[A](Future.successful(aID)));
         b <- converterB.retrieve(new MLValue[B](Future.successful(bID)));
         c <- converterC.retrieve(new MLValue[C](Future.successful(cID))))
      yield (a, b, c)
  }

  @inline override def store(value: (A, B, C))(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[(A, B, C)] = {
    val (a, b, c) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    val mlC = converterC.store(c)
    Ops.storeTuple3[A, B, C](for (idA <- mlA.id; idB <- mlB.id; idC <- mlC.id) yield (DList(DObject(idA), DObject(idB), DObject(idC))))
      .asInstanceOf[MLValue[(A, B, C)]]
  }

  override lazy val exnToValue: String = s"fn E_Pair (a, E_Pair (b, c)) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b, (${converterC.exnToValue}) c)"
  override lazy val valueToExn: String = s"fn (a,b,c) => E_Pair ((${converterA.valueToExn}) a, E_Pair ((${converterB.valueToExn}) b, (${converterC.valueToExn}) c))"
}
