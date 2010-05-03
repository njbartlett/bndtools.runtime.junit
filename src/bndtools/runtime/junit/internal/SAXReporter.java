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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

class SAXReporter implements TestReporter {
	
	private static final String CDATA = "CDATA";
	
	private static final String ELEM_TEST = "test";
	private static final String ELEM_TESTREPORT = "testreport";
	private static final String ELEM_ABORTED = "aborted";
	private static final String ATTR_TIME = "time";

	private static final String ATTR_BUNDLE_LOCATION = "location";
	private static final String ATTR_BUNDLE_MODIFIED = "modified";
	private static final String ATTR_BUNDLE_STATE = "state";
	private static final String ATTR_BUNDLE_ID = "id";
	private static final String ATTR_BUNDLE_BSN = "bsn";
	private static final String ATTR_BUNDLE_VERSION = "version";

	private static final String ELEM_ERROR = "error";
	private static final String ELEM_FAILURE = "failure";

	private static final String ATTR_TEST_NAME = "name";
	private static final String ATTR_TEST_CLASS = "class";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_MESSAGE = "message";


	
	final File outputFile;
	final DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss:SSS");
	
	TransformerHandler handler;
	Collection<Exception> errors;
	private boolean finished;

	SAXReporter(File output) {
		this.outputFile = output;
	}
	
	public void begin(Bundle[] allBundles, List<? extends Test> tests, int realCount) throws Exception {
		SAXTransformerFactory xformFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
		
		finished = false;
		handler = xformFactory.newTransformerHandler();
		handler.setResult(new StreamResult(outputFile));
		
		AttributesImpl atts;
		
		handler.startDocument();
		atts = new AttributesImpl();
		atts.addAttribute(null, ATTR_TIME, ATTR_TIME, CDATA, dateFormat.format(new Date()));
		handler.startElement(null, ELEM_TESTREPORT, ELEM_TESTREPORT, atts);
		
		for (Bundle bundle : allBundles) {
			atts = new AttributesImpl();
			atts.addAttribute(null, ATTR_BUNDLE_LOCATION, ATTR_BUNDLE_LOCATION, CDATA, bundle.getLocation());
			atts.addAttribute(null, ATTR_BUNDLE_MODIFIED, ATTR_BUNDLE_MODIFIED, CDATA, Long.toString(bundle.getLastModified()));
			atts.addAttribute(null, ATTR_BUNDLE_STATE, ATTR_BUNDLE_STATE, CDATA, Integer.toString(bundle.getState()));
			atts.addAttribute(null, ATTR_BUNDLE_ID, ATTR_BUNDLE_ID, CDATA, Long.toString(bundle.getBundleId()));
			atts.addAttribute(null, ATTR_BUNDLE_BSN, ATTR_BUNDLE_BSN, CDATA, bundle.getSymbolicName());
			atts.addAttribute(null, ATTR_BUNDLE_VERSION, ATTR_BUNDLE_VERSION, CDATA, "" + bundle.getHeaders().get(Constants.BUNDLE_VERSION));
		}
	}
	
	public void aborted() {
		try {
			handler.endElement(null, ELEM_TEST, ELEM_TEST);
			
			handler.startElement(null, ELEM_ABORTED, ELEM_ABORTED, null);
			handler.endElement(null, ELEM_ABORTED, ELEM_ABORTED);
		} catch (SAXException e) {
			errors.add(e);
		}
	}

	public Collection<Exception> end() {
		try {
			handler.endElement(null, ELEM_TESTREPORT, ELEM_TESTREPORT);
			handler.endDocument();
		} catch (SAXException e) {
			errors.add(e);
		}
		return errors;
	}

	public void addError(Test test, Throwable t) {
		try {
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute(null, ATTR_TEST_NAME, ATTR_TEST_NAME, CDATA, test.toString());
			atts.addAttribute(null, ATTR_TYPE, ATTR_TYPE, CDATA, t.toString());
			handler.startElement(null, ELEM_ERROR, ELEM_ERROR, atts);
			
			handler.startCDATA();
			StringWriter tmp = new StringWriter();
			t.printStackTrace(new PrintWriter(tmp));
			char[] chars = tmp.toString().toCharArray();
			handler.characters(chars, 0, chars.length);
			handler.endCDATA();
			
			handler.endElement(null, ELEM_ERROR, ELEM_ERROR);
		} catch (SAXException e) {
			errors.add(e);
		}
	}

	public void addFailure(Test test, AssertionFailedError t) {
		try {
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute(null, ATTR_TEST_NAME, ATTR_TEST_NAME, CDATA, test.toString());
			atts.addAttribute(null, ATTR_TYPE, ATTR_TYPE, CDATA, t.getClass().getName());
			atts.addAttribute(null, ATTR_MESSAGE, ATTR_MESSAGE, CDATA, t.getMessage());
			handler.startElement(null, ELEM_FAILURE, ELEM_FAILURE, atts);
			
			handler.startCDATA();
			StringWriter tmp = new StringWriter();
			t.printStackTrace(new PrintWriter(tmp));
			char[] chars = tmp.toString().toCharArray();
			handler.characters(chars, 0, chars.length);
			handler.endCDATA();
			
			handler.endElement(null, ELEM_FAILURE, ELEM_FAILURE);
		} catch (SAXException e) {
			errors.add(e);
		}
	}

	public void endTest(Test test) {
		try {
			handler.endElement(null, ELEM_TEST, ELEM_TEST);
		} catch (SAXException e) {
			errors.add(e);
		}
	}

	public void startTest(Test test) {
        String nameAndClass = test.toString();
        String name = nameAndClass;
        String clazz = "";

        int n = nameAndClass.indexOf('(');
        if (n > 0 && nameAndClass.endsWith(")")) {
            name = nameAndClass.substring(0, n);
            clazz = nameAndClass.substring(n + 1, nameAndClass.length() - 1);
        }
        
        try {
			AttributesImpl atts = new AttributesImpl();
			atts.addAttribute(null, ATTR_TEST_NAME, ATTR_TEST_NAME, CDATA, name);
			atts.addAttribute(null, ATTR_TEST_CLASS, ATTR_TEST_CLASS, CDATA, clazz);
			handler.startElement(null, ELEM_TEST, ELEM_TEST, atts);
		} catch (SAXException e) {
			errors.add(e);
		}
	}

}
