package by.anegin.myapp.common.util

import android.os.SystemClock
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SettableFuture<T> : Future<T> {

    private var completed: Boolean = false
    private var canceled: Boolean = false

    @Volatile
    private var result: T? = null

    @Volatile
    private var exception: Throwable? = null

    @Synchronized
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (!completed && !canceled) {
            canceled = true
            return true
        }
        return false
    }

    @Synchronized
    override fun isCancelled(): Boolean {
        return canceled
    }

    @Synchronized
    override fun isDone(): Boolean {
        return completed
    }

    fun set(result: T): Boolean {
        synchronized(this) {
            if (completed || canceled) return false

            this.result = result
            this.completed = true

            notifyAll()
        }
        return true
    }

    fun setException(throwable: Throwable): Boolean {
        synchronized(this) {
            if (completed || canceled) return false

            this.exception = throwable
            this.completed = true

            notifyAll()
        }
        return true
    }

    @Synchronized
    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T? {
        while (!completed) wait()

        return if (exception != null)
            throw ExecutionException(exception)
        else
            result
    }

    @Synchronized
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T? {
        val startTime = SystemClock.elapsedRealtime()

        while (!completed && SystemClock.elapsedRealtime() - startTime < unit.toMillis(timeout)) {
            waitMillis(unit.toMillis(timeout))
        }

        return if (!completed)
            throw TimeoutException()
        else
            get()
    }

}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.wait() = (this as Object).wait()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.notify() = (this as Object).notify()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.notifyAll() = (this as Object).notifyAll()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun Any.waitMillis(timeout: Long) = (this as Object).wait(timeout)