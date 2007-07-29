/***
 * $Id: TelnetInputStream.java,v 1.1 2002/04/03 01:04:28 brekke Exp $
 *
 * NetComponents Internet Protocol Library
 * Copyright (C) 1997-2002  Daniel F. Savarese
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library in the LICENSE file; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307  USA
 ***/

package com.oroinc.net.telnet;

import java.io.*;

/***
 *
 * <p>
 *
 * <p>
 * <p>
 * @author Daniel F. Savarese
 ***/


final class TelnetInputStream extends BufferedInputStream implements Runnable {
  static final int _STATE_DATA = 0, _STATE_IAC = 1, _STATE_WILL = 2,
    _STATE_WONT = 3, _STATE_DO = 4, _STATE_DONT = 5, _STATE_SB = 6,
    _STATE_SE = 7, _STATE_CR = 8;

  private boolean __hasReachedEOF, __isClosed;
  private boolean __readIsWaiting;
  private int __receiveState, __queueHead, __queueTail, __bytesAvailable;
  private int[] __queue;
  private TelnetClient __client;
  private Thread __thread;
  private IOException __ioException;

  TelnetInputStream(InputStream input, TelnetClient client) {
    super(input);
    __client        = client;
    __receiveState  = _STATE_DATA;
    __isClosed = true;
    __hasReachedEOF = false;
    // Make it 1025, because when full, one slot will go unused, and we
    // want a 1024 byte buffer just to have a round number (base 2 that is)
    //__queue         = new int[1025];
    __queue         = new int[2049];
    __queueHead      = 0;
    __queueTail      = 0;
    __bytesAvailable = 0;
    __ioException    = null;
    __readIsWaiting  = false;
    __thread         = new Thread(this);
  }

  void _start() {
    int priority;
    __isClosed = false;
    // Need to set a higher priority in case JVM does not use pre-emptive
    // threads.  This should prevent scheduler induced deadlock (rather than
    // deadlock caused by a bug in this code).
    priority = Thread.currentThread().getPriority() + 1;
    if(priority > Thread.MAX_PRIORITY)
      priority = Thread.MAX_PRIORITY;
    __thread.setPriority(priority);
    __thread.setDaemon(true);
    __thread.start();
  }


  // synchronized(__client) critical sections are to protect against
  // TelnetOutputStream writing through the telnet client at same time
  // as a processDo/Will/etc. command invoked from TelnetInputStream
  // tries to write.
  private int __read() throws IOException {
    int ch;

  _loop:
    while(true) {
      // Exit only when we reach end of stream.
      if((ch = super.read()) < 0)
	return -1;

      ch = (ch & 0xff);

    _mainSwitch:
      switch(__receiveState) {

      case _STATE_CR:
	if(ch == '\0') {
	  // Strip null
	  continue;
	}
	// How do we handle newline after cr?
	//  else if (ch == '\n' && _requestedDont(TelnetOption.ECHO) &&
	    
	// Handle as normal data by falling through to _STATE_DATA case

      case _STATE_DATA:
	if(ch == TelnetCommand.IAC) {
	  __receiveState = _STATE_IAC;
	  continue;
	}


	if(ch == '\r') {
	  synchronized(__client) {	  
	    if(__client._requestedDont(TelnetOption.BINARY))
	      __receiveState = _STATE_CR;
	    else
	      __receiveState = _STATE_DATA;
	  }
	} else
	  __receiveState = _STATE_DATA;
	break;

      case _STATE_IAC:
	switch(ch) {
	case TelnetCommand.WILL: __receiveState = _STATE_WILL; continue;
	case TelnetCommand.WONT: __receiveState = _STATE_WONT; continue;
	case TelnetCommand.DO:   __receiveState = _STATE_DO;   continue;
	case TelnetCommand.DONT: __receiveState = _STATE_DONT; continue;
	case TelnetCommand.IAC:
	  __receiveState = _STATE_DATA;
	  break;
	default: break;
	}
	__receiveState = _STATE_DATA;
	continue;
      case _STATE_WILL:
	synchronized(__client) {
	  __client._processWill(ch);
	  __client._flushOutputStream();
	}
	__receiveState = _STATE_DATA;
	continue;
      case _STATE_WONT:
	synchronized(__client) {
	  __client._processWont(ch);
	  __client._flushOutputStream();
	}
	__receiveState = _STATE_DATA;
	continue;
      case _STATE_DO:
	synchronized(__client) {
	  __client._processDo(ch);
	  __client._flushOutputStream();
	}
	__receiveState = _STATE_DATA;
	continue;
      case _STATE_DONT:
	synchronized(__client) {
	  __client._processDont(ch);
	  __client._flushOutputStream();
	}
	__receiveState = _STATE_DATA;
	continue;
      }

      break;
    }

    return ch;
  }



  public int read() throws IOException {
    // Critical section because we're altering __bytesAvailable,
    // __queueHead, and the contents of _queue in addition to
    // testing value of __hasReachedEOF.
    synchronized(__queue) {

      while(true) {
	if(__ioException != null) {
	  IOException e;
	  e = __ioException;
	  __ioException = null;
	  throw e;
	}

	if(__bytesAvailable == 0) {
	  // Return -1 if at end of file
	  if(__hasReachedEOF)
	    return -1;

	  // Otherwise, we have to wait for queue to get something
	  __queue.notify();
	  try {
	    //System.out.println("READ WAIT");
	    __readIsWaiting = true;
	    __queue.wait();
	    __readIsWaiting = false;
	    //System.out.println("READ END WAIT");
	  } catch(InterruptedException e) {
	    throw new IOException("Fatal thread interruption during read.");
	  }
	  continue;
	} else {
	  int ch;

	  ch = __queue[__queueHead];

	  if(++__queueHead >= __queue.length)
	    __queueHead = 0;

	  --__bytesAvailable;

	  return ch;
	}
      }
    }
  }


  /***
   * Reads the next number of bytes from the stream into an array and
   * returns the number of bytes read.  Returns -1 if the end of the
   * stream has been reached.
   * <p>
   * @param buffer  The byte array in which to store the data.
   * @return The number of bytes read. Returns -1 if the
   *          end of the message has been reached.
   * @exception IOException If an error occurs in reading the underlying
   *            stream.
   ***/
  public int read(byte buffer[]) throws IOException {
    return read(buffer, 0, buffer.length);
  }                 

 
  /***
   * Reads the next number of bytes from the stream into an array and returns
   * the number of bytes read.  Returns -1 if the end of the
   * message has been reached.  The characters are stored in the array
   * starting from the given offset and up to the length specified.
   * <p>
   * @param buffer The byte array in which to store the data.
   * @param offset  The offset into the array at which to start storing data.
   * @param length   The number of bytes to read.
   * @return The number of bytes read. Returns -1 if the
   *          end of the stream has been reached.
   * @exception IOException If an error occurs while reading the underlying
   *            stream.
   ***/
  public int read(byte buffer[], int offset, int length) throws IOException {
    int ch, off;

    if(length < 1)
      return 0;

    // Critical section because run() may change __bytesAvailable
    synchronized(__queue) {
      if(length > __bytesAvailable)
	length = __bytesAvailable;
    }

    if((ch = read()) == -1)
      return -1;

    off = offset;

    do {
      buffer[offset++] = (byte)ch;
    } while(--length > 0 && (ch = read()) != -1);

    return (offset - off);
  }


  /*** Returns false.  Mark is not supported. ***/
  public boolean markSupported() { return false; }

  public int available() throws IOException {
    // Critical section because run() may change __bytesAvailable
    synchronized(__queue) {
      return __bytesAvailable;
    }
  }


  // Cannot be synchronized.  Will cause deadlock if run() is blocked
  // in read because BufferedInputStream read() is synchronized.
  public void close() throws IOException {
    // Completely disregard the fact thread may still be running.
    // We can't afford to block on this close by waiting for
    // thread to terminate because few if any JVM's will actually
    // interrupt a system read() from the interrupt() method.
    super.close();

    synchronized(__queue) {
      __hasReachedEOF = true;
      if(__thread.isAlive()) {
	__isClosed = true;
	__thread.interrupt();
      }
      __queue.notifyAll();
    }
    /*
    while(__thread.isAlive()) {
      __thread.interrupt();
      try {
	__thread.join();
      } catch(InterruptedException e) {
	// If this happens, we just continue to loop
      }
    }
    */
  }

  public void run() {
    int ch;

    try {
    _outerLoop:
      while(!__isClosed) {
	try {
	  if((ch = __read()) < 0)
	    break;
	} catch(InterruptedIOException e) {
	  synchronized(__queue) {
	    __ioException = e;
	    __queue.notify();
	    try {
	      //System.out.println("THREAD WAIT B");
	      __queue.wait();
	      //System.out.println("THREAD END WAIT B");
	    } catch(InterruptedException interrupted) {
	      if(__isClosed)
		break _outerLoop;
	    }
	    continue;
	  }
	}


	// Critical section because we're altering __bytesAvailable,
	// __queueTail, and the contents of _queue.
	synchronized(__queue) {
	  while(__bytesAvailable >= __queue.length - 1) {
	    __queue.notify();
	    try {
	      //System.out.println("THREAD WAIT");
	      __queue.wait();
	      //System.out.println("THREAD END WAIT");
	    } catch(InterruptedException e) {
	      if(__isClosed)
		break _outerLoop;
	    }
	  }

	  // Need to do this in case we're not full, but block on a read 
	  if(__readIsWaiting) {
	    //System.out.println("NOTIFY");
	    __queue.notify();
	  }

	  __queue[__queueTail] = ch;
	  ++__bytesAvailable;

	  if(++__queueTail >= __queue.length)
	    __queueTail = 0;
	}
      }
    } catch(IOException e) {
      synchronized(__queue) {
	__ioException = e;
      }
    }

    synchronized(__queue) {
      __isClosed = true; // Possibly redundant
      __hasReachedEOF = true;
      __queue.notify();
    }
  }


}