package de.unruh.isabelle.control

import de.unruh.isabelle.control.Isabelle.{cygwinIfWin, makeIsabelleEnvironment}
import de.unruh.isabelle.control.PIDEWrapperCommandline.makeIsabelleCommandLine
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.misc.Utils.optionalAsScala
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.annotations.NotNull
import org.log4s

import java.io.{BufferedReader, InputStream, InputStreamReader, UncheckedIOException}
import java.net.{URL, URLClassLoader}
import java.nio.file.{FileVisitOption, Files, NoSuchFileException, Path}
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.ListBuffer
import scala.sys.process.ProcessLogger

/** A class that wraps a mechanism to instantiate an Isabelle process.
 *
 * This class is an internal implementation detail of scala-isabelle and should not be considered part of
 * the public facing API. In particular, the methods in this class may change without notice.
 *
 * An instance of this class suitable for instantiating a given Isabelle version can be created using
 * [PIDEWrapper.getDefaultPIDEWrapper].
 *
 * Different implementations could either invoke the Isabelle process on the command line ([PIDEWrapperCommandline]),
 * or directly via Isabelle/Scala methods. The latter is handled by subclasses of [PIDEWrapperViaClassloader]
 * that are loaded through a fresh classloader that has all JARs needed by Isabelle/Scala on its classpath.
 * (A fresh classloader is needed because Isabelle/Scala might expect a different version of the scala library that
 * scala-isabelle is running with.)
 *
 * This abstract class intentionally does not refer to any classes from the Scala library. This is because the classes
 * that access the PIDEWrapper instance may be running with a difference instance of the Scala library than
 * the the class that implements this abstract class.
 **/
abstract class PIDEWrapper {
  /** Stop the running Isabelle process. (Possibly forcefully.) */
  def killProcess(process: Process): Unit

  /** A type that represents a running Isabelle process. */
  type Process <: AnyRef

  // DOCUMENT
  def startIsabelleProcess(cwd: Path, mlCode: String,
                           logic: String, sessionRoots: Array[Path],
                           build: Boolean, userDir: Optional[Path]): Process

  // DOCUMENT
  def jedit(cwd: Path, logic: String, sessionRoots: Array[Path],
            userDir: Optional[Path], files: Array[Path]) : Unit

  // DOCUMENT
  /** @return true if the process terminated normally (without error) */
  def waitForProcess(process: Process, progress_stdout: Consumer[String], progress_stderr: Consumer[String]): Boolean

  // DOCUMENT
  def catchException[A](exception: (String, Throwable) => IsabelleControllerException)(function: => A): A
}

// DOCUMENT
abstract class PIDEWrapperViaClassloader extends PIDEWrapper {
  def catchException[A](exception: (String, Throwable) => IsabelleControllerException)(function: => A): A =
    try {
      function
    } catch {
      case e if (!e.isInstanceOf[IsabelleControllerException]) &&
        e.getClass.getClassLoader == getClass.getClassLoader =>
        throw exception(e.getMessage, e)
    }
}

// DOCUMENT
object PIDEWrapper {
  private lazy val regex = """Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?(.exe)?""".r.anchored

  @NotNull lazy val pideWrapperJar2021: URL = getClass.getResource("pidewrapper2021.jar")
  @NotNull lazy val pideWrapperJar2021_1: URL = getClass.getResource("pidewrapper2021-1.jar")

  def getDefaultPIDEWrapper(isabelleRoot: Path): PIDEWrapper = {
    if (!Files.exists(isabelleRoot))
      throw new NoSuchFileException(isabelleRoot.toString, null, "Isabelle home not found while initializing PIDE wrapper")
    val isabelleVersion =
      for (file <- Files.list(isabelleRoot).iterator().asScala.toSeq;
           name = file.getFileName.toString;
           matcher <- regex.findFirstMatchIn(name);
           year = matcher.group("year").toInt;
           step = matcher.group("step") match { case null => 0; case step => step.toInt })
        yield (name, (year, step))

    if (isabelleVersion.isEmpty)
      throw IsabelleSetupException(s"$isabelleRoot is not a valid Isabelle installation (does not contain a file named 'IsabelleVERSION')")

    val isabelleVersion2 = isabelleVersion.map(_._2).toSet

    if (isabelleVersion2.size > 1)
      throw IsabelleSetupException(s"$isabelleRoot is not a valid Isabelle installation (contains multiple files named 'IsabelleVERSION': ${isabelleVersion map {_._1} mkString ", "})")

    def fallbackPIDE() = {
      logger.warn(s"Isabelle version ${isabelleVersion.head._1} unknown. Using PIDE wrapper code for Isabelle2021-1")
      getPIDEWrapperFromJar("2021-1", isabelleRoot, pideWrapperJar2021_1)
    }

    isabelleVersion2.head match {
      case (2021, 0) => getPIDEWrapperFromJar("2021", isabelleRoot, pideWrapperJar2021)
      case (2021, 1) => getPIDEWrapperFromJar("2021-1", isabelleRoot, pideWrapperJar2021_1)
      case (year, _) if year < 2021 => new PIDEWrapperCommandline(isabelleRoot)
      case (2021, step) if step > 0 => fallbackPIDE()
      case (year, _) if year > 2021 => fallbackPIDE()
    }
  }

  private val counter = new AtomicInteger()

  private val logger = log4s.getLogger

  private def resourceToFile(@NotNull resource: URL): URL = {
    if (resource.getProtocol == "file") return resource
    else {
      val tempFile = Files.createTempFile("pidewrapper-temp", ".jar")
      FileUtils.copyURLToFile(resource, tempFile.toFile)
      tempFile.toUri.toURL
    }
  }

  def getPIDEWrapperFromJar(@NotNull version: String, @NotNull isabelleRoot: Path, @NotNull pideWrapperJar: URL): PIDEWrapper = {
    if (pideWrapperJar == null)
      throw new UnsupportedOperationException(s"PIDE Wrapper for Isabelle version $version not included in scala-isabelle. Set buildOnlyFor = None in build.sbt and recompile scala-isabelle.")

    val id = counter.incrementAndGet()

    val isabelleJars : List[URL] = for (file <- Files.walk(isabelleRoot, FileVisitOption.FOLLOW_LINKS).iterator().asScala.toList
                    if file.getFileName.toString.endsWith(".jar")
                    if Files.isRegularFile(file))
      yield file.toUri.toURL

    // Using resourceToFile(pideWrapperJar) because URLClassLoader cannot handle jars in jars.
    val jars = (resourceToFile(pideWrapperJar) :: isabelleJars).toArray[URL]

    val filteredClassloader = new ClassLoader(getClass.getClassLoader) {
      override def loadClass(name: String, resolve: Boolean): Class[_] = {
        if (name.startsWith("scala"))
          throw new ClassNotFoundException(name)
        super.loadClass(name, resolve)
      }
    }

    logger.debug(s"Initialized classloader PIDEWrapper@$id: $pideWrapperJar, $isabelleRoot")

    val classloader = new URLClassLoader(s"PIDEWrapper@$id", jars, filteredClassloader)

    classloader
      .loadClass("PIDEWrapperImpl")
      .getDeclaredConstructor(classOf[Path])
      .newInstance(isabelleRoot).asInstanceOf[PIDEWrapper]
  }
}

// DOCUMENT
class PIDEWrapperCommandline(val isabelleRoot: Path) extends PIDEWrapper {
  import de.unruh.isabelle.control.PIDEWrapperCommandline.logger

  override def killProcess(process: Process): Unit =
    Utils.destroyProcessThoroughly(process)

  override type Process = java.lang.Process

  /** DOCUMENT
   *
   *  All paths must be absolute. */
  override def startIsabelleProcess(cwd: Path, mlCode: String, logic: String, sessionRoots: Array[Path], build: Boolean,
                                    userDir: Optional[Path]): Process = {
    if (build)
      buildSession(wd = cwd, logic = logic, sessionRoots = sessionRoots, userDir = Utils.optionalAsScala(userDir))

    val isabelleArguments = ListBuffer[String]()

    isabelleArguments += "process"
    isabelleArguments += "-l" += logic

    val mlCodeFile = Files.createTempFile("isabellecontrol", ".ML").toAbsolutePath
    logger.debug(s"ML code file: $mlCodeFile")
    mlCodeFile.toFile.deleteOnExit()
    Files.writeString(mlCodeFile, mlCode)

    isabelleArguments += "-f" += mlCodeFile.toAbsolutePath.toString.replace('\\', '/')

    for (root <- sessionRoots)
      isabelleArguments += "-d" += cygwinIfWin(root)

    val cmd = makeIsabelleCommandLine(isabelleRoot, isabelleArguments.toSeq)

    logger.debug(s"Cmd line: ${cmd.mkString(" ")}")

    val processBuilder = new java.lang.ProcessBuilder(cmd :_*)
    processBuilder.directory(cwd.toFile)
    for ((k,v) <- Isabelle.makeIsabelleEnvironment(Utils.optionalAsScala(userDir)))
      processBuilder.environment().put(k,v)

    val process = processBuilder.start()

    process
  }


  /** DOCUMENT
   *
   *  All paths must be absolute. */
  def jedit(cwd: Path, logic: String, sessionRoots: Array[Path],
            userDir: Optional[Path], files: Array[Path]) : Unit = {
    val isabelleArguments = ListBuffer[String]()

    isabelleArguments += "jedit"

    for (root <- sessionRoots)
      isabelleArguments += "-d" += cygwinIfWin(root)

    isabelleArguments += "-l" += logic

    isabelleArguments += "--"
    isabelleArguments ++= files.map(cygwinIfWin)

    val cmd = makeIsabelleCommandLine(isabelleRoot, isabelleArguments.toSeq)

    logger.debug(s"Cmd line: ${cmd.mkString(" ")}")

    val processBuilder = scala.sys.process.Process(cmd, cwd.toFile,
      makeIsabelleEnvironment(optionalAsScala(userDir)) :_*)

//    val lock = buildLocks.get(absPath(setup.isabelleHome).normalize).readLock
//    lock.lockInterruptibly()
//    try {
    if (0 != processBuilder.!(ProcessLogger(line => logger.debug(s"Isabelle jedit: $line"),
      { line => logger.warn(s"Isabelle jedit: $line") })))
      throw IsabelleJEditException("Could not start Isabelle/jEdit")
//    } finally
//      lock.unlock()
  }

  /** Runs the Isabelle build process to build the session heap image `setup.logic`
   *
   * This is done automatically by the constructors of [[Isabelle]] unless `build=false`.
   */
  private def buildSession(wd: Path, logic: String, sessionRoots: Array[Path], userDir: Option[Path]) : Unit = {
    val isabelleArguments = ListBuffer[String]()

    isabelleArguments += "build"
    isabelleArguments += "-b" // Build heap image
    isabelleArguments += "-v" // Verbose build

    for (root <- sessionRoots)
      isabelleArguments += "-d" += Isabelle.cygwinIfWin(root)

    isabelleArguments += logic

    val cmd = makeIsabelleCommandLine(isabelleRoot, isabelleArguments.toSeq)

    logger.debug(s"Cmd line: ${cmd.mkString(" ")}")

    val processBuilder = scala.sys.process.Process(cmd, wd.toAbsolutePath.toFile,
      Isabelle.makeIsabelleEnvironment(userDir): _*)
    val errors = ListBuffer[String]()

    if (0 != processBuilder.!(ProcessLogger(line => logger.debug(s"Isabelle build: $line"),
      { line => errors.append(line); logger.warn(s"Isabelle build: $line") })))
      throw IsabelleBuildException(s"Isabelle build for session $logic failed", errors.toList)
  }

  override def waitForProcess(process: Process, progress_stdout: Consumer[String],
                              progress_stderr: Consumer[String]): Boolean = {
    def logStream(stream: InputStream, progress: Consumer[String]) : Unit =
      Utils.runAsDaemonThread(s"Isabelle output logger", () =>
        try
          new BufferedReader(new InputStreamReader(stream)).lines().forEach(line => progress.accept(line.stripLineEnd))
        catch {
          case _: UncheckedIOException =>
          // Can happen if the stream is closed. Ignore
        }
      )

    logStream(process.getInputStream, progress_stdout)
    logStream(process.getErrorStream, progress_stderr)

    val exitCode = process.waitFor()

    exitCode == 0
  }

  override def catchException[A](exception: (String, Throwable) => IsabelleControllerException)(function: => A): A =
    function
}

object PIDEWrapperCommandline {
  private val logger = log4s.getLogger

  private def makeIsabelleCommandLine(isabelleHome: Path, arguments: Seq[String]) : Seq[String]= {
    if (SystemUtils.IS_OS_WINDOWS) {
      val bash = isabelleHome.resolve("contrib").resolve("cygwin").resolve("bin").resolve("bash").toString
      val isabelle = Utils.cygwinPath(isabelleHome.resolve("bin").resolve("isabelle"))
      List(bash, "--login", "-c",
        (List(isabelle) ++ arguments).map(StringEscapeUtils.escapeXSI).mkString(" "))
    } else
      List(isabelleHome.resolve("bin").resolve("isabelle").toString) ++ arguments
  }
}