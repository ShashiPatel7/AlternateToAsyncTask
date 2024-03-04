package com.impirion.utils

import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author shashi.patel
 * @createdOn 29th March 2024
 * @purpose to work with backgroung tasks (in replacement of deprecated AsyncTask)
 */
abstract class SHCThreading<Params, Progress, Result> {
    private var executorService = Executors.newSingleThreadExecutor()
    private var prePostHandler = Handler(Looper.getMainLooper())
    private val paramHelper: ParamsHelper<Params>? = null
    private val isSHCThreadingCancelled = AtomicBoolean()

    /**
     * to initiate execution process
     */
    open fun startTask() {
        try {
            executorService?.execute {
                prePostHandler.post(Runnable {
                    //pre execution
                    Log.e("SHASHI", "pre execution")
                    onPreExecute()
                })

                //bg task
                var result: Result? = null
                try {
                    Log.e("SHASHI", "BG execution")
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    result = doInBackground(paramHelper?.mParams)
                    Binder.flushPendingCommands()
                } catch (tr: Throwable) {
                    isSHCThreadingCancelled.set(true)
                    throw tr
                }

                prePostHandler.post(Runnable {
                    //post execution
                    Log.e("SHASHI", "post execution")
                    onPostExecute(result)
                })
            }
        } catch (ex: Exception) {
            Log.e("", "")
        }
    }

    /**
     * to initiate execution process
     */
    open fun execute(params: Array<Params>? = null) {
        if (params != null) {
            paramHelper?.mParams = params
        }
        startTask()
    }

    /**
     * method implements to do pre execution tasks. That is initiated before backgroung task initiated
     * For eg. to show Progress Dialog
     */
    abstract fun onPreExecute()

    /**
     * method implements to do pre execution tasks. That is initiated before backgroung task initiated
     * For eg. to show Progress Dialog
     */
    abstract fun doInBackground(params: Array<Params>?): Result

    /**
     * method implements to do post execution tasks. That is initiated after background task completed
     * For eg. to close / dismiss Progress Dialog
     */
    abstract fun onPostExecute(result: Result)

    /**
     * method implements to check current execution and update the progress.
     */
    protected fun publishProgress(vararg values: Progress) {
        if (!isSHCThreadingCancelled()) {
            onProgressUpdate(values)
        }
    }

    /**
     * method implements to update any progress.
     * For eg. to update ProgressDialog percentage etc.
     */
    protected open fun onProgressUpdate(values: Array<out Progress>) {}

    private abstract class ParamsHelper<Params> {
        var mParams: Array<Params>? = null
    }

    /**
     * purpose of this method is to cancel any ongoing task
     */
    fun cancelSHCThreading() {
        isSHCThreadingCancelled.set(true)
        executorService?.shutdown() ?: return
    }

    /**
     * purpose of this method is to check whether any ongoing task is canceled or not
     */
    fun isSHCThreadingCancelled(): Boolean {
        return isSHCThreadingCancelled.get() || executorService?.isShutdown!! || executorService?.isTerminated!!
    }
}