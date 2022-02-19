package de.unruh.isabelle.pure

import java.nio.file.{Path, Paths}

import de.unruh.isabelle.control.Isabelle.DString
import de.unruh.isabelle.control.{Isabelle, IsabelleMLException, OperationCollection}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.{MLRetrieveFunction, MLStoreFunction, MLValue}
import org.apache.commons.lang3.SystemUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

// Implicits
import Implicits._

object PathConverter extends MLValue.Converter[Path] with OperationCollection {
  def exceptionName(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Path"

  override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "Path.T"

  val slashRegex: Regex = "/".r

  override def retrieve(value: MLValue[Path])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Path] =
    for (DString(string) <- Ops.retrievePath(value))
      yield /*if (SystemUtils.IS_OS_WINDOWS) {
        slashRegex.split(string).toSeq match {
          case Seq("", "cygdrive", root, rest @ _*) =>
            Paths.get(root+":", rest :_*)
          case Seq("", rest @ _*) =>
            throw IsabelleException(s"Don't know how to translate $string to a Java path object. You may want to file a bug report for scala-isabelle if this is a valid path.")
          case Seq(first, rest @ _*) =>
            Paths.get(first, rest :_*)
          case Seq() =>
            throw IsabelleException(s"Don't know how to translate $string to a Java path object. You may want to file a bug report for scala-isabelle if this is a valid path.")
        }
      } else*/
        Paths.get(string)

  override def store(path: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Path] = {
    val string =
      if (SystemUtils.IS_OS_WINDOWS)
        Utils.cygwinPath(path)
      else
        path.toString
    Ops.storePath(DString(string))
  }

  override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = s"fn $exceptionName path => path"

  override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = exceptionName

  //noinspection TypeAnnotation
  protected class Ops(implicit isabelle: Isabelle, ec: ExecutionContext) {
//    lazy val retrievePath = MLRetrieveFunction[Path]("DString o Path.implode o Path.expand")
    lazy val retrievePath = MLRetrieveFunction[Path]("DString o File.platform_path")
    lazy val storePath = MLStoreFunction[Path]("fn DString path => Path.explode path")
  }
  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops
}
