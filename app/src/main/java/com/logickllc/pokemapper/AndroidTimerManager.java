package com.logickllc.pokemapper;

import java.util.ArrayList;
import java.util.TimerTask;

public class AndroidTimerManager {
	private ArrayList<AndroidTimer> timerList = new ArrayList<AndroidTimer>(); // Shared list of all AndroidTimers
	
	public void addTimer(TimerTask task, long delay, boolean repeatTask) {
		// Initialize a new AndroidTimer and add it to the list of AndroidTimers
		timerList.add(new AndroidTimer(task, delay, repeatTask));
	}
	
	public void addTimer(AndroidTimer timer) {
		timerList.add(timer);
	}
	
	public boolean pauseTimers() {
		// Pause every AndroidTimer that exists. This should be called in onPause() of the Activity that uses these.
		boolean success = true;
		if (timerList.size() > 0) {
			for (AndroidTimer timer : timerList) {
				if (!timer.pause()) success = false;
			}
		}
		return success;
	}
	
	public void resumeTimers() {
		// Resume every AndroidTimer that exists. This should be called in onResume() of the Activity that uses these.
		if (timerList.size() > 0) {
			for (AndroidTimer timer : timerList) {
				timer.resume();
			}
		}
	}
	
	public void cancelTimer(AndroidTimer timer) {
		// Remove this instance from the list of timers and kill this timer
		timer.cancel();
		timerList.remove(timer);
		timer = null;
	}
	
	public void cancelAllTimers() {
		int size = timerList.size();
		if (size > 0) {
			for (AndroidTimer timer : timerList) {
				timer.cancel();
				timer = null;
			}
			timerList.clear();
		}
	}
	
	public int getListSize() {
		return timerList.size();
	}
	
	public void clearInactiveTimers() {
		ArrayList<AndroidTimer> tempList = new ArrayList<AndroidTimer>();
		int size = timerList.size();
		if (size > 0) {
			for (int n = 0; n < size; n++) {
				AndroidTimer timer = timerList.get(n);
				if (!timer.isActive()) tempList.add(timer);
			}
			for (AndroidTimer timer : tempList) {
				cancelTimer(timer);
			}
		}
	}
}
