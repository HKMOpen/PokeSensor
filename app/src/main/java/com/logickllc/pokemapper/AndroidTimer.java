package com.logickllc.pokemapper;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

/* This class is a Timer that can be safely used in an Android activity. The timer will only
 * count down while the activity is in the foreground. Use a separate AndroidTimerManager class per
 * activity to handle all the AndroidTimers for that activity.
 */

public class AndroidTimer {
	private Timer myTimer; // Reference to the actual timer this object uses
	private long startTime, delay, timeLeft;
	private boolean repeatTask; // Whether or not the timer should repeat at the delay specified
	private TimerTask task; // The TimerTask that should be run when the timer goes off
	private boolean isActive; // This is set to false after single execution timers run their task
	private TimerTaskClone tempTask;
	
	public AndroidTimer(TimerTask task, long delay, boolean repeatTask) {
		myTimer = new Timer();
		this.repeatTask = repeatTask;
		this.task = task;
		this.delay = delay;		
		isActive = true;
		tempTask = new TimerTaskClone();
		
		if (repeatTask) {
			myTimer.schedule(tempTask, this.delay, this.delay);
		} else {
			myTimer.schedule(tempTask, this.delay);
		}
		startTime = System.currentTimeMillis();
		timeLeft = delay;
	}
	
	public boolean pause() {
		// Pause this individual timer. Record the time remaining on the timer and cancel it.
		// Return true if the task is cancelled or false if the task is already running.
		boolean success = true;
		if (myTimer != null && isActive) {
			success = tempTask.cancel();
			myTimer.purge();
			myTimer = null;
			long currentTime = System.currentTimeMillis();
			// The modulus accounts for the possibiity that the timer is a repeat task and has been running longer than the delay time
			timeLeft = timeLeft - ((currentTime - startTime) % delay);
			Log.d("Time remaining on Pause", Long.toString(timeLeft) + " ms");
		}
		return success;
	}
	
	public void resume() {
		// Resume this individual timer. Set the startTime again and reschedule the task for the time remaining.
		if (myTimer == null && isActive) {
			myTimer = new Timer();
			tempTask = new TimerTaskClone();
			if (timeLeft < 0) timeLeft = delay;
			if (repeatTask) {
				myTimer.schedule(tempTask, timeLeft, delay);
			} else {
				myTimer.schedule(tempTask, timeLeft);
			}
			startTime = System.currentTimeMillis();
			Log.d("Time remaining on Resume", Long.toString(timeLeft) + " ms");
		}
	}
	
	public boolean cancel() {
		// Kill this timer
		boolean success = true;
		if (myTimer != null) {
			isActive = false;
			success = tempTask.cancel();
			myTimer.purge();
			myTimer = null;
		}
		return success;
	}
	
	public boolean isActive() { return isActive; }
	
	private class TimerTaskClone extends TimerTask {

		@Override
		public void run() {
			task.run();
			isActive = false;
		}
		
	}
}
