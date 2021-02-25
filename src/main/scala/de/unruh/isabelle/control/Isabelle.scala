package de.unruh.isabelle.control

import java.io.{BufferedReader, BufferedWriter, DataInputStream, DataOutputStream, EOFException, FileInputStream, FileOutputStream, FileWriter, IOException, InputStream, InputStreamReader, OutputStream, OutputStreamWriter, UncheckedIOException}
import java.lang
import java.lang.ref.Cleaner
import java.net.{InetAddress, ServerSocket, Socket}
import java.nio.charset.StandardCharsets
import java.nio.file.{FileSystemNotFoundException, Files, Path, Paths}
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue, ConcurrentHashMap, ConcurrentLinkedQueue}

import com.google.common.escape.Escaper
import com.google.common.util.concurrent.Striped
import de.unruh.isabelle.control.Isabelle.SetupGeneral
import de.unruh.isabelle.misc.SMLCodeUtils.mlInteger
import de.unruh.isabelle.misc.{FutureValue, SMLCodeUtils, SharedCleaner, Utils}
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.pure.Term
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.commons.text.StringEscapeUtils
import org.jetbrains.annotations.ApiStatus.Experimental
import org.log4s
import org.log4s.{Debug, LogLevel, Logger, Warn}

import scala.annotation.tailrec
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.io.Source
import scala.sys.process.{Process, ProcessLogger}
import scala.util.{Failure, Random, Success, Try}

/**
  * A running instance of Isabelle.
  *
  * The Isabelle process is initialized with some ML code that allows this class to remote control Isabelle. In more detail:
  *
  * The Isabelle process maintains a map from IDs to values (the "object store").
  * Those values can be of any type (e.g., integers, terms, functions, etc.).
  * (How this works in a strongly typed language such as ML is described below.)
  * The Isabelle class has functions to operate on the values in the object store
  * (e.g., creating new objects, retrieving the value of an object, performing operations on objects).
  *
  * The operations provided by this class are very lowlevel. For more convenient and type-safe operations on
  * values in the object store, see [[de.unruh.isabelle.mlvalue.MLValue]].
  *
  * Operations on objects are asynchronous and return futures.
  *
  * On the Scala side, the IDs of objects are represented by the class [[de.unruh.isabelle.control.Isabelle.ID]].
  * These IDs ensure garbage collection – if an ID is not referenced any more on the Scala side, it will
  * be removed from the object store in the Isabelle process, too.
  *
  * To be able to store objects of different types in the object store, even though ML does not support subtyping,
  * we make use of the fact that all exceptions share the same ML type `exn`. The object store stores only values of
  * type `exn`. To store, e.g., integers, we define an exception `exception E_Int of int`. Then for an integer `i`,
  * `E_Int i` is an exception that can be stored in the object store. We can convert the exception back to an integer
  * using the function `fn E_Int i => i` that uses pattern matching. (This will raise `Match` if the given exception is
  * does not contain an integer. This way we achieve dynamic typing of our object store.) The following exceptions are
  * predefined in structure `Control_Isabelle`:
  * {{{
  * exception E_Function of exn -> exn
  * exception E_Int of int
  * exception E_String of string
  * exception E_Pair of exn * exn
  * }}}
  * (That structure also exports functions `store` and `handleLines` which are for internal use only
  * and must not be used in the ML code.)
  *
  * Note that some of the exception again refer to the exn type, e.g., `E_Pair`. Thus, to store a pair of integers,
  * we use the term `E_Pair (E_Int 1, E_Int 2)`.
  *
  * New exceptions for storing other types can be defined at runtime using [[executeMLCode]].
  *
  * @constructor The constructor initialize the Isabelle instance partly asynchronously. That is, when the
  *              constructor returns successfully, it is not guaranteed that the Isabelle process was initialized
  *              successfully. To check and wait for successful initialization, use the methods from
  *              [[mlvalue.FutureValue FutureValue]] (supertrait of this class), e.g.,
  *              `new Isabelle(...).`[[mlvalue.FutureValue.force force]].
  *
  * @param setup Configuration object that specifies the path of the Isabelle binary etc. See [[de.unruh.isabelle.control.Isabelle.SetupGeneral]]. This also
  *              specifies with Isabelle heap to load.
  */
class Isabelle(val setup: SetupGeneral) extends FutureValue {
  import Isabelle._

  private val sendQueue : BlockingQueue[(DataOutputStream => Unit, Try[Data] => Unit)] = new ArrayBlockingQueue(1000)
  private val callbacks : ConcurrentHashMap[Long, Try[Data] => Unit] = new ConcurrentHashMap()

  private val inSecret = Random.nextLong()
  private val outSecret = Random.nextLong()

  /** This promise will be completed when initializing the Isabelle process finished (first successful communication).
   * Contains an exception if initilization fails. */
  private val initializedPromise : Promise[Unit] = Promise()
  def someFuture: Future[Unit] = initializedPromise.future
  def await: Unit = Await.result(initializedPromise.future, Duration.Inf)

  // Must be Integer, not Int, because ConcurrentLinkedList uses null to indicate that it is empty
  private val garbageQueue = new ConcurrentLinkedQueue[java.lang.Long]()

  // Must not contain any references to this Isabelle instance, even indirectly (otherwise the cleaner will not be called)
  private val destroyActions = new ConcurrentLinkedQueue[Runnable]()
  // Ensures that process will be terminated if this object is garbage collected
  SharedCleaner.register(this, new Isabelle.ProcessCleaner(destroyActions))

  private def garbageCollect(stream: DataOutputStream) : Boolean = {
    //    println("Checking for garbage")
    if (garbageQueue.peek() == null)
      false
    else {
      val buffer = ListBuffer[Data]()

      @tailrec def drain(): Unit = garbageQueue.poll() match {
        case null =>
        case obj =>
          buffer += DInt(obj)
          drain()
      }

      drain()
      logger.debug(s"Sending GC command to Isabelle, ${buffer.size} freed objects")
      stream.writeByte(8)
      writeData(stream, DList(buffer.toSeq :_*))
      true
    }
  }

  private def processQueue(isabelleInput: OutputStream) : Unit = try {
    logger.debug("Process queue thread started")
    destroyActions.add(() => isabelleInput.close())

    val stream = new DataOutputStream(isabelleInput)
    var count = 0

    stream.writeLong(inSecret)
    stream.flush()

    // Make sure we do not send any data before security check has finished (in case the data is secret)
    Await.result(initializedPromise.future, Duration.Inf)

    def sendLine(line: DataOutputStream => Unit, callback: Try[Data] => Unit): Unit = {
      if (callback != null)
        callbacks.put(count, callback)
      line(stream)
      count += 1
    }

    @tailrec @inline
    def drainQueue() : Unit = {
      val elem = sendQueue.poll()
      if (elem!=null) {
        val (line,callback) = elem
        sendLine(line,callback)
        drainQueue()
      }
    }

    while (true) {
      val (line,callback) = sendQueue.take()
      sendLine(line, callback)
      drainQueue()
      if (garbageCollect(stream))
        count += 1
      stream.flush()
    }
  } catch {
    case e : IsabelleDestroyedException =>
      destroy(e)
      throw e
    case e : Throwable =>
      destroy(IsabelleDestroyedException(e))
      throw e
  }

  private def readString(stream: DataInputStream): String = {
    val len = stream.readInt()
    val bytes = stream.readNBytes(len)
    new String(bytes, StandardCharsets.US_ASCII)
  }

  private def writeString(stream: DataOutputStream, str: String): Unit = {
    val bytes = str.getBytes(StandardCharsets.US_ASCII)
    stream.writeInt(bytes.length)
//    logger.debug(s"length: ${bytes.length}, content: ${new String(bytes)}")
    stream.write(bytes)
  }

  private def writeData(stream: DataOutputStream, data: Data): Unit = data match {
    case DInt(i) => stream.writeByte(1); stream.writeLong(i)
    case DString(s) => stream.writeByte(2); writeString(stream, s)
    case DList(list@_*) =>
      stream.writeByte(3)
      stream.writeLong(list.length)
      for (d <- list)
        writeData(stream, d)
    case DObject(id) =>
      stream.writeByte(4)
      stream.writeLong(id.id)
  }

  private def readData(stream: DataInputStream): Data = {
    stream.readByte() match {
      case 1 => DInt(stream.readLong())
      case 2 => DString(readString(stream))
      case 3 =>
        val len = stream.readLong()
        val list = ListBuffer[Data]()
        for (_ <- 1L to len)
          list += readData(stream)
        DList(list.toSeq:_*)
      case 4 =>
        val id = stream.readLong()
        DObject(new ID(id, this))
    }
  }


  /** Message format:
   *
   * int|1b|data - success response for command #int
   * int|2b|string - failure response for command #int
   *
   * 1b,2b,...: byte literals
   *
   * int64: 64 msb-first signed integer
   *
   * data: binary representation of [[Data]]:
   *   1b|int64 - DInt
   *   2b|string - DString (must be ASCII)
   *   3b|int64|data|data|... - DTree (int64 = # of data)
   *   4b|int64 - DObject (responsibility for GC is on Scala side)
   *
   * string: int32|bytes
   *
   **/
  private def parseIsabelle(isabelleOutput: InputStream) : Unit = try {
    val output = new DataInputStream(isabelleOutput)
    destroyActions.add(() => isabelleOutput.close())

    val outSecret2 = output.readLong()
    if (outSecret != outSecret2) throw IsabelleProtocolException("Got incorrect secret value from Isabelle process")
    initializedPromise.success(())

    def missingCallback(seq: Long, answerType: Int): Unit = {
      var exn : IsabelleProtocolException = null
      try {
        if (answerType == 2) {
          import ExecutionContext.Implicits.global
          val msg = Await.result(Future { readString(output) }, Duration(5, scala.concurrent.duration.SECONDS))
          exn = IsabelleProtocolException(s"Received a protocol response from Isabelle with seq# $seq " +
            s"but no callback is registered for that seq#. Probably the communication is out of sync now. " +
            s"The response indicated the following exception: $msg")
        }
      } catch { case _ : Throwable => }
      if (exn == null)
        exn = IsabelleProtocolException(s"Received a protocol response from Isabelle with seq# $seq, answerType $answerType, " +
          s"but no callback is registered for that seq#. Probably the communication is out of sync now")
      throw exn
    }

    while (true) {
      val seq = output.readLong()
      val answerType = output.readByte()
      val callback = callbacks.remove(seq)
      //      logger.debug(s"Seq: $seq, type: $answerType, callback: $callback")
      answerType match {
        case 1 =>
          if (callback==null) missingCallback(seq, answerType)
          val payload = readData(output)
          callback(Success(payload))
        case 2 =>
          if (callback==null) missingCallback(seq, answerType)
          val msg = readString(output)
          callback(Failure(IsabelleException(msg)))
        case 3 =>
          if (seq != 0) IsabelleProtocolException(s"Received a protocol response from Isabelle with seq# $seq and " +
            s"answerType $answerType. Seq should be 0. Probably the communication is out of sync now.")
          val payload = readData(output)
          ExecutionContext.global.execute { () =>
            try {
              setup.isabelleCommandHandler(payload)
            } catch {
              case e: Throwable =>
                logger.error(e)("Exception in Isabelle command handler")
            }
          }
        case _ =>
          throw IsabelleProtocolException(s"Received a protocol response from Isabelle with seq# $seq and invalid" +
            s"answerType $answerType. Probably the communication is out of sync now")
      }
    }
  } catch {
    case e : Throwable =>
      if (destroyed == null) { // Do not report the exception if the process is already supposed to be destroyed.
        destroy(IsabelleDestroyedException(e))
        throw e
      }
  }

  //noinspection SameParameterValue
  private def filePathFromResource(name: String, tmpDir: Path, replace: String => String): Path = {
    val url = getClass.getResource(name)
    val tmpPath = tmpDir.resolve(name.split('/').last)
    val tmpFile = tmpPath.toFile
    tmpFile.deleteOnExit()
    val source = Source.fromURL(url)
    val writer = new FileWriter(tmpFile)
    for (line <- source.getLines())
      writer.write(replace(line)+"\n")
    writer.close()
    source.close()
    tmpPath
  }

  /** Invokes the Isabelle process.
   * */
  private def startProcessSlave(setup: Setup) : java.lang.Process = {
    implicit val s: Setup = setup
    def wd = setup.workingDirectory
    val useSockets = SystemUtils.IS_OS_WINDOWS

    val tempDir = Files.createTempDirectory("isabellecontrol").toAbsolutePath
    tempDir.toFile.deleteOnExit()
    logger.debug(s"Temp directory: $tempDir")

    assert(setup.userDir.forall(_.endsWith(".isabelle")))

    val (inputPipe, outputPipe) =
      if (useSockets)
        (null, null)
      else {
        val inputPipe = tempDir.resolve("in-fifo").toAbsolutePath
        inputPipe.toFile.deleteOnExit()
        val outputPipe = tempDir.resolve("out-fifo").toAbsolutePath
        outputPipe.toFile.deleteOnExit()
        if (Process(List("mkfifo", inputPipe.toString)).! != 0)
          throw new IOException(s"Cannot create fifo $inputPipe")
        if (Process(List("mkfifo", outputPipe.toString)).! != 0)
          throw new IOException(s"Cannot create fifo $outputPipe")
        (inputPipe, outputPipe)
      }

    val serverSocket =
      if (useSockets) new ServerSocket(0,0,InetAddress.getLoopbackAddress)
      else null

    lazy val (input,output) =
      if (useSockets) {
        val socket = serverSocket.accept();
        (socket.getOutputStream, socket.getInputStream)
      } else {
        (new FileOutputStream(inputPipe.toFile), new FileInputStream(outputPipe.toFile))
      }

    val isabelleArguments = ListBuffer[String]()

    isabelleArguments += "process"
    isabelleArguments += "-l" += setup.logic

    val communicationStreams = if (useSockets) {
      val address = s"${serverSocket.getInetAddress.getHostAddress}:${serverSocket.getLocalPort}"
      s"""Socket_IO.open_streams ("$address")"""
    } else {
      import de.unruh.isabelle.misc.SMLCodeUtils.escapeSml
      val inFile = escapeSml(inputPipe.toString)
      val outFile = escapeSml(outputPipe.toString)
      s"""(BinIO.openIn "$inFile", BinIO.openOut "$outFile")"""
    }

    val mlFile = filePathFromResource("control_isabelle.ml", tempDir,
      _.replace("COMMUNICATION_STREAMS", communicationStreams)
        .replace("SECRETS", s"(${mlInteger(inSecret)}, ${mlInteger(outSecret)})"))

    isabelleArguments += "-f" += mlFile.toAbsolutePath.toString.replace('\\', '/')

    isabelleArguments += "-e" += "Control_Isabelle.handleLines()"

    for (root <- setup.sessionRoots)
      isabelleArguments += "-d" += cygwinAbs(root)

    val cmd = makeIsabelleCommandLine(absPath(setup.isabelleHome), isabelleArguments.toSeq)

    logger.debug(s"Cmd line: ${cmd.mkString(" ")}")

    val processBuilder = new java.lang.ProcessBuilder(cmd :_*)
    processBuilder.directory(wd.toAbsolutePath.toFile)
    for ((k,v) <- makeIsabelleEnvironment) processBuilder.environment().put(k,v)

    val processQueueThread = new Thread("Send to Isabelle") {
      override def run(): Unit = processQueue(input) }
    processQueueThread.setDaemon(true)
    processQueueThread.start()

    val parseIsabelleThread = new Thread("Read from Isabelle") {
      override def run(): Unit = parseIsabelle(output) }
    parseIsabelleThread.setDaemon(true)
    parseIsabelleThread.start()

    val lock = Isabelle.buildLocks.get(absPath(setup.isabelleHome).normalize).readLock

    lock.lockInterruptibly()
    try {
      val process = processBuilder.start()
      destroyActions.add(() => Utils.destroyProcessThoroughly(process))

      logStream(process.getErrorStream, Warn) // stderr
      logStream(process.getInputStream, Debug) // stdout

      process.onExit.thenRun(() => processTerminated())

      process
    } finally {
      // This happens almost immediately, so it would be possible that a build process starts *after*
      // we initiated the Isabelle process. So ideally, the lock.unlock() should be delayed until we know that
      // the current Isabelle process has loaded any files that would be written by a build. But this is
      // a very exotic situation, so we just release the lock right away.
      lock.unlock()
    }
  }

  private def startProcessRunning(setup: SetupRunning) : java.lang.Process = {
    val inputPipe = setup.inputPipe
    val outputPipe = setup.outputPipe

    if (!Files.exists(inputPipe))
      throw IsabelleProtocolException(s"Input pipe $inputPipe does not exist")
    if (!Files.exists(outputPipe))
      throw IsabelleProtocolException(s"Output pipe $outputPipe does not exist")

    val processQueueThread = new Thread("Send to Isabelle") {
      override def run(): Unit = processQueue(new FileOutputStream(inputPipe.toFile)) }
    processQueueThread.setDaemon(true)
    processQueueThread.start()

    val parseIsabelleThread = new Thread("Read from Isabelle") {
      override def run(): Unit = parseIsabelle(new FileInputStream(outputPipe.toFile)) }
    parseIsabelleThread.setDaemon(true)
    parseIsabelleThread.start()

    return null; // No process
  }

  setup match {
    case setup : Setup => if (setup.build) buildSession(setup)
    case _ => }
  private val process: lang.Process = setup match {
    case setup : Setup => startProcessSlave(setup)
    case setup : SetupRunning => startProcessRunning(setup)
  }

  /** Returns whether the Isabelle process has been destroyed (via [[destroy]]) */
  def isDestroyed: Boolean = destroyed != null

  @volatile private var destroyed : IsabelleDestroyedException = _

  /** Kills the running Isabelle process.
   * After this, no more operations on values in the object store are possible.
   * Futures corresponding to already running computations will throw an [[IsabelleDestroyedException]].
   */
  def destroy(): Unit = destroy(IsabelleDestroyedException("Isabelle process has been destroyed"))

  private def destroy(cause: IsabelleDestroyedException): Unit = {
    if (destroyed != null) return

    destroyed = cause

    try initializedPromise.complete(Failure(cause))
    catch { case _ : IllegalStateException => }

    garbageQueue.clear()
    new ProcessCleaner(destroyActions).run()

    def callCallback(cb: Try[Data] => Unit): Unit =
      cb(Failure(cause))

    for ((_,cb) <- sendQueue.asScala)
      callCallback(cb)
    sendQueue.clear()

    for (cb <- callbacks.values.asScala)
      callCallback(cb)
  }

  private def processTerminated() : Unit = {
    logger.debug("Isabelle process terminated")
    val exitValue = process.exitValue
    if (exitValue == 0)
      destroy(IsabelleDestroyedException(s"Isabelle process terminated normally"))
    else {
      Thread.sleep(500) // To ensure the error processing thread has time to store lastMessage
      destroy(IsabelleDestroyedException(
        (s"Isabelle process failed with exit value $exitValue, last lines of output:" ::
          lastMessages.toList.map("> "+_)).mkString("\n")))
    }
  }

  /** Throws an [[IsabelleDestroyedException]] if this Isabelle process has been destroyed.
   * Otherwise does nothing. */
  @throws[IsabelleDestroyedException]("if the process was destroyed")
  def checkDestroyed(): Unit = {
    if (destroyed != null) {
      val exn = IsabelleDestroyedException(destroyed.message)
      if (destroyed.getCause != null) exn.initCause(destroyed.getCause)
      throw exn
    }
  }

  private def send(str: DataOutputStream => Unit, callback: Try[Data] => Unit) : Unit = {
    checkDestroyed()
    sendQueue.put((str,callback))
  }

  /** Executes the ML code `ml` in the Isabelle process.
   *
   * WARNING: This has a global effect on the Isabelle process because it modifies the ML name space.
   *
   * Definitions made in `ml` affect the global Isabelle name space.
   * This is intended mostly for defining new types.
   * To create values (e.g., if `ml` is the code of a function that should be executed),
   * preferably use [[storeValue]] which creates anonymous values in the object store.
   * The ML code is executed in a context where the structure `Control_Isabelle` is not opened
   * (i.e., you have to write `Control_Isabelle.E_Int` instead of `E_Int`).
   *
   * @return A future that completes when the code was executed.
   *         (Or throws an [[IsabelleControllerException]] if the ML code compilation/execution fails.)
   */
  def executeMLCode(ml : String) : Future[Unit] = {
    val promise : Promise[Unit] = Promise()
    logger.debug(s"Executing ML code: $ml")
    send({ stream => stream.writeByte(1); writeString(stream, ml) }, { result => promise.complete(result.map(_ => ())) })
    promise.future
  }

  /** Like [[executeMLCode]] but waits for the code to be executed before returning. */
  def executeMLCodeNow(ml : String): Unit = Await.result(executeMLCode(ml), Duration.Inf)

  /** Executes the ML expression `ml` in the Isabelle process.
   *
   * WARNING: This has a global effect on the Isabelle process because it modifies the ML name space.
   *
   * The expression must be of ML type `exn`.
   * The result of evaluating the expression is added to the object store.
   * The ML code is executed in a context where the structure `Control_Isabelle` is opened
   * (i.e., you can write `E_Int` instead of `Control_Isabelle.E_Int`).
   *
   * Example: `storeValue("exception E_Term of term")` (this is actually done by [[de.unruh.isabelle.pure.Term]]).
   *
   * In code that is supposed to support multiple instances of Isabelle, it can be cumbersome to
   * keep track in which instances a given ML code fragment was already executed. See [[OperationCollection]]
   * for a helper class to facilitate that.
   *
   * @return Future that contains an ID referencing the result in the object store.
   *         (Or throws an [[IsabelleControllerException]] if the ML code compilation/execution fails.)
   */
  def storeValue(ml : String): Future[ID] = {
    val promise : Promise[ID] = Promise()
//    logger.debug(s"Compiling ML value: $ml")
    send({ stream => stream.writeByte(4); writeString(stream, ml) },
      { result => promise.complete(result.map {
        case DInt(id) => new ID(id, this)
        case data => throw IsabelleException(s"Internal error: expected DInt, not $data")}) })
    promise.future
  }

  /** Applies `f` to `x` and returns the result.
   *
   * `f` must be the ID of an object in the object store of the form `E_Function f'` (and thus `f'` of ML type `data -> data`).
   *
   * `x` is serialized and transferred to the Isabelle process, the value `f' x` is computed, serialized and transferred back.
   *
   * By definition of the type [[Isabelle.Data]], `x` can be a tree containing integers, strings, and object IDs.
   * When transferring an object ID to the Isabelle process, it is replaced by the object (exception) that is referred by the ID.
   * And similarly, objects (exceptions) in the return value are added to the object store and replaced by IDs upon transfer to the Scala side.
   *
   * This behavior gives rise to two simple use patterns:
   *
   * Retrieving values: Say `tree` is some algebraic data type on the ML side, `Tree` is a corresponding Scala class,
   * `encode : tree -> data` is a function that
   * encodes a tree as `data` (using the DList, DInt, and DString constructors only), and `E_Tree of tree`
   * is an exception type to store trees in the object store. Then we can define a function for retrieving a tree from
   * the object store to Scala as follows:
   * {{{
   * val encodeID : Future[ID] = isabelle.storeValue("fn DObject (E_Tree tree) => encode tree")
   * def decode(data: Data) : Tree = ??? // The opposite of the ML function encode
   * def retrieve(id: ID) : Tree = {
   *   // Apply encode to the element referenced by id, result is an encoding of the tree as Data
   *   val dataFuture : Future[Data] = isabelle.applyFunction(encodeID, DObject(id))
   *   // For simplicitly, we force synchronous execution
   *   val data : Data = Await.result(dataFuture, Duration.Inf)
   *   decode(data)
   * }
   * }}}
   *
   * Storing values: Continuing the above example, say `decode : data -> tree` is an ML function that decodes trees
   * (inverse of `encode` above). Then we can store trees in the object store from Scala using the following function
   * `store`:
   * {{{
   * val decodeID : Future[ID] = isabelle.storeValue("fn data => DObject (E_Tree (decode data))")
   * def encode(tree: Tree) : Data = ??? // The opposite of the ML function decode
   * def store(tree: Tree) : ID = {
   *   // Apply ML-decode to the Scala-encoded tree, store it in the object store, and return the ID (inside a Data)
   *   val dataFuture : Future[Data] = isabelle.applyFunction(decodeID, encode(tree))
   *   // For simplicitly, we force synchronous execution
   *   val data : Data = Await.result(dataFuture, Duration.Inf)
   *   // get the ID inside the returned data (referring to the tree object in the object store)
   *   val DObject(id) = data
   *   id
   * }
   * }}}
   *
   * Of course, arbitrary combinations of these two ideas are possible. For example, one could have a Scala data structure
   * that contains IDs of objects still on the ML side. These data structures can be serialized and deserialized
   * similar to the above example, using the fact that the type [[Isabelle.Data]] allows IDs to occur anywhere in
   * the tree.
   *
   * Objects added to the object store by this mechanism are garbage collected on the ML side when the corresponding
   * IDs are not used any more on the Scala side.
   *
   * This approach is very low level. In particular, there is no type system support to ensure that the IDs contained
   * in the serialized data actually refer to objects of the right type. A higher level typesafe approach for accessing data
   * in the object store is given by [[de.unruh.isabelle.mlvalue.MLValue]] (see there). However, `MLValue`s internally use the mechanism
   * described here to transfer data to/from the Isabelle process. Thus, to add support for `MLValue`s of new types,
   * the `applyFunction` needs to be used.
   *
   * @return A future holding the ID of the result (or holding an exception
   *         if the `f` is not `E_Function f'` or `f' x` throws an exception in ML)
   * @see [[Isabelle.Data]] for information what kind of data can be contained in `x` and the result
   */
  def applyFunction(f: ID, x: Data): Future[Data] = {
    val promise: Promise[Data] = Promise()
    send({ stream => stream.writeByte(7); stream.writeLong(f.id); writeData(stream,x) },
      { result => promise.complete(result) })
    promise.future
  }

  /** Like [[applyFunction(f:de* applyFunction(ID,Data)]], except `f` is a future. */
  def applyFunction(f: Future[ID], x: Data)(implicit ec: ExecutionContext) : Future[Data] =
    for (f2 <- f; fx <- applyFunction(f2, x)) yield fx

  private val lastMessages = new mutable.Queue[String]()
  private def logStream(stream: InputStream, level: LogLevel) : Unit = {
    val log = logger(level)
    val thread = new Thread(s"Isabelle output logger, $level") {
      override def run(): Unit = {
        try
          new BufferedReader(new InputStreamReader(stream)).lines().forEach { line =>
            if (lastMessages.size >= 10) lastMessages.dequeue()
            lastMessages.enqueue(line)
            log(line)
          }
        catch {
          case _ : UncheckedIOException =>
            // Can happen if the stream is closed. Ignore
        }
      }
    }
    thread.setDaemon(true)
    thread.start()
  }
}

object Isabelle {
  def defaultCommandHandler(data: Data): Unit =
    throw new RuntimeException(s"Command $data received from Isabelle, but default command handler is installed")

  private val logger = log4s.getLogger

  /** An ID referencing an object in the object store (see the description of [[Isabelle]]).
   * If this ID is not referenced any more, the referenced object will be garbage collected
   * in the Isabelle process, too.
   */
  final class ID private[control] (private[control] val id: Long, isabelle: Isabelle) {
    SharedCleaner.register(this, new IDCleaner(id, isabelle))

    override def equals(obj: Any): Boolean = obj match {
      case obj: ID => id == obj.id
      case _ => false
    }

    override def hashCode(): Int = id.hashCode()
  }
  private final class IDCleaner(id: Long, isabelle: Isabelle) extends Runnable {
    def run(): Unit = isabelle.garbageQueue.add(id)
  }

  /** Parent trait for different kinds of configuration for [[Isabelle]]. See in particular [[Setup]]. */
  sealed trait SetupGeneral {
    /** Installs a handler for commands sent from the Isabelle process to the Scala process.
     * When invoking `Control_Isabelle.sendToScala data` (for `data` of ML type `data`),
     * then `isabelleCommandHandler` is invoked as `isabelleCommandHandler(data)`. (After transferring
     * and converting `data` to type [[Data]].)
     **/
    val isabelleCommandHandler : Data => Unit
  }

  /** Configuration for initializing an [[Isabelle]] instance.
   *
   * (The fields of this class are documents in the source code. I am not sure why they do not occur in the
   * generated API doc.)
   *
   * @param workingDirectory Working directory in which the Isabelle process should run. (Default:
   *                         working directory of the Scala process.) All other paths described
   *                         below are interpreted relative to `workingDirectory` (unless they are absolute).
   * @param isabelleHome Path to the Isabelle distribution
   * @param logic Heap image to load in Isabelle (e.g., `HOL`, `HOL-Analysis`, etc.)
   * @param sessionRoots Additional session directories in which Isabelle will search for sessions
   *                     (must contain `ROOT` files and optionally `ROOTS` files, see the Isabelle system manual).
   *                     Default: no additional session directories
   * @param userDir User configuration directory for Isabelle. Must end in `/.isabelle` if provided.
   *                None (default) means to let Isabelle chose the default location.
   *                Here Isabelle stores user configuration and heap images (unless
   *                the location of the heap images is configured differently, see the Isabelle system manual)
   * @param build Whether to build the Isabelle heap before running Isabelle. If false, the heap will never be
   *              built. (This means changes in the Isabelle theories will not be reflected. And if the heap was never
   *              built, the Isabelle process fails.) If true, the Isabelle build command will be invoked. That
   *              command automatically checks for changed dependencies but may add a noticable delay even if
   *              the heap was already built.
   * @param isabelleCommandHandler see [[SetupGeneral.isabelleCommandHandler]]
   */
  case class Setup(isabelleHome : Path,
                   logic : String = "HOL",
                   userDir : Option[Path] = None,
                   workingDirectory : Path = Paths.get(""),
                   sessionRoots : Seq[Path] = Nil,
                   build : Boolean = true,
                   verbose : Boolean = false, // TODO: make separate build options subclass // DOCUMENT
                   isabelleCommandHandler: Data => Unit = Isabelle.defaultCommandHandler) extends SetupGeneral {
    /** [[isabelleHome]] as an absolute path */
    def isabelleHomeAbsolute: Path = workingDirectory.resolve(isabelleHome)
    /** [[userDir]] as an absolute path. If [[userDir]] is [[scala.None None]], the Isabelle default user directory is returned. */
    def userDirAbsolute: Path = userDir.map(workingDirectory.resolve) match {
      case Some(dir) => workingDirectory.resolve(dir)
      case None => SystemUtils.getUserHome.toPath.resolve(".isabelle")
    }
  }

  /**
   * Configuration for connecting to an already running Isabelle process.
   * The Isabelle process must load `control_isabelle.ml` (available as a resource in this package)
   * and invoke `Control_Isabelle.handleLines ()`.
   *
   * Before loading `control_isabelle.ml`,
   * the values `COMMUNICATION_STREAMS` and `SECRETS` need to be initialized in ML.
   * `SECRETS` needs to be initialized with the secrets use in the communication with the Isabelle
   * process. These values are currently chosen by the [[Isabelle]] class itself,
   * '''making it currently impossible to use [[SetupRunning]]'''.
   *
   * @param inputPipe the path of a named pipe for the protocol messages sent to the Isabelle process
   *                  (corresponding to first component of COMMUNICATION_STREAMS)
   * @param outputPipe the path of a named pipe for the protocol messages sent by the Isabelle process
   *                  (corresponding to second component of COMMUNICATION_STREAMS)
   * @param isabelleCommandHandler see [[SetupGeneral.isabelleCommandHandler]]
   */
  @Experimental
  case class SetupRunning(inputPipe : Path, outputPipe : Path,
                          isabelleCommandHandler: Data => Unit = Isabelle.defaultCommandHandler) extends SetupGeneral

  //noinspection UnstableApiUsage
  private val buildLocks = Striped.lazyWeakReadWriteLock(10)

  /** Runs the Isabelle build process to build the session heap image `setup.logic`
   *
   * This is done automatically by the constructors of [[Isabelle]] unless `build=false`.
   *
   * @param setup Configuration of Isabelle.
   */
  def buildSession(setup: Setup) : Unit = {
    implicit val s: Setup = setup
    def wd = setup.workingDirectory

    val isabelleArguments = ListBuffer[String]()

    isabelleArguments += "build"
    isabelleArguments += "-b" // Build heap image

    if (setup.verbose)
      isabelleArguments += "-v" // Verbose build

    for (root <- setup.sessionRoots)
      isabelleArguments += "-d" += cygwinAbs(root)

    isabelleArguments += setup.logic

    val cmd = makeIsabelleCommandLine(absPath(setup.isabelleHome), isabelleArguments.toSeq)

    logger.debug(s"Cmd line: ${cmd.mkString(" ")}")

    val processBuilder = scala.sys.process.Process(cmd, wd.toAbsolutePath.toFile, makeIsabelleEnvironment :_*)
    val errors = ListBuffer[String]()

    val lock = buildLocks.get(absPath(setup.isabelleHome).normalize).writeLock
    lock.lockInterruptibly()
    try {
      if (0 != processBuilder.!(ProcessLogger(line => logger.debug(s"Isabelle build: $line"),
        { line => errors.append(line); logger.warn(s"Isabelle build: $line") })))
        throw IsabelleBuildException(s"Isabelle build for session ${setup.logic} failed", errors.toList)
    } finally
      lock.unlock()
  }

  /**
   * Starts Isabelle/jEdit (interactive editing of theories) with the given Isabelle configuration.
   *
   * @param setup Isabelle configuration
   * @param files Files to open in jEdit
   * @throws IsabelleJEditException if jEdit fails (returns return code ≠0)
   */
  def jedit(setup: Setup, files: Seq[Path]) : Unit = {
    implicit val s = setup
    def wd = setup.workingDirectory

//    val isabelleBinary = setup.isabelleHome.resolve("bin").resolve("isabelle")
    val isabelleArguments = ListBuffer[String]()

    isabelleArguments += "jedit"

    for (root <- setup.sessionRoots)
      isabelleArguments += "-d" += cygwinAbs(root)

    isabelleArguments += "-l" += setup.logic

    isabelleArguments += "--"
    isabelleArguments ++= files.map(cygwinAbs)

    val cmd = makeIsabelleCommandLine(absPath(setup.isabelleHome), isabelleArguments.toSeq)

    logger.debug(s"Cmd line: ${cmd.mkString(" ")}")

    val processBuilder = scala.sys.process.Process(cmd.toSeq, wd.toAbsolutePath.toFile, makeIsabelleEnvironment :_*)

    val lock = buildLocks.get(absPath(setup.isabelleHome).normalize).readLock
    lock.lockInterruptibly()
    try {
      if (0 != processBuilder.!(ProcessLogger(line => logger.debug(s"Isabelle jedit: $line"),
        { line => logger.warn(s"Isabelle jedit: $line") })))
        throw IsabelleJEditException("Could not start Isabelle/jEdit")
    } finally
      lock.unlock()
  }

  private def makeIsabelleEnvironment(implicit setup: Setup): List[(String, String)] = {
    val env = ListBuffer[(String, String)]()
    setup.userDir match {
      case Some(path) => env += "USER_HOME" -> cygwinAbs(path.getParent)
      case None =>
    }
    /* Things copied from Isabelle's Cygwin-Terminal.bat, they seem necessary for correct startup:
       set TEMP_WINDOWS=%TEMP%
       set HOME=%HOMEDRIVE%%HOMEPATH%
       set PATH=%CD%\bin;%PATH%
       set LANG=en_US.UTF-8
       set CHERE_INVOKING=true
     */
    if (SystemUtils.IS_OS_WINDOWS) {
      // Needed on Windows so that cygwin-bash does not cd to home
      env += "CHERE_INVOKING" -> "true"
      env += "HOME" -> SystemUtils.getUserHome.getAbsolutePath
      System.getenv("TEMP") match {
        case null =>
          val tempDir = Files.createTempDirectory("isabellecontrol").toAbsolutePath
          tempDir.toFile.deleteOnExit()
          env += "TEMP_WINDOWS" -> tempDir.toString
        case temp =>
          env += "TEMP_WINDOWS" -> temp
      }
      env += "LANG" -> "en_US.UTF-8"
    }
    env.toList
  }

  private def cygwinIfWin(path: Path) =
    if (SystemUtils.IS_OS_WINDOWS) Utils.cygwinPath(path) else path.toString
  /** Path to absolute string, interpreted relative to wd */
  private def absPath(path: Path)(implicit setup: Setup) = setup.workingDirectory.resolve(path).toAbsolutePath
  /** Path to absolute string, interpreted relative to wd */
  private def cygwinAbs(path: Path)(implicit setup: Setup) = cygwinIfWin(absPath(path))

  private def makeIsabelleCommandLine(isabelleHome: Path, arguments: Seq[String]) : Seq[String]= {
    if (SystemUtils.IS_OS_WINDOWS) {
      val bash = isabelleHome.resolve("contrib").resolve("cygwin").resolve("bin").resolve("bash").toString
      val isabelle = Utils.cygwinPath(isabelleHome.resolve("bin").resolve("isabelle"))
      List(bash, "--login", "-c",
        (List(isabelle) ++ arguments).map(StringEscapeUtils.escapeXSI).mkString(" "))
    } else
      List(isabelleHome.resolve("bin").resolve("isabelle").toString) ++ arguments
  }

  /** An algebraic datatype that allows to encode trees of data containing integers ([[DInt]]), strings ([[DString]]), and IDs of
   * objects ([[DObject]]) in the object store of the Isabelle process. A constructor [[DList]] is used to create a tree
   * structure.
   *
   * No particular semantics is given to these trees, their purpose is to be a sufficiently flexible datatype to be able
   * to encode arbitrary data types for transfer.
   *
   * A corresponding datatype is defined in the `Control_Isabelle` ML structure in the Isabelle process:
   * {{{
   * datatype data = DString of string | DInt of int | DList of data list | DObject of exn
   * }}}
   * Note that while [[DObject]] on the Scala side contains an ID of an object, on the ML side we instead
   * directly have the object that is references (of type `exn`). Serialization and deserialization creates and
   * dereferences object IDs as needed.
   *
   * The data that can be stored in these trees is subject to the following additional limitations:
   *  - Strings must be ASCII (non-ASCII characters will be replaced by default characters).
   *  - Integers must be 64bit signed integers (this is enforced in Scala due to the size of the type
   *    [[scala.Long Long]] but ML integers have no size limit (like [[scala.BigInt BigInt]])). Larger integers will
   *    be truncated to 64 bits.
   *  - Strings must be at most 67.108.856 characters long (`String.maxSize` in ML). Otherwise there an exception is
   *    raised in the Isabelle process
   *
   * @see [[Isabelle.applyFunction(f:de* applyFunction]] for details how to use this type to transfer data
   * */
  sealed trait Data
  final case class DInt(int: Long) extends Data
  final case class DString(string: String) extends Data
  final case class DList(list: Data*) extends Data
  final case class DObject(id: ID) extends Data

  private final class ProcessCleaner(destroyActions: ConcurrentLinkedQueue[Runnable]) extends Runnable {
    override def run(): Unit = {
      while (!destroyActions.isEmpty) {
        val action = destroyActions.poll()
        if (action != null)
          try action.run()
          catch {
            case e: Throwable => e.printStackTrace()
          }
      }
    }
  }
}

/** Ancestor of all exceptions specific to [[Isabelle]] */
abstract class IsabelleControllerException(message: String) extends IOException(message)

/** Thrown if an operation cannot be executed because [[Isabelle.destroy]] has already been invoked. */
case class IsabelleDestroyedException(message: String) extends IsabelleControllerException(message)
object IsabelleDestroyedException {
  def apply(cause: Throwable): IsabelleDestroyedException = {
    val exn = IsabelleDestroyedException("Isabelle process was destroyed: " + cause.getMessage)
    exn.initCause(cause)
    exn
  }
}
/** Thrown if running Isabelle/jEdit fails */
case class IsabelleJEditException(message: String) extends IsabelleControllerException(message)
/** Thrown if the build process of Isabelle fails */
case class IsabelleBuildException(message: String, errors: List[String])
  extends IsabelleControllerException(if (errors.nonEmpty) message + ": " + errors.last else message)
/** Thrown in case of an error in the ML process (ML compilation errors, exceptions thrown by ML code) */
case class IsabelleException(message: String) extends IsabelleControllerException(message)
/** Thrown in case of protocol errors in Isabelle process */
case class IsabelleProtocolException(message: String) extends IsabelleControllerException(message)
