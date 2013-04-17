package com.blueocean.jnsinput;
    
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;  
import android.view.MotionEvent.PointerProperties;  


class JNSInput {
	private static final String TAG = "JNSInput";
	private ServerSocket mServerSocket = null;
	private Socket socket = null;
	private BufferedReader is = null;
	private PrintWriter pw;
	private static final boolean debug = true;
	
	public static void main(String[] args) {
		System.err.println("this is JNSInput");
		JNSInput mInput = new JNSInput();
		mInput.createServer();
	}
	
	public void createServer() {
		try {
			mServerSocket = new ServerSocket(44444);
			while (true) {
				socket = mServerSocket.accept();
				new Thread(monitorRunnable).start();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private Runnable monitorRunnable = new Runnable() {
		public void run() {
			System.err.println("monitorRunnable");
			try {
				is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				pw = new PrintWriter(socket.getOutputStream());
				System.err.println("connect address = " + socket.getInetAddress());
				System.err.println("socket.isclosed = " + socket.isConnected());
				while (true) {
					try {
						System.err.println("is.readline");
						String line = is.readLine();
						System.err.println("line = " + line);
						if (line != null) {
							processData(line);
						}
						pw.println("got server version 1.4");
						pw.flush();
						//Thread.sleep(500);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};
	
	private void processData(String data) {
		String[] listData = data.split(":");
		if (debug) System.err.println("listData = " + listData);
		if (listData[0].equals("injectKey")) {
			injectKeyProcess(listData);
		} else if (listData[0].equals("injectTouch")) {
			injectTouchProcess(listData);
		}
	}
	
	private void injectKeyProcess(String[] listD) {
		if (listD.length < 4) {
			System.err.println("Invalid content = " + listD);
			return;
		}
		int eventCode = Integer.parseInt(listD[1]);
		int scanCode = Integer.parseInt(listD[2]);
		int state = Integer.parseInt(listD[3]);
		long now = SystemClock.uptimeMillis();
		KeyEvent event = new KeyEvent(now, now, (state == 1) ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, eventCode, 0);
		injectKeyEvent(event);
	}
	
	private void injectTouchProcess(String[] listD) {
		int pointerCount = Integer.parseInt(listD[1]);
		int state = Integer.parseInt(listD[2]);
		long now = SystemClock.uptimeMillis();
		int index = 3;
		if (debug) {
			System.err.println("pointerCount = " + pointerCount + " state = " + state);
		}
		
		if (pointerCount == 0) {
			System.err.println("injectTouchProcess data is invalid");
			return;
		}
		
		PointerProperties[] properties = new PointerProperties[pointerCount];  
		PointerCoords[] pointerCoords = new PointerCoords[pointerCount];
		
		for (int i = 0; i < pointerCount; i ++) {
			PointerProperties mPointerProperties = new PointerProperties();  
			mPointerProperties.id= i;  
			mPointerProperties.toolType = MotionEvent.TOOL_TYPE_FINGER;  
			properties[i] = mPointerProperties; 
			
			PointerCoords mPointerCoords = new PointerCoords();
			mPointerCoords.x = Float.parseFloat(listD[index++]);
			mPointerCoords.y = Float.parseFloat(listD[index++]); 
			mPointerCoords.pressure = 1;  
			mPointerCoords.size = 1;  
			pointerCoords[i] = mPointerCoords;  
			if (debug) {
				System.err.println("pointerIndex = " + i + " x = " + mPointerCoords.x + " y = " + mPointerCoords.y);
			}
		}
		
		if (1 == pointerCount) {
			injectTouchEvent(MotionEvent.obtain(now, now, state, 1, properties, pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0));
		} else {
			//injectTouchEvent(MotionEvent.obtain(now, now, (state | ((pointerCount-1) << 8)), pointerCount, properties, pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0));
			injectTouchEvent(MotionEvent.obtain(now, now, (state), pointerCount, properties, pointerCoords, 0, 0, 1, 1, 0, 0, 0, 0));
		}
	}
	
	private void injectKeyEvent(final KeyEvent event) {
		if (event == null) {
			System.err.println("KeyEvent event == null");
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					(IWindowManager.Stub
			                .asInterface(ServiceManager.getService("window")))
			                .injectKeyEvent(event, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
	private void injectTouchEvent(final MotionEvent event) {
		if (event == null) {
			System.err.println(" MotionEvent  event == null");
			return;
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					(IWindowManager.Stub
			                .asInterface(ServiceManager.getService("window")))
			                .injectPointerEvent(event, true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
