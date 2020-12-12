package com.artifex.solib;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.LinkedBlockingDeque;

/*

    This class holds a Thread and a queue of Tasks.
    The queue is a LinkedBlockingDeque so that entries
    Can be added at the beginning or the end.

    The Thread runs a loop, pulling Tasks from it and
    executing them.

 */

public class Worker
{
    private boolean alive;
    private Looper mLooper = null;
    private Thread mThread = null;
    protected LinkedBlockingDeque<Task> mQueue;

    public static class Task implements Runnable
    {
        //  this method runs on this worker's Thread
        public void work() {}

        //  this method runs on the UI thread when work() is done.
        public void run() {}
    }

    public Worker(Looper looper)
    {
        mQueue = new LinkedBlockingDeque<>();
        mLooper = looper;
    }

    public void start()
    {
        Runnable threadWorker = new Runnable()
        {
            @Override
            public void run()
            {
                while (alive) {
                    try {
                        Task task = mQueue.take();
                        task.work();
                        new Handler(mLooper).post(task);
                    } catch (InterruptedException x) {
                        // this one is OK
                    } catch (Throwable x) {
                        Log.e("Worker", "exception in Worker thread: " + x.toString());
                    }
                }
            }
        };
        mThread = new Thread(threadWorker);
        mThread.start();

        alive = true;
    }

    public boolean isRunning() {return alive;}

    public void stop()
    {
        alive = false;
        mThread.interrupt();
        mQueue.clear();
    }

    public void add(final Task task)
    {
        //  don't add a task is we've been stopped
        if (!alive)
            return;

        try {
            mQueue.put(task);
        } catch (Throwable x) {
            Log.e("Worker", "exception in Worker.add: " + x.toString());
        }
    }

    public void addFirst(final Task task)
    {
        //  don't add a task is we've been stopped
        if (!alive)
            return;

        try {
            mQueue.addFirst(task);
        } catch (Throwable x) {
            Log.e("Worker", "exception in Worker.addFirst: " + x.toString());
        }
    }

    //  this function tells us whether the calling Thread is this Worker's Thread.
    public boolean isWorkerThread()
    {
        return Thread.currentThread().equals(mThread);
    }
}
