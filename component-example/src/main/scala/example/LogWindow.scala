package example

import javax.swing.WindowConstants.HIDE_ON_CLOSE
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultCaret.ALWAYS_UPDATE
import javax.swing.{JFrame, JScrollPane, JTextArea}

/** A window that shows log messages. This is a very primitive implementation just for demo purposes.
 * Use [[LogWindow.log]] to show a log message. */
class LogWindow private() extends JFrame("Demo logging window") {
  /** textArea containing log messages */
  private val textArea = new JTextArea()
  // Do not allow to edit the log messages
  textArea.setEditable(false)
  // Makes the textArea scroll when text is appended
  textArea.getCaret.asInstanceOf[DefaultCaret].setUpdatePolicy(ALWAYS_UPDATE)

  /** Scrolling pane that contains the [[textArea]], otherwise we do not get scrollbars. */
  private val scrollPane = new JScrollPane(textArea)
  // Add the scrollPane to this window
  add(scrollPane)
  // Without this, the window would have zero size
  setSize(500, 500)
  // Hide the window when the user clicks the close button
  setDefaultCloseOperation(HIDE_ON_CLOSE)
  // Show the window
  setVisible(true)

  /** Add `string` to the window content */
  def log(string: String): Unit = textArea.append(string + "\n")
}

object LogWindow {
  /** The single instance of the [[LogWindow]], created on demand */
  private lazy val singleton = new LogWindow()

  /** Returns the singleton [[LogWindow]], makes it visible if needed (e.g., after user closed it) */
  def getLogWindow: LogWindow = {
    if (!singleton.isVisible)
      singleton.setVisible(true)
    singleton
  }

  /** Makes the [[LogWindow]] visible if needed (see [[getLogWindow]]) and appends `string` to the window content */
  def log(str: String): Unit = getLogWindow.log(str)
}
