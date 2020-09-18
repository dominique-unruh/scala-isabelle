package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.{Isabelle, IsabelleException}
import de.unruh.isabelle.control.Isabelle.{DList, DObject, Data, ID}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, Ops, matchFailExn}

import scala.concurrent.{ExecutionContext, Future}
import Implicits._

// TODO: Document API
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
           case _ => throw IsabelleException("In ListConverter.retrieve: function result is not a DObject (internal error)")
         };
         list <- Future.traverse(listMLValue) {
           converter.retrieve(_)
         })
      yield list.toList
  }

  @inline override def exnToValue: String = s"fn E_List list => map (${converter.exnToValue}) list | ${matchFailExn("ListConverter.exnToValue")}"
  @inline override def valueToExn: String = s"E_List o map (${converter.valueToExn})"

  override def mlType: String = s"(${converter.mlType}) list"
}
