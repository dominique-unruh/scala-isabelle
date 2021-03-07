package de.unruh.isabelle.control

import org.log4s

import java.io.{BufferedReader, File}
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.ListBuffer

// TODO document
abstract class PIDEWrapper {
  def killProcess(process: Process): Unit
  type Process <: AnyRef
  def startIsabelleProcess(cwd: File = new File("").getAbsoluteFile, mlCode: String = "",
                           logic: String = "HOL"): Process

  def waitForProcess(process: Process, progress_stdout: Consumer[String], progress_stderr: Consumer[String]): Unit

//  def stdout(process: Process): BufferedReader
//  def stderr(process: Process): BufferedReader

  def catchException[A](exception: (String, Throwable) => IsabelleControllerException)(function: => A): A =
    try {
      function
    } catch {
      case e if e.getClass.getClassLoader == getClass.getClassLoader =>
        throw exception(e.getMessage, e)
    }
}

object PIDEWrapper {
  private lazy val regex = """Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?(.exe)?""".r.anchored

  def getPIDEWrapperJar(isabelleRoot: Path): URL = {
    val isabelleVersion =
      for (file <- Files.list(isabelleRoot).iterator().asScala.toSeq;
           name = file.getFileName.toString;
           matcher <- regex.findFirstMatchIn(name))
        yield matcher

    if (isabelleVersion.isEmpty)
      throw IsabelleSetupException(s"$isabelleRoot is not a valid Isabelle installation (does not contain a file named 'IsabelleVERSION')")
    if (isabelleVersion.length > 1)
      throw IsabelleSetupException(s"$isabelleRoot is not a valid Isabelle installation (contains multiple files named 'IsabelleVERSION': ${isabelleVersion map {_.matched} mkString(", ")})")

    val year = isabelleVersion.head.group("year").toInt
    val step = isabelleVersion.head.group("step") match { case null => 0; case step => step.toInt }

    val pideWrapperJar = (year, step) match {
      case (2021, 0) => "pidewrapper2021.jar"
      case _ =>
        // TODO
        ???
    }

    val pideWrapperJarURL = getClass.getResource(pideWrapperJar)
    assert(pideWrapperJarURL != null)

    pideWrapperJarURL
  }

  private val counter = new AtomicInteger()

  private val logger = log4s.getLogger

  def getPIDEWrapper(isabelleRoot: Path, pideWrapperJar: URL = null): PIDEWrapper = {
    val id = counter.incrementAndGet()

    val pideWrapperJar2 = if (pideWrapperJar==null) getPIDEWrapperJar(isabelleRoot) else pideWrapperJar

    val isabelleJars : List[URL] = for (file <- Files.walk(isabelleRoot).iterator().asScala.toList
                    if file.getFileName.toString.endsWith(".jar")
                    if Files.isRegularFile(file))
      yield file.toUri.toURL

    val jars = (pideWrapperJar2 :: isabelleJars).toArray[URL]

    val filteredClassloader = new ClassLoader(getClass.getClassLoader) {
      override def loadClass(name: String, resolve: Boolean): Class[_] = {
        if (name.startsWith("scala"))
          throw new ClassNotFoundException(name)
        super.loadClass(name, resolve)
      }
    }

    logger.debug(s"Initialized classloader PIDEWrapper@$id: ${pideWrapperJar2}, ${isabelleRoot}")

    val classloader = new URLClassLoader(s"PIDEWrapper@$id", jars, filteredClassloader)

    classloader
      .loadClass("PIDEWrapperImpl")
      .getDeclaredConstructor(classOf[Path])
      .newInstance(isabelleRoot).asInstanceOf[PIDEWrapper]
  }
}
