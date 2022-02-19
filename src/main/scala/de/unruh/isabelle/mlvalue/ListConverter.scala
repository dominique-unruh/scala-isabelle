package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, IsabelleMiscException, IsabelleMLException}
import de.unruh.isabelle.control.Isabelle.{DList, DObject, Data, ID}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}
import Implicits._

/**
 * [[MLValue.Converter]] for type [[scala.List List]][A].
 *
 *  - ML type: `a list` (if `a` is the ML type corresponding to `A`).
 *  - Encoding of a list `[x_1,...,x_n]` as an exception: `E_List [e_1,...,e_n]` where `e_i`
 *    is the encoding of `x_i` as an exception (according to the converter for `A`).
 *
 * @see MLValue.Converter for explanations what [[MLValue.Converter Converter]]s are for.
 */
@inline final class ListConverter[A](implicit converter: Converter[A]) extends Converter[List[A]] {
  @inline override def store(value: List[A])(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[List[A]] = {
    val listID: Future[List[ID]] = Future.traverse(value) {
      converter.store(_).id
    }
    val data: Future[Data] = for (listID <- listID) yield DList(listID map DObject: _*)
    val result: MLValue[List[MLValue[Nothing]]] = Ops.storeList(data)
    result.asInstanceOf[MLValue[List[A]]]
  }

  @inline override def retrieve(value: MLValue[List[A]])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[List[A]] = {
    for (DList(listObj@_*) <- Ops.retrieveList(value.asInstanceOf[MLValue[List[MLValue[Nothing]]]]);
         listMLValue = listObj map {
           case DObject(id) => MLValue.unsafeFromId[A](Future.successful(id))
           case _ => throw IsabelleMiscException("In ListConverter.retrieve: function result is not a DObject (internal error)")
         };
         list <- Future.traverse(listMLValue) {
           converter.retrieve(_)
         })
      yield list.toList
  }

  @inline override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"fn E_List list => map (${converter.exnToValue}) list | ${matchFailExn("ListConverter.exnToValue")}"
  @inline override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"E_List o map (${converter.valueToExn})"

  override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"(${converter.mlType}) list"
}
