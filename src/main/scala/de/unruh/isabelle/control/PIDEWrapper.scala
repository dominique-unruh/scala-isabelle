package de.unruh.isabelle.control

import de.unruh.isabelle.control.Isabelle.{cygwinIfWin, makeIsabelleCommandLine, makeIsabelleEnvironment}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.misc.Utils.optionalAsScala
import org.log4s

import java.io.{BufferedReader, InputStream, InputStreamReader, UncheckedIOException}
import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path}
import java.util.Optional
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.ListBuffer
import scala.sys.process.ProcessLogger

// DOCUMENT
// DOCUMENT must not reference any scala classes
abstract class PIDEWrapper {
  def killProcess(process: Process): Unit

  type Process <: AnyRef

  def startIsabelleProcess(cwd: Path, mlCode: String,
                           logic: String, sessionRoots: Array[Path],
                           build: Boolean, userDir: Optional[Path]): Process

  def jedit(cwd: Path, logic: String, sessionRoots: Array[Path],
            userDir: Optional[Path], files: Array[Path]) : Unit

  // DOCUMENT
  /** @return true if the process terminated normally (without error) */
  def waitForProcess(process: Process, progress_stdout: Consumer[String], progress_stderr: Consumer[String]): Boolean

  def catchException[A](exception: (String, Throwable) => IsabelleControllerException)(function: => A): A
}

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

object PIDEWrapper {
  private lazy val regex = """Isabelle(?<year>[0-9]+)(-(?<step>[0-9]+))?(-RC(?<rc>[0-9]+))?(.exe)?""".r.anchored

  lazy val pideWrapperJar2021: URL = getClass.getResource("pidewrapper2021.jar")

  def getDefaultPIDEWrapper(isabelleRoot: Path): PIDEWrapper = {
    val isabelleVersion =
      for (file <- Files.list(isabelleRoot).iterator().asScala.toSeq;
           name = file.getFileName.toString;
           matcher <- regex.findFirstMatchIn(name))
        yield matcher

    if (isabelleVersion.isEmpty)
      throw IsabelleSetupException(s"$isabelleRoot is not a valid Isabelle installation (does not contain a file named 'IsabelleVERSION')")
    if (isabelleVersion.length > 1)
      throw IsabelleSetupException(s"$isabelleRoot is not a valid Isabelle installation (contains multiple files named 'IsabelleVERSION': ${isabelleVersion map {_.matched} mkString ", "})")

    val year = isabelleVersion.head.group("year").toInt
    val step = isabelleVersion.head.group("step") match { case null => 0; case step => step.toInt }

    def fallbackPIDE() = {
      logger.warn(s"Isabelle version ${isabelleVersion.head.matched} unknown. Using PIDE wrapper code for Isabelle2021")
      getPIDEWrapperFromJar(isabelleRoot, pideWrapperJar2021)
    }

    (year, step) match {
      case (2021, 0) => getPIDEWrapperFromJar(isabelleRoot, pideWrapperJar2021)
      case (year, _) if year < 2021 => new PIDEWrapperCommandline(isabelleRoot)
      case (2021, step) if step > 0 => fallbackPIDE()
      case (year, _) if year > 2021 => fallbackPIDE()
    }
  }

  private val counter = new AtomicInteger()

  private val logger = log4s.getLogger

  def getPIDEWrapperFromJar(isabelleRoot: Path, pideWrapperJar: URL): PIDEWrapper = {
    val id = counter.incrementAndGet()

    val isabelleJars : List[URL] = for (file <- Files.walk(isabelleRoot).iterator().asScala.toList
                    if file.getFileName.toString.endsWith(".jar")
                    if Files.isRegularFile(file))
      yield file.toUri.toURL

    val jars = (pideWrapperJar :: isabelleJars).toArray[URL]

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

    val cmd = Isabelle.makeIsabelleCommandLine(isabelleRoot, isabelleArguments)

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

    val cmd = makeIsabelleCommandLine(isabelleRoot, isabelleArguments)

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

    val cmd = Isabelle.makeIsabelleCommandLine(isabelleRoot, isabelleArguments)

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
}