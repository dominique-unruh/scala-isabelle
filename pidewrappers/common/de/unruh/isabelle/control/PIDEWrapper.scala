package de.unruh.isabelle.control

import java.io.{BufferedReader, File}
import java.net.{URLClassLoader, URL}
import java.nio.file.{Files, Path, Paths}
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.ListBuffer

// TODO document
trait PIDEWrapper {
  def killProcess(process: Process): Unit
  type Process <: AnyRef
  def startIsabelleProcess(cwd: File = new File("").getAbsoluteFile, mlCode: String = "",
                           logic: String = "HOL"): Process
  def result(process: Process): Unit
  def stdout(process: Process): BufferedReader
  def stderr(process: Process): BufferedReader
}

object PIDEWrapper {
  private lazy val regex = """Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?(.exe)?""".r.anchored

  def getPIDEWrapper(isabelleRoot: Path): PIDEWrapper = {
    val isabelleVersion =
      for (file <- Files.list(isabelleRoot).iterator().asScala.toSeq;
           name = file.getFileName.toString;
           matcher <- regex.findFirstMatchIn(name))
        yield matcher

    if (isabelleVersion.isEmpty)
      throw new RuntimeException(s"$isabelleRoot is not a valid Isabelle installation (does not contain a file named 'IsabelleVERSION')")
    if (isabelleVersion.length > 1)
      throw new RuntimeException(s"$isabelleRoot is not a valid Isabelle installation (contains multiple files named 'IsabelleVERSION': ${isabelleVersion map {_.matched} mkString(", ")})")

    val year = isabelleVersion.head.group("year").toInt
    val step = isabelleVersion.head.group("step") match { case null => 0; case step => step.toInt }

    val pideWrapperJar = (year, step) match {
      case (2021, 0) => "pidewrapper2021.jar"
      case _ =>
        // TODO
        ???
    }

    val isabelleJars : List[URL] = for (file <- Files.walk(isabelleRoot).iterator().asScala.toList
                    if file.getFileName.toString.endsWith(".jar")
                    if Files.isRegularFile(file))
      yield file.toUri.toURL

    val pideWrapperJarURL = getClass.getResource(pideWrapperJar)
    assert(pideWrapperJarURL != null)

    val jars = (pideWrapperJarURL :: isabelleJars).toArray[URL]

    val classloader = new URLClassLoader(
      s"Isabelle${isabelleVersion} PIDE", jars, getClass.getClassLoader)

    classloader
      .loadClass("PIDEWrapperImpl")
      .getDeclaredConstructor(classOf[Path])
      .newInstance(isabelleRoot).asInstanceOf[PIDEWrapper]
  }
}
