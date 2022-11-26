package HelperUtils


import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

object CreateLogger{
  def apply[T](class4Logger: Class[T]): Logger = {
    val logback = "logback.xml"
    val logger = LoggerFactory.getLogger(class4Logger)
    Try(getClass.getClassLoader.getResourceAsStream(logback)) match {
      case Failure(exception) => logger.error(s"Failed to locate $logback for reason $exception")
      case Success(inStream) => inStream.close()
    }
    logger
  }
}
