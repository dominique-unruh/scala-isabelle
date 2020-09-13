package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DList, DObject}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops}

import scala.concurrent.{ExecutionContext, Future}

import MLValue.Implicits._

@inline class Tuple2Converter[A, B](converterA: Converter[A], converterB: Converter[B]) extends Converter[(A, B)] {
  @inline override def retrieve(value: MLValue[(A, B)])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[(A, B)] = {
    for (DList(DObject(aID), DObject(bID)) <- Ops.retrieveTuple2(value.id);
         a <- converterA.retrieve(new MLValue[A](Future.successful(aID)));
         b <- converterB.retrieve(new MLValue[B](Future.successful(bID))))
      yield (a, b)
  }

  @inline override def store(value: (A, B))(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[(A, B)] = {
    val (a, b) = value
    val mlA = converterA.store(a)
    val mlB = converterB.store(b)
    Ops.storeTuple2[A, B](for (idA <- mlA.id; idB <- mlB.id) yield (DList(DObject(idA), DObject(idB))))
      .asInstanceOf[MLValue[(A, B)]]
  }

  override lazy val exnToValue: String = s"fn E_Pair (a,b) => ((${converterA.exnToValue}) a, (${converterB.exnToValue}) b) | ${MLValue.matchFailExn("Tuple2Converter.exnToValue")}"
  override lazy val valueToExn: String = s"fn (a,b) => E_Pair ((${converterA.valueToExn}) a, (${converterB.valueToExn}) b)"
}
