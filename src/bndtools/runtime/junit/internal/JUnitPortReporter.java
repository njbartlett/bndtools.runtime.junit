/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.runtime.junit.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.osgi.framework.Bundle;

public class JUnitPortReporter implements TestReporter {
	
	private final Logger log;
	private final PrintWriter output;
	
	long startTime;
	List<? extends Test> tests;
	Test current;
	private LinkedList<Exception> errors;
	private final Reader in;

	public static JUnitPortReporter createReporter(int port) {
		Logger log = Logger.getLogger(JUnitPortReporter.class.getPackage().getName());
		try {
			Socket socket = null;
			for (int attempt = 0; socket == null && attempt < 10; attempt++) {
				try {
					log.log(Level.FINE, "Connecting to JUnit reporting socket on port {0}, attempt number {1}.",
							new Object[] { Integer.toString(port), attempt });
					socket = new Socket((String) null, port);
				} catch (ConnectException ce) {
					try {
						Thread.sleep(attempt * 200);
					} catch (InterruptedException e) {
						return null;
					}
				}
			}
			if (socket == null) {
				log.log(Level.SEVERE, "Unable to connect to JUnit reporting socket on port {0}", Integer.toString(port));
				return null;
			}

	        Reader in = new InputStreamReader(socket.getInputStream(), "UTF-8");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
			return new JUnitPortReporter(in, out, log);
		} catch (UnsupportedEncodingException e) {
			// Can't happen, surely??
			throw new RuntimeException(e);
		} catch (IOException e) {
			log.log(Level.SEVERE, "Error connecting to JUnit reporting socket.", e);
			return null;
		}
	}
    
    private JUnitPortReporter(Reader in, PrintWriter out, Logger log) {
    	this.in = in;
		this.output = out;
    	this.log = log;
    }

    public void begin(Bundle[] allBundles, List<? extends Test> tests, int realCount) {
        this.tests = tests;
        message("%TESTC  ", realCount + " v2");
        report(tests);
        startTime = System.currentTimeMillis();
        errors = new LinkedList<Exception>();
    }

    public Collection<Exception> end() {
    	synchronized (output) {
    		message("%RUNTIME", "" + (System.currentTimeMillis() - startTime));
    		output.flush();
    		output.close();
		}
        try {
            in.close();
        } catch (Exception ioe) {
            // ignore
        }
        return errors;
    }

    public void addError(Test test, Throwable t) {
        message("%ERROR  ", test);
        trace(t);
    }

    public void addFailure(Test test, AssertionFailedError t) {
        message("%FAILED ", test);
        trace(t);
    }

    void trace(Throwable t) {
    	synchronized (output) {
    		message("%TRACES ", "");
    		t.printStackTrace(output);
    		output.println();
    		message("%TRACEE ", "");
		}
    }

    public void endTest(Test test) {
        message("%TESTE  ", test);
    }

    public void startTest(Test test) {
        this.current = test;
        message("%TESTS  ", test);
    }

    private void message(String key, String payload) {
        if (key.length() != 8)
            throw new IllegalArgumentException(key + " is not 8 characters");
        synchronized(output) {
	        output.print(key);
	        output.println(payload);
	        output.flush();
        }
        log.log(Level.FINEST, "{0}{1}", new Object[] { key, payload });
    }

    private void message(String key, Test test) {
        int index = tests.indexOf(test);
		message(key, (index + 1) + "," + test);
    }

    private void report(List<? extends Test> flattened) {
        for (int i = 0; i < flattened.size(); i++) {
            StringBuffer sb = new StringBuffer();
            sb.append(i + 1);
            sb.append(",");
            Test test = flattened.get(i);
            sb.append(flattened.get(i));
            sb.append(",");
            sb.append(test instanceof TestSuite);
            sb.append(",");
            sb.append(test.countTestCases());
            message("%TSTTREE", sb.toString());
        }
    }

    public void aborted() {
        end();
    }
}
