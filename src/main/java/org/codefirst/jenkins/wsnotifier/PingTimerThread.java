package org.codefirst.jenkins.wsnotifier;

public class PingTimerThread extends Thread {
	private boolean run = true;
	private int interval;
	
	public PingTimerThread(int interval) {
		this.interval = interval;
		// autostart
		start();
	}
	
	public void run() {
		while (run) {
    		try {
				Thread.sleep(interval * 1000);
	    		WsServer.ping();
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void terminate() {
		run = false;
		interrupt();
	}
}
