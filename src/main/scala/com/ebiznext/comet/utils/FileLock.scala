package com.ebiznext.comet.utils

import com.ebiznext.comet.schema.handlers.StorageHandler
import com.typesafe.scalalogging.StrictLogging
import org.apache.hadoop.fs.Path

import scala.util.{Failure, Success}

/**
  * HDFS does not have a file locking mechanism.
  * We implement it here in the following way
  * - A file is locked when it's modification time is less than 5000ms tahn the current time
  * - If the modification time is older tahn the current time of more than 5000ms, it is considered unlocked.
  * - The owner of a lock spawn a thread that update the modification every 5s to keep the lock
  * - The process willing to get the lock check every 5s second for the modification time of the file to gain the lock.
  * - When a process gain the lock, it deletes the file first and tries to create a new one, this make sure that of two process gaining the lock, only one will be able to recreate it after deletion.
  *
  * To gain the lock, call tryLock, to release it, call release.
  *
  * @param path Lock File path
  * @param storageHandler Filesystem Handler
  */
class FileLock(path: Path, storageHandler: StorageHandler) extends StrictLogging {
  val checkinPeriod = 5000L
  val fileWatcher = new LockWatcher(path, storageHandler, checkinPeriod)

  /**
    * Try to gain the lock during timeoutInMillis millis
    *
    * @param timeoutInMillis number of milliseconds during which the calling process will try to get the lock before it time out. -1 means no timeout
    * @return true when locked is acquired, false otherwise
    */
  def tryLock(timeoutInMillis: Long = -1): Boolean = {
    storageHandler.mkdirs(path.getParent)
    val maxTries = if (timeoutInMillis == -1) Integer.MAX_VALUE else timeoutInMillis / checkinPeriod
    var numberOfTries = 1
    logger.info(s"Trying to acquire lock for file ${path.toString} during $timeoutInMillis ms")
    var ok = false
    while (numberOfTries <= maxTries && !ok) {
      ok = storageHandler.touchz(path) match {
        case Success(_) =>
          logger.info(
            s"Succeeded to acquire lock for file ${path.toString} after $numberOfTries tries"
          )
          watch()
          true
        case Failure(_) =>
          val lastModified = storageHandler.lastModified(path)
          logger.info(s"""
              |lastModified=$lastModified
              |System.currentTimeMillis()=${System.currentTimeMillis()}
              |checkinPeriod*4=${checkinPeriod * 4}

          """)
          if (System.currentTimeMillis() - lastModified > checkinPeriod * 4) {
            storageHandler.delete(path)
          }
          numberOfTries = numberOfTries + 1
          Thread.sleep(checkinPeriod)
          false
      }
    }
    ok
  }

  /**
    * Release the lock and delete the lock file.
    */
  def release(): Unit = fileWatcher.release()

  private def watch(): Unit = {
    val th = new Thread(fileWatcher, s"LockWatcher-${System.currentTimeMillis()}")
    th.start()
  }
}

class LockWatcher(path: Path, storageHandler: StorageHandler, checkinPeriod: Long)
    extends Runnable
    with StrictLogging {

  def release(): Unit =
    stop = true

  var stop = false
  override def run(): Unit = {
    try {
      while (!stop) {
        Thread.sleep(checkinPeriod)
        storageHandler.touch(path)
        logger.info(s"watcher $path modified=${storageHandler.lastModified(path)}")
      }
      storageHandler.delete(path)
    } catch {
      case e: InterruptedException =>
        e.printStackTrace();
    }
  }

}