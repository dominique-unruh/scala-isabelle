package de.unruh.isabelle.misc

import java.lang.ref.Cleaner

object SharedCleaner {
  private val cleaner = Cleaner.create()
  def register(obj: AnyRef, action: Runnable): Unit = cleaner.register(obj, action)
}
