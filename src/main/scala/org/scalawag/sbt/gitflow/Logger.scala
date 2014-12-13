package org.scalawag.sbt.gitflow

trait Logger {
  def debug(s:String)
  def info(s:String)
  def warn(s:String)
  def error(s:String)
}

object StdoutLogger extends Logger {
  override def debug(s:String) = println(s"DEBUG: $s")
  override def info(s:String) = println(s"INFO: $s")
  override def warn(s:String) = println(s"WARN: $s")
  override def error(s:String) = println(s"ERROR: $s")
}

object NoopLogger extends Logger {
  override def debug(s:String) = {}
  override def info(s:String) = {}
  override def warn(s:String) = {}
  override def error(s:String) = {}
}
