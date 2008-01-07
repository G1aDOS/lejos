package lejos.nxt.comm;
import java.util.*;


/**
 * Provides Bluetooth comminications
 * Allows inbound and outbound connections.
 * Access to to device registration
 */
public class Bluetooth
{
	public static  final int MSG_BEGIN_INQUIRY = 0;
	public static  final int MSG_CANCEL_INQUIRY = 1;
	public static  final int MSG_CONNECT = 2;
	public static  final int MSG_OPEN_PORT = 3;
	public static  final int MSG_LOOKUP_NAME = 4;
	public static  final int MSG_ADD_DEVICE = 5;
	public static  final int MSG_REMOVE_DEVICE = 6;
	public static  final int MSG_DUMP_LIST = 7;
	public static  final int MSG_CLOSE_CONNECTION = 8;
	public static  final int MSG_ACCEPT_CONNECTION = 9;
	public static  final int MSG_PIN_CODE = 10;
	public static  final int MSG_OPEN_STREAM = 11;
	public static  final int MSG_START_HEART = 12;
	public static  final int MSG_HEARTBEAT = 13;
	public static  final int MSG_INQUIRY_RUNNING = 14;
	public static  final int MSG_INQUIRY_RESULT = 15;
	public static  final int MSG_INQUIRY_STOPPED = 16;
	public static  final int MSG_LOOKUP_NAME_RESULT = 17;
	public static  final int MSG_LOOKUP_NAME_FAILURE = 18;
	public static  final int MSG_CONNECT_RESULT = 19;
	public static  final int MSG_RESET_INDICATION = 20;
	public static  final int MSG_REQUEST_PIN_CODE = 21;
	public static  final int MSG_REQUEST_CONNECTION = 22;
	public static  final int MSG_LIST_RESULT = 23;
	public static  final int MSG_LIST_ITEM = 24;
	public static  final int MSG_LIST_DUMP_STOPPED = 25;
	public static  final int MSG_CLOSE_CONNECTION_RESULT = 26;
	public static  final int MSG_PORT_OPEN_RESULT = 27;
	public static  final int MSG_SET_DISCOVERABLE = 28;
	public static  final int MSG_CLOSE_PORT = 29;
	public static  final int MSG_CLOSE_PORT_RESULT = 30;
	public static  final int MSG_PIN_CODE_ACK = 31;
	public static  final int MSG_SET_DISCOVERABLE_ACK = 32;
	public static  final int MSG_SET_FRIENDLY_NAME = 33;
	public static  final int MSG_SET_FRIENDLY_NAME_ACK = 34;
	public static  final int MSG_GET_LINK_QUALITY = 35;
	public static  final int MSG_LINK_QUALITY_RESULT = 36;
	public static  final int MSG_SET_FACTORY_SETTINGS = 37;
	public static  final int MSG_SET_FACTORY_SETTINGS_ACK = 38;
	public static  final int MSG_GET_LOCAL_ADDR = 39;
	public static  final int MSG_GET_LOCAL_ADDR_RESULT = 40;
	public static  final int MSG_GET_FRIENDLY_NAME = 41;
	public static  final int MSG_GET_DISCOVERABLE = 42;
	public static  final int MSG_GET_PORT_OPEN = 43;
	public static  final int MSG_GET_FRIENDLY_NAME_RESULT = 44;
	public static  final int MSG_GET_DISCOVERABLE_RESULT = 45;
	public static  final int MSG_GET_PORT_OPEN_RESULT = 46;
	public static  final int MSG_GET_VERSION = 47;
	public static  final int MSG_GET_VERSION_RESULT = 48;
	public static  final int MSG_GET_BRICK_STATUSBYTE_RESULT = 49;
	public static  final int MSG_SET_BRICK_STATUSBYTE_RESULT = 50;
	public static  final int MSG_GET_BRICK_STATUSBYTE = 51;
	public static  final int MSG_SET_BRICK_STATUSBYTE = 52;
	public static  final int MSG_GET_OPERATING_MODE = 53;
	public static  final int MSG_SET_OPERATING_MODE = 54;
	public static  final int MSG_OPERATING_MODE_RESULT = 55;
	public static  final int MSG_GET_CONNECTION_STATUS = 56;
	public static  final int MSG_CONNECTION_STATUS_RESULT = 57;
	public static  final int MSG_GOTO_DFU_MODE = 58;
	public static  final int MSG_ANY = -1;
	
	public static final int BT_PENDING_INPUT = 1;
	public static final int BT_PENDING_OUTPUT = 2;

	private static final int CHANCNT = 4;
	private static final int RS_INIT = -1;
	private static final int RS_IDLE = 0;
	private static final int RS_CMD = 1;
	private static final int RS_WAIT = 2;
	private static final int RS_REPLY = 3;
	private static final int RS_REQUESTCONNECT = 4;
	private static final int RS_ERROR = 5;
	
	private static final int IO_TIME = 100;
	private static final int CMD_TIME = 50;
	private static final int TO_SWITCH = 500;
	private static final int TO_REPLY = 250;
	private static final int TO_SHORT = 2000;
	private static final int TO_LONG = 30000;
	private static final int TO_RESET = 5000;
	private static final int TO_INIT = 500;
	private static final int TO_CLOSE = 100;
	private static final int TO_FORCERESET = -1;
	private static final int TO_NONE = 0;
	private static final int TO_FLUSH = 500;
	
	static BTConnection [] Chans = new BTConnection[CHANCNT];
	static byte [] cmdBuf = new byte[128];
	static byte [] replyBuf = new byte[256];
	static int cmdTimeout;
	static int reqState = RS_INIT;
	static int savedState;
	static boolean listening = false;
	static int connected;
	static int resetCnt;
	static boolean powerOn;
	public final static byte[] defaultPin = 
	    {(byte) '1', (byte) '2', (byte) '3', (byte) '4'};
	private static byte [] pin = defaultPin;
	static Object sync = new Object();

	/**
	 * Low-level method to write BT data
	 * 
	 * @param buf the buffer to send
	 * @param off the offset to start the write from.
	 * @param len the number of bytes to send
	 * @return number of bytes actually written
	 */
	public static native int btWrite(byte[] buf, int off, int len);
	/**
	 * Low-level method to read BT data
	 * 
	 * @param buf the buffer to send
	 * @param off the offset at which to start the transfer
	 * @param len the number of bytes to send
	 * @return number of bytes actually written
	 */
	public static native int btRead(byte[] buf, int off, int len);	
	/**
	 *Low-Level method to access the Bluetooth interface. Bitwise values returned.
	 * @return		0 No data pending
	 *				0x1 input pending
	 *				0x2 output pending
	 */
	public static native int btPending();
	/**
	 * Low-level method to switch BC4 chip between command
	 * and data (stream) mode.
	 * 
	 * @param mode 0=data mode, 1=command mode
	 */
	public static native void btSetArmCmdMode(int mode);
	
	/**
	 * Low-level method to get the BC4 chip mode
	 */
	public static native int btGetBC4CmdMode();
	
	/**
	 * Low-level method to start ADC converter
	 *
	 */
	public static native void btStartADConverter();

	/**
	 * Low-level method to take the BC4 reset line low
	 */
	public static native void btSetResetLow();
	
	/**
	 * Low-level method to take the BC4 reset line high
	 */
	public static native void btSetResetHigh();
	/**
	 * Perform a hardware reset of the BlueCore chip.
	 * 
	 */	

	
	public Bluetooth()
	{
	}
	
	private static void cmdInit(int cmd, int len, int param1, int param2)
	{
		// Helper function. Setup a simple command in the buffer ready to go.
		cmdBuf[0] = (byte)len;
		cmdBuf[1] = (byte)cmd;
		cmdBuf[2] = (byte)param1;
		cmdBuf[3] = (byte)param2;
	}
	
	private static void startTimeout(int period)
	{
		// Start a command timeout
		cmdTimeout = (int)System.currentTimeMillis() + period;
	}
	
	private static boolean checkTimeout()
	{
		return (cmdTimeout > 0 && (int)System.currentTimeMillis() > cmdTimeout);
	}
	
	private static void cancelTimeout()
	{
		cmdTimeout = -1;
	}
	
	/**
	 * The main Bluetooth control thread. This controls access to the Bluetooth
	 * interface. It controls and peforms all low level access to the device.
	 * Switches it between data channels and command streams as required.
	 */
	static class BTThread extends Thread
	{
		static final int MO_STREAM = 0;
		static final int MO_CMD = 1;
		private int mode;
		private int curChan;

		public BTThread()
		{
			mode = MO_CMD;
			reqState = RS_INIT;
			curChan = -1;
			resetCnt = 0;
			// Make sure power is on(may cause a reset!)
			setPower(true);
			for(int i = 0; i < CHANCNT; i++)
				Chans[i] = new BTConnection(i);
			connected = 0;
			listening = false;
			cancelTimeout();
			setDaemon(true);
			start();
			// Setup initial state
			setOperatingMode((byte)1);
			closePort();
		}

		
		private void sendCommand()
		{
			// Command should be all setup and ready to go in cmdBuf
			int checkSum = 0;
			int len = (int)cmdBuf[0] & 0xff;
			//1 Debug.out("send cmd " + (int)cmdBuf[1] + "\n");
			for(int i=0;i<len;i++)
			{
				checkSum += cmdBuf[i+1];
			}
			checkSum = -checkSum;
			cmdBuf[len+1] = (byte) ((checkSum >> 8) & 0xff);
			cmdBuf[len+2] = (byte) checkSum;
			cmdBuf[0] = (byte)(len + 2);
			btWrite(cmdBuf,0, len+3);
		}

		private int recvReply()
		{
			// Read a reply and place it in replyBuf
			if (checkTimeout()) return -1;
			int cnt = Bluetooth.btRead(replyBuf, 0, 1);
			if (cnt <= 0) return 0;
			int len = (int)replyBuf[0] & 0xff;
			if (len < 3 ||len >= replyBuf.length)
			{
				//1 Debug.out("Bad packet len " + len + " cnt " + cnt + "\n");
				return -1;
			}
			int timeout = (int)System.currentTimeMillis() + TO_REPLY;
			while (cnt < len+1)
			{
				cnt += Bluetooth.btRead(replyBuf, cnt, len + 1 - cnt);
				if ((int)System.currentTimeMillis() > timeout)
				{
					//1 Debug.out("recvReply timeout\n");
					return -1;
				}
			}
			
			int csum = len;	
			len -= 2;
			for(int i = 0; i < len; i++)
				csum += (int)replyBuf[i+1] & 0xff;
			csum = -csum;
			//1 Debug.out("Got reply " + replyBuf[1] + "\n");
			if (((byte) csum == replyBuf[len+2]) && ((byte)(csum >> 8) == replyBuf[len+1]))
				return len;
			else
			{
				//1 Debug.out("Bad csum\n");
				return -1;
			}
		}
	
		/**
		 * Perform a hardware reset of the BlueCore chip.
		 * 
		 */	
		private void reset() 
		{

			synchronized(Bluetooth.sync)
			{
				int len;
				//1 Debug.out("hardware reset\n");
				for(int resetCnt = 0; resetCnt < 2; resetCnt++)
				{
					// Ditch any pending data in the input buffer
					startTimeout(TO_FLUSH);
					while (!checkTimeout())
					{
						recvReply();
					}
					// Debug.out("End of flush\n");
					// BC4 reset seq. First take the reset line low...
					btSetArmCmdMode(MO_CMD);
					btSetResetLow();
					// Keep it that way for 100ms and discard any input
					startTimeout(100);
					while (!checkTimeout())
					{
						recvReply();
					}
					// Now bring it high
					btSetResetHigh();
					// Now wait for either 5 seconds or for a RESET_INDICATION
					startTimeout(TO_RESET);
					while ((len = recvReply()) == 0 || ( len > 0 && replyBuf[1] != MSG_RESET_INDICATION))
							Thread.yield();
					//1 if (len < 0) Debug.out("Reset timed out");
					// Check things are working
					//Debug.out("Send mode cmd\n");
					cmdInit(MSG_GET_OPERATING_MODE, 1, 0, 0);
					sendCommand();
					startTimeout(TO_SHORT);
					while ((len = recvReply()) == 0 || (len > 0 && replyBuf[1] != MSG_OPERATING_MODE_RESULT))
							Thread.yield();
					//1 if (len < 0) Debug.out("mode had timed out\n");
					// if we got the response without a timeout we are done!
					if (len > 0) break;
				}
				// We are now in command mode
				mode = MO_CMD;
				// Now reset everything else that is going on gulp!
				for(int i = 0; i < CHANCNT; i++)
					Chans[i].reset();
				//1 Debug.out("reset complete state is " + reqState + "\n");
				listening = false;
				connected = 0;
				curChan = -1;
				cancelTimeout();
				// Tell anyone that is waiting
				if (reqState > RS_IDLE)	reqState = RS_ERROR;
				Bluetooth.sync.notifyAll();
				resetCnt++;
			}
		}
		
		private void processReply()
		{
			// Read and process and command replies from the bc4
			// If the reply buffer is free, look to see if we have a new reply
			synchronized(Bluetooth.sync)
			{
				//Debug.out("processn reply " + reqState + "\n");
				int len;
				while (reqState < RS_REPLY && (len = recvReply()) != 0)
				{
					//Debug.out("process request\n");
					// Got a message. We only deal with the messages we have to deal
					// with here. In general we allow the calling thread to decide
					// what to do (this includes ignoring the message!).
					//1 Debug.out("got request " + (int)replyBuf[1] + " state " + reqState + "\n");
					if (len < 0 || replyBuf[1] == MSG_RESET_INDICATION)
					{
						//1 Debug.out("Got reply error\n");
						reset();
						break;
					}
					else if (replyBuf[1] == MSG_CLOSE_CONNECTION_RESULT)
					{
						if (replyBuf[3] >= 0 && replyBuf[3] < Chans.length)
						{
							if (Chans[replyBuf[3]].disconnected())
								connected--;
							if (replyBuf[3] == (byte)curChan) curChan = -1;
						}
					}
					else if (replyBuf[1] == MSG_REQUEST_CONNECTION)
					{
						if (listening)
						{
							// Push the current state
							savedState = reqState;
							reqState = RS_REQUESTCONNECT;
						}
						else
						{
							// No one wants to know so reject it.
							// Note: Doing this seems to cause a device reset!
							cmdInit(MSG_ACCEPT_CONNECTION, 2, 0, 0);
							sendCommand();
							continue;
						}
					}
					else if (replyBuf[1] == MSG_REQUEST_PIN_CODE)
					{
						// If we have no pin then nothing to do
						if (pin == null) continue;
						// Otherwise send the pin as requested
						cmdInit(MSG_PIN_CODE, 24, 0, 0);
						System.arraycopy(replyBuf, 2, cmdBuf, 2, 7);
						for(int i = 0; i < 16; i++)
							cmdBuf[i + 9] = (i < pin.length ? pin[i] : 0);
						sendCommand();
						continue;
					} else if (replyBuf[1] == MSG_PIN_CODE_ACK)
						continue;
					// All other messages give the caller it to deal with
					if (reqState == RS_WAIT)
						reqState = RS_REPLY;
					if (reqState >= RS_REPLY)
						Bluetooth.sync.notifyAll();
				}
			}
			//Debug.out("process reply end\n");
		}
		
		private void processCommands()
		{
			// Process commands. Return when we should consider switching to
			// stream mode.
			//Debug.out("Process cmd1\n");
			switchToCmd();
			int cmdEnd = (int)System.currentTimeMillis() + CMD_TIME;
			while (cmdEnd > (int)System.currentTimeMillis() || reqState > RS_IDLE)
			{
				//Debug.out("ProcessCommands state " + reqState + "\n");
				synchronized(Bluetooth.sync)
				{
					if (reqState == RS_CMD)
					{
						// Have a command ready to go so send it
						sendCommand();
						reqState = RS_WAIT;
					}
					processReply();
				}
				Thread.yield();
			}
			//Debug.out("Process cmd end\n");
		}
		
		private int selectChan()
		{
			// Select the next channel to be processed
			if (connected == 0) return -1;
			int i;
			int cur = curChan;
			for(i = 0; i < Chans.length; i++)
			{
				cur = (cur + 1) % Chans.length;
				if (Chans[cur].needsAttention()) 
				{
					// if (cur != curChan) Debug.out("Selected " + cur + "\n");
					return cur;
				}
			}
			// Nothing better found so stick with the current channel
			return curChan;
		}
		
		private void processStreams()
		{
			// Process the streams. Return when we should switch to command mode
			// Debug.out("PS cur " + curChan + " state " + reqState + "\n");
			while (true)
			{
				synchronized(Bluetooth.sync)
				{
					//Debug.out("Process streams " + reqState + "\n");
					if (reqState != RS_IDLE) return;
					int next = selectChan();
					if (next < 0 || !switchToStream(next)) return;
					// Perform I/O on the current stream. Switching from one stream
					// to another is a slow process, so we spend at least IO_TIME ms
					// on each stream before switching away.
					//Debug.out("Process streams 2" + reqState + "\n");
					int ioEnd = (int)System.currentTimeMillis() + IO_TIME;			
					while (ioEnd > (int)System.currentTimeMillis() && Chans[curChan].state >= BTConnection.CS_CONNECTED)
					{
						if (bc4Mode() != MO_STREAM) return;
						Chans[curChan].send();
						Chans[curChan].recv();
						Thread.yield();
					}
					//Debug.out("Process streams 3" + reqState + "\n");
					// Do we need to switch back to command mode?
					if (listening) return;
				}
				Thread.yield();
			}
		}
		
		private int waitSwitch(int target, boolean flush)
		{
			// Wait for the BC4 to switch to mode, or timeout...
			int timeout = (int) System.currentTimeMillis() + TO_SWITCH;
			while (timeout > (int)System.currentTimeMillis())
			{
				if (bc4Mode() == target) return target;
				// Need to flush input when switching to command mode
				if (flush && curChan >= 0) Chans[curChan].flushInput();
			}
			//1 Debug.out("Failed to switch\n");
			mode = -1;
			curChan = -1;
			return bc4Mode();
		}
		
		private boolean switchToStream(int chan)
		{
			// Decide which (if any) stream to switch to
			if (mode == MO_STREAM && chan == curChan) return true;
			switchToCmd();
			//1 Debug.out("Switch to chan " + chan + " handle " + Chans[chan].handle + "\n");
			cmdInit(MSG_OPEN_STREAM, 2, Chans[chan].handle, 0);
			sendCommand();
			// Now wait for the BC4 to switch
			if (waitSwitch(MO_STREAM, false) != MO_STREAM) return false;
			//1 Debug.out("In stream mode\n");
			// Make sure we process any remaining command input that may have arrived
			processReply();
			// Finally switch the ARM over
			btSetArmCmdMode(MO_STREAM);
			mode = MO_STREAM;
			curChan = chan;
			return true;
		}
		
		private void switchToCmd()
		{
			// Need to switch back into command mode
			// First step send any pending data
			if (mode == MO_CMD) return;
			//1 Debug.out("switch to cmd\n");
			if (mode == MO_STREAM && bc4Mode() == MO_CMD && curChan >= 0)
			{
				//1 Debug.out("Trying early flush\n");
				Chans[curChan].flushInput();
			}
			// wait for any output to drain
			while((btPending() & BT_PENDING_OUTPUT) != 0)
				Thread.yield();
			try{Thread.sleep(100);}catch(Exception e){}
			btSetArmCmdMode(MO_CMD);
			// If there is any input data left we could be in trouble. Try and
			// flush everything.
			if (waitSwitch(MO_CMD, true) != MO_CMD)
			{
				//1 Debug.out("Failed to switch to cmd\n");
				reset();
				return;
			}
			mode = MO_CMD;
		}
		
		private int bc4Mode()
		{
			// return the current mode of the BC4 chip
			int ret = btGetBC4CmdMode();
			// > 512 indicates a high logic level which is mode 0!
			if (ret > 512)
				return MO_STREAM;
			else
				return MO_CMD;
		}
		
		private void waitInit()
		{
			synchronized (Bluetooth.sync)
			{
				reqState = RS_INIT;
				processCommands();
				reqState = RS_IDLE;
				Bluetooth.sync.notifyAll();
			}
		}
		
		public void run()
		{
			//1 Debug.out("Thread running\n");
			waitInit();
			//1 Debug.out("Init complete\n");
			while(true)
			{
				processCommands();
				processStreams();
				Thread.yield();
			}
		}
	}

	// Create the Bluetooth device thread.
	private static BTThread btThread = new BTThread();
	
	static private int waitState(int target)
	{
		//Debug.out("Wait state " + target + "\n");
		synchronized (Bluetooth.sync) {
			// Wait for the system to enter the specified state (or timeout)
			while (reqState != target && reqState != RS_ERROR)
				try{Bluetooth.sync.wait();}catch(Exception e){}
			if (reqState == RS_ERROR)
				return -1;
			else
				return 0;
		}
	}
	
	private static void cmdStart()
	{
		// Wait for the system to be idle. Ignore timeout errors.
		while (waitState(RS_IDLE) < 0)
			;
	}
	
	private static void cmdComplete()
	{
		// command now complete. Reset to idle (clears any timeout state)
		synchronized(Bluetooth.sync)
		{
			reqState = RS_IDLE;
			cancelTimeout();
			Bluetooth.sync.notifyAll();
		}
	}
	
	private static int cmdWait(int state, int waitState, int msg, int timeout)
	{
		//Debug.out("Cmd wait\n");
		synchronized (Bluetooth.sync)
		{
			if (waitState > 0) reqState = waitState;
			if (timeout != TO_NONE) startTimeout(timeout);
			while (true)
			{
				if (waitState(state) < 0) return -1;
				if (msg == MSG_ANY || replyBuf[1] == msg) return 0;
				// Ignore any unwanted message
				if (reqState == RS_REPLY) reqState = RS_WAIT; 
			}
		}
	}

	/**
	 * Set the pin to be used for pairing/connecting to the system
	 * 
	 * @param newPin the new pin code
	 * 
	 */
	public static void setPin(byte[] newPin)
	{
		pin = newPin;
	}

	/**
	 * Return the pin to be used for pairing/connecting to the system
	 * 
	 * @return The current pin code
	 * 
	 */	
	public static byte[] getPin()
	{
		return pin;
	}

	
	/**
	 * Close an open connection
	 * 
	 * @param handle the handle for the connection
	 * @return the status 0 = success
	 */
	public static int closeConnection(byte handle)
	{
		int ret = -1;
		//1 Debug.out("Close connection state " + reqState + "\n");
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			// We can have a race condition when both ends of the connection
			// close at the same time. This can mean we try and close an already
			// closed connection. If we do this the BC4 goes into reset mode.
			// To try and avoid this happening we insert a different length
			// delay between outbound and inbound streams. We then wait to see
			// if the other end has already closed things...
			int timeout = (handle == 3 ? 5*TO_CLOSE : TO_CLOSE);
			reqState = RS_WAIT;
			try{Bluetooth.sync.wait(timeout);}catch(Exception e){}
			reqState = RS_IDLE;
			// There is a small chance that we may have had a reset so make sure
			// that we still have an open channel to close!
			byte [] status = getConnectionStatus();
			//1 Debug.out("Conn status " + status[handle]);
			if (status == null || status[handle] != 2) return -1;
			if (Chans[handle].state != BTConnection.CS_DISCONNECTING) return -1;
			cmdInit(MSG_CLOSE_CONNECTION, 2, handle, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_CLOSE_CONNECTION_RESULT, TO_SHORT) >= 0)
				ret = (int)replyBuf[2];
			int retryCnt = 5;
			do {
				// We may have a race condition here, or have triggered a reset
				// wait for things to settle
				reqState = RS_WAIT;
				try{Bluetooth.sync.wait(TO_REPLY);}catch(Exception e){}
				reqState = RS_IDLE;
			} while (getConnectionStatus() == null);
			cmdComplete();
			return ret;
		}
	}

	/**
	 * Opens the  port to allow incoming connections.
	 * 
	 * @return an array of three bytes: success, handle, ps_success
	 */
	public static byte[] openPort() {
		byte[] result = new byte[3];
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			cmdInit(MSG_OPEN_PORT, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_PORT_OPEN_RESULT, TO_SHORT) < 0)
				result = null;
			else
				System.arraycopy(replyBuf, 2, result, 0, 3);
			//1 Debug.out("Port open handle " + (int)replyBuf[3] + " status " + (int)replyBuf[2] + "\n");
			cmdComplete();
			return result;
		}
	}

	
	/**
	 * Closes the  port to disallow incoming connections.
	 * 
	 * @return an array of two bytes: success, ps_success
	 */
	public static byte [] closePort() {
		byte [] result = new byte[2];
		//1 Debug.out("Close port\n");
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			// The Lego doc says the handle should always be 3!
			cmdInit(MSG_CLOSE_PORT, 2, 3, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_CLOSE_PORT_RESULT, TO_SHORT) < 0)
				result = null;
			else
				System.arraycopy(replyBuf, 2, result, 0, 2);
			cmdComplete();
			return result;
		}
	}

	/**
	 * Wait for a remote device to connect.
	 * 
	 * @param pin the pin to use
	 * @return a BTConnection
	 */
	public static BTConnection waitForConnection(byte[] pin)
	{
		//1 Debug.out("waitForConnection\n");
		synchronized (Bluetooth.sync)
		{
			BTConnection ret = null;
			// only one listener at once
			if (listening) return null;
			// First open up the listening port
			byte [] port = openPort();
			if (port == null || port[0] != 1 || port[1] >=  Chans.length || port[1] < 0)
			{
				//1 Debug.out("Failed to open port\n");
				return null;
			}
			// Now in listening mode
			listening = true;
			byte []savedPin = getPin();
			setPin(pin);
			// Wait for special connect indication
			while (listening && reqState != RS_REQUESTCONNECT)
				try{Bluetooth.sync.wait();}catch(Exception e){}
			if (listening)
			{
				//1 Debug.out("Got connect request\n");
				// Restore state
				reqState = savedState;
				// and wait until we have control
				cmdStart();
				// Acknowledge the request
				cmdInit(MSG_ACCEPT_CONNECTION, 2, 1, 0);
				if (cmdWait(RS_REPLY, RS_CMD, MSG_CONNECT_RESULT, TO_LONG) >= 0)
				{
					//1 Debug.out("Connect result " + (int)replyBuf[2] + " handle " + (int)replyBuf[3] + "\n");
					if (replyBuf[2] == 1)
					{
						byte handle = replyBuf[3];
						// Got a connection
						if (handle >= 0 && handle < Chans.length)
						{
							// Assert(Chans[handle].state == CS_IDLE);
							Chans[handle].bind(handle);
							// now have one more connected
							connected++;
							ret = Chans[handle];
						}
					}
				}
				listening = false;
				cmdComplete();
				setPin(savedPin);
			}
			closePort();
			return ret;
		}
	}
		
	public static BTConnection waitForConnection()
	{
		return waitForConnection(defaultPin);
	}
	
	/**
	 * Connects to a remote device
	 * 
	 * @param remoteDevice remote device
	 * @return BTConnection Object or null
	 */
	public static BTConnection connect(BTRemoteDevice remoteDevice) {
		if (remoteDevice == null) return null;
		return connect(remoteDevice.getDeviceAddr());
	}
	/**
	 * Connects to a Device by it's Byte-Device-Address Array
	 * Uses default pin "1234"
	 * 
	 * @param device_addr byte-Array with device-Address
	 * @return BTConnection Object or null
	 */
	public static BTConnection connect(byte[] device_addr) {
		return connect(device_addr, defaultPin);
	}
	
	/**
	 * Connects to a Device by it's Byte-Device-Address Array
	 * 
	 * @param device_addr byte-Array with device-Address
	 * @param pin the pin to use
	 * @return BTConnection Object or null
	 */
	public static BTConnection connect(byte[] device_addr, byte[] pin) {
		//1 Debug.out("Connect\n");
		synchronized(Bluetooth.sync)
		{
			BTConnection ret = null;
			byte[] savedPin = getPin();
			setPin(pin);
			cmdStart();
			cmdInit(MSG_CONNECT, 8, 0, 0);
			System.arraycopy(device_addr, 0, cmdBuf, 2, 7);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_CONNECT_RESULT, TO_LONG) >= 0)
			{
				//1 Debug.out("Connect result " + (int)replyBuf[2] + " handle " + (int)replyBuf[3] + "\n");
				if (replyBuf[2] != 0)
				{
					byte handle = replyBuf[3];
					// Now need to check that the connection is not closed imm
					reqState = RS_WAIT;
					try{Bluetooth.sync.wait(300);}catch(Exception e){}
					if (reqState == RS_WAIT && handle >= 0 && handle < Chans.length)
					{
						// Got a connection
						Chans[handle].bind(handle);
						// now have one more connected
						connected++;
						ret = Chans[handle];
					}
				}
			}
			cmdComplete();
			setPin(savedPin);
			return ret;
		}
	}

	
	/**
	 * Get the Bluetooth signal strength (link quality)
	 * Higher values mean stronger signal.
	 * 
	 * 
	 * @return link quality value 0 to 255. 
	 * 
	 */
	public static int getSignalStrength(byte handle) {
		//1 Debug.out("getSignalStrength\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			if (Chans[handle].state != BTConnection.CS_CONNECTED) return -1;
			cmdInit(MSG_GET_LINK_QUALITY, 2, handle, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_LINK_QUALITY_RESULT, TO_SHORT) >= 0)	
				ret = replyBuf[2] & 0xff;
			cmdComplete();
			return ret;
		}
	}
	
	
	/**
	 * Get the friendly name of the local device
	 * @return the friendly name
	 */
	public static byte [] getFriendlyName() {
		byte[] result = new byte[16];
		//1 Debug.out("getFriendlyName\n");
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			cmdInit(MSG_GET_FRIENDLY_NAME, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_GET_FRIENDLY_NAME_RESULT, TO_SHORT) < 0)	
				result = null;
			else
				System.arraycopy(replyBuf, 2, result, 0, 16);
			cmdComplete();
			return result;
		}
	}
		
	/**
	 * Set the name of the local device
	 * @param name the friendly name for the device
	 */
	public static boolean setFriendlyName(byte[] name) {
		//1 Debug.out("setFriendlyName\n");
		synchronized (Bluetooth.sync)
		{
			boolean ret=false;
			cmdStart();
			cmdInit(MSG_SET_FRIENDLY_NAME, 17, 0, 0);
			System.arraycopy(name, 0, cmdBuf, 2, 16);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_SET_FRIENDLY_NAME_ACK, TO_LONG) >= 0)	
				ret = true;
			cmdComplete();
			return ret;
		}	
	}
		
	/**
	 * get the Bluetooth address of the local device
	 * @return the local address
	 */
	public static byte[] getLocalAddress() {
		byte[] result = new byte[7];
		//1 Debug.out("getLocalAddress\n");
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			cmdInit(MSG_GET_LOCAL_ADDR, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_GET_LOCAL_ADDR_RESULT, TO_SHORT) < 0)	
				result = null;
			else
				System.arraycopy(replyBuf, 2, result, 0, 7);
			cmdComplete();
			return result;
		}
	}	
	
	
	/**
	 * The internal Chip has a list of already paired Devices. This Method returns a 
	 * Vector-List which contains all the known Devices on the List. These need not be reachable. 
	 * To connect to a "not-known"-Device, you should use the Inquiry-Process. 
	 * The pairing-Process can also be done with the original Lego-Firmware. The List of known 
	 * devices will not get lost, when installing the LeJOS Firmware. 
	 * 
	 * @return Vector with List of known Devices
	 */
	public static Vector getKnownDevicesList() {
		//1 Debug.out("getKnownDevicesList\n");
		synchronized(Bluetooth.sync)
		{
			int state = RS_CMD;
			byte[] device = new byte[7];
			byte[] devclass = new byte[4];
			char[] name = new char[16];
			Vector retVec = new Vector(1);
			BTRemoteDevice curDevice;
			cmdStart();
			cmdInit(MSG_DUMP_LIST, 1, 0, 0);
			while (cmdWait(RS_REPLY, state, MSG_ANY, TO_LONG) >= 0)
			{
				state = RS_WAIT;
				if (replyBuf[1] == MSG_LIST_ITEM)
				{
					System.arraycopy(replyBuf, 2, device, 0, 7);
					int nl = 0;
					for(; nl < 16 && replyBuf[nl+9] != 0; nl++)
						name[nl] = (char)replyBuf[nl+9];
					System.arraycopy(replyBuf, 25, devclass, 0, 4);
					curDevice = new BTRemoteDevice(name, nl, device, devclass);
					//1 Debug.out("got name " + curDevice.getFriendlyName() + "\n");
					retVec.addElement(curDevice);
				}
				else if (replyBuf[1] == MSG_LIST_DUMP_STOPPED)
					break;
			}
			cmdComplete();
			return retVec;
		}
	}
	
	/**
	 * Gets a Device of the BC4-Chips internal list of known Devices 
	 * (those who have been paired before) into the BTDevice Object. 
	 * @param fName Friendly-Name of the device
	 * @return BTDevice Object or null, if not found.
	 */
	public static BTRemoteDevice getKnownDevice(String fName) {
		BTRemoteDevice btd = null;
		//look the name up in List of Known Devices
		Vector devList = getKnownDevicesList();
		if (devList.size() > 0) {
			for (int i = 0; i < devList.size(); i++) {
				btd = (BTRemoteDevice) devList.elementAt(i);
				if (btd.getFriendlyName().equals(fName)) {
					return btd; 
				}
			}
		}
		return null;
	}

	/**
	 * Add device to known devices
	 * @param d Remote Device
	 * @return true iff add was successful
	 */
	public static boolean addDevice(BTRemoteDevice d) {
		byte [] addr = d.getDeviceAddr();
		String name = d.getFriendlyName();
		byte[] cod = d.getDeviceClass();
		//1 Debug.out("addDevice " + name + "\n");
		synchronized (Bluetooth.sync)
		{
			boolean ret=false;
			cmdStart();
			cmdInit(MSG_ADD_DEVICE, 28, 0, 0);
			System.arraycopy(addr, 0, cmdBuf, 2, 7);
			System.arraycopy(cod, 0, cmdBuf, 25, 4);
			for(int i=0;i<name.length();i++)  cmdBuf[i+9] = (byte) name.charAt(i);
			for(int i=name.length(); i < 16; i++) cmdBuf[i+9] = 0;
			if (cmdWait(RS_REPLY, RS_CMD, MSG_LIST_RESULT, TO_LONG) >= 0)	
				ret = replyBuf[2] == 0x50;
			cmdComplete();
			return ret;
		}		
	}
	
	/**
	 * Add device to known devices
	 * @param d Remote Device
	 * @return true iff remove was successful
	 */
	public static boolean removeDevice(BTRemoteDevice d) {
		byte [] addr = d.getDeviceAddr();
		synchronized (Bluetooth.sync)
		{
			boolean ret = false;
			cmdStart();
			cmdInit(MSG_REMOVE_DEVICE, 8, 0, 0);
			System.arraycopy(addr, 0, cmdBuf, 2, 7);	
			if (cmdWait(RS_REPLY, RS_CMD, MSG_LIST_RESULT, TO_LONG) >= 0)	
				ret = replyBuf[2] == 0x53;
			cmdComplete();
			return ret;
		}	
	}
	
	/**
	 * Start a Bluetooth inquiry process
	 * 
	 * @param maxDevices the maximum number of devices to discover
	 * @param timeout the timeout value in units of 1.28 econds
	 * @param cod the class of device to look for
	 * @return a vector of all the devices found
	 */
	public static Vector inquire(int maxDevices,  int timeout, byte[] cod) {
		Vector retVec = new Vector();
		byte[] device = new byte[7];
		char[] name = new char[16];
		synchronized (Bluetooth.sync)
		{
			int state = RS_CMD;
			cmdStart();
			cmdInit(MSG_BEGIN_INQUIRY, 8, maxDevices, 0);
			cmdBuf[4] = (byte)timeout;
			System.arraycopy(cod, 0, cmdBuf, 5, 4);	
			while (cmdWait(RS_REPLY, state, MSG_ANY, TO_LONG) >= 0)
			{
				state = RS_WAIT;
				if (replyBuf[1] == MSG_INQUIRY_RESULT)
				{
					System.arraycopy(replyBuf, 2, device, 0, 7);
					int nameLen = 0;
					for(;nameLen<16 && replyBuf[9+nameLen] != 0;nameLen++)
						name[nameLen] = (char) replyBuf[9+nameLen];
					System.arraycopy(replyBuf, 25, cod, 0, 4);
					// add the Element to the Vector List
					retVec.addElement(new BTRemoteDevice(name, nameLen, device, cod));					
				}
				else if (replyBuf[1] == MSG_INQUIRY_STOPPED)
				{
					cmdComplete();
					// Fill in the names	
					for (int i = 0; i < retVec.size(); i++) {
						BTRemoteDevice btrd = ((BTRemoteDevice) retVec.elementAt(i));
						String s = btrd.getFriendlyName();
						if (s.length() == 0) {
							String nm = lookupName(btrd.getDeviceAddr());
							btrd.setFriendlyName(nm.toCharArray(),nm.length());
						}
					}
					return retVec;
				}
			}
			cmdComplete();
			return retVec;
		}			
	}
	
	/**
	 * Look up the name of a device using its address
	 * 
	 * @param addr device address
	 * @return friendly name of device
	 */
	public static String lookupName(byte [] addr) {
		char[] name = new char[16];
		synchronized (Bluetooth.sync)
		{
			String ret = "";
			int state = RS_CMD;
			cmdStart();
			cmdInit(MSG_LOOKUP_NAME, 8, 0, 0);
			System.arraycopy(addr, 0, cmdBuf, 2, 7);	
			while (cmdWait(RS_REPLY, state, MSG_ANY, TO_LONG) >= 0)
			{
				state = RS_WAIT;
				if (replyBuf[1] == MSG_LOOKUP_NAME_RESULT)
				{
					int len = 0;
					for(; len < 16 && replyBuf[len+9] != 0; len++)
						name[len] = (char)replyBuf[len+9];
					ret = new String(name, 0, len);
					break;
				}
				else if (replyBuf[1] == MSG_LOOKUP_NAME_FAILURE)
					break;
				
			}
			cmdComplete();
			return ret;
		}			
	}
		
	
	/**
	 * Get the status of all connections
	 * 
	 * @return byte array of status for each handle
	 */
	public static byte[] getConnectionStatus() {
		byte[] result = new byte[4];
		//1 Debug.out("getConnectionStatus\n");
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			cmdInit(MSG_GET_CONNECTION_STATUS, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_CONNECTION_STATUS_RESULT, TO_SHORT) < 0)	
				result = null;
			else
				System.arraycopy(replyBuf, 5, result, 0, 4);
			cmdComplete();
			return result;
		}
	}
	
	/**
	 * Get the major and minor version of the BlueCore code
	 * 
	 * @return an array of two bytes: major version, minor version
	 */
	public static byte[] getVersion() {
		byte [] version = new byte[2];
		//1 Debug.out("getVersion\n");
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			cmdInit(MSG_GET_VERSION, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_GET_VERSION_RESULT, TO_SHORT) < 0)	
				version = null;
			else
				System.arraycopy(replyBuf, 2, version, 0, 2);
			cmdComplete();
			return version;
		}
	}
	
	/**
	 * Get the persistent status value from the BC4 chip
	 * 
	 * @return the byte value
	 */
	public static int getStatus() {
		//1 Debug.out("getStatus\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_GET_BRICK_STATUSBYTE, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_GET_BRICK_STATUSBYTE_RESULT, TO_SHORT) >= 0)	
				ret = ((int)replyBuf[2] & 0xff) | (((int)replyBuf[3] & 0xff) << 8);
			cmdComplete();
			return ret;
		}
	}

	/**
	 * Set the persistent status byte for the BC4 chip
	 * 
	 * @param status the byte status value
	 * @return < 0 Error
	 */
	public static int setStatus(int status) {
		//1 Debug.out("setStatus\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_SET_BRICK_STATUSBYTE, 3, status & 0xff, (status >> 8) & 0xff);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_SET_BRICK_STATUSBYTE_RESULT, TO_SHORT) >= 0)	
				ret = 0;
			cmdComplete();
			return ret;
		}
	}
	
	/**
	 * Get the visibility (discoverable) status of the device
	 * 
	 * @return 1 = visible, 0 = invisible
	 */
	public static int getVisibility() {
		//1 Debug.out("getVisibility\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_GET_DISCOVERABLE, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_GET_DISCOVERABLE_RESULT, TO_SHORT) >= 0)	
				ret = replyBuf[2];
			cmdComplete();
			return ret;
		}
	}
	
	/**
	 * Get the port open status, 
	 * i.e whether connections are being accepted
	 * 
	 * @return 1 if the port is open, 0 otherwise
	 */
	public static int getPortOpen() {
		//1 Debug.out("getPortOpen\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_GET_PORT_OPEN, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_GET_PORT_OPEN_RESULT, TO_SHORT) >= 0)	
				ret = replyBuf[2];
			cmdComplete();
			return ret;
		}	
	}
	
	/**
	 * Get the operating mode (stream breaking or not) 
	 * 
	 * @return 0 = stream breaking mode, 1 = don't break stream mode
	 *		   < 0 Error
	 */
	public static int getOperatingMode() {
		//1 Debug.out("getOperatingMode\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_GET_OPERATING_MODE, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_OPERATING_MODE_RESULT, TO_SHORT) >= 0)	
				ret = replyBuf[2];
			cmdComplete();
			return ret;
		}	
	}
	
	/**
	 * Set Bluetooth visibility (discoverable) on or off for the local device
	 * 
	 * @param visible true to set visibility on, false to set it off
	 * @return < 0 error 
	 */
	public static int setVisibility(byte visible) {
		//1 Debug.out("setVisibility\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_SET_DISCOVERABLE, 2, visible, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_SET_DISCOVERABLE_ACK, TO_SHORT) >= 0)	
				ret = 0;
			cmdComplete();
			return ret;
		}	
	}
	
	/**
	 * Reset the settings of the BC4 chip to the factory defaults.
	 * The NXT should be restarted after this.
	 *
	 */
	public static int setFactorySettings() {
		//1 Debug.out("setFactorySettings\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_SET_FACTORY_SETTINGS, 1, 0, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_SET_FACTORY_SETTINGS_ACK, TO_SHORT) >= 0)	
				ret = 0;
			cmdComplete();
			return ret;
		}
	}
	
	/**
	 * Set the operating mode
	 * 
	 * @param mode 0 = Stream breaking, 1 don't break stream 
	 * @return	< 0 error
	 */
	public static int setOperatingMode(byte mode) {
		//1 Debug.out("setOperatingMode\n");
		synchronized (Bluetooth.sync)
		{
			int ret = -1;
			cmdStart();
			cmdInit(MSG_SET_OPERATING_MODE, 2, mode, 0);
			if (cmdWait(RS_REPLY, RS_CMD, MSG_OPERATING_MODE_RESULT, TO_SHORT) >= 0)	
				ret = 0;
			cmdComplete();
			return ret;
		}
	}
	
	/**
	 * Force a reset of the Bluetooth module.
	 * Notes:
	 * After this call power will be on.
	 * Any existing connections will be closed
	 * Any listening threads will be aborted
	 * 
	 */	
	public static void reset()
	{
		synchronized (Bluetooth.sync)
		{
			cmdStart();
			// Force a timeout and hence a reset
			cmdWait(RS_REPLY, RS_WAIT, MSG_RESET_INDICATION, TO_FORCERESET);
			cmdComplete();
		}		
	}
	
	/**
	 * Set the power to the module
	 * 
	 * @param on power on or off 
	 */
	public static void setPower(boolean on)
	{
		if (on)
			btSetResetHigh();
		else
			btSetResetLow();
		powerOn = on;
	}

	/**
	 * Return the current state of the module power
	 * 
	 * @return power on or off 
	 */
	public static boolean getPower()
	{
		return powerOn;
	}
	

	public static int getResetCount()
	{
		return resetCnt;
	}
	/**
	 * The following are provided for compatibility with the old Bluetooth
	 * class. They should not be used, in new programs and should probably
	 * be removed.
	 */
	
	/**
	 * Read a packet from the stream. Do not block and for small packets
	 * (< 254 bytes), do not return a partial packet.
	 * @param	buf		Buffer to read data into.
	 * @param	len		Number of bytes to read.
	 * @return			> 0 number of bytes read.
	 *					other values see read.
	 */
	public static int readPacket(byte buf[], int len)
	{
		return Chans[3].readPacket(buf, len);
	}
	
	/**
	 * Send a data packet.
	 * Must be in data mode.
	 * @param buf the data to send
	 * @param bufLen the number of bytes to send
	 */
	public static void sendPacket(byte [] buf, int bufLen)
	{
		Chans[3].sendPacket(buf, bufLen);
	}
	
	/**
	 * Set the BC4 mode, and wait for that mode to be confirmed by the chip.
	 *
	 * @param mode the requested mode 1 == Command mode 0 == Stream mode
	 */
	public static void btSetCmdMode(int mode)
	{
		
	}

}
