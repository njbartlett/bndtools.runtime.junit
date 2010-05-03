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

import junit.framework.Test;
import junit.framework.TestResult;

class InvalidTest implements Test {
	
	private final String name;
	private final Throwable error;
	
	InvalidTest(String name, Throwable error) {
		this.name = name;
		this.error = error;
	}
	public int countTestCases() {
		return 1;
	}
	public void run(TestResult result) {
		result.startTest(this);
		result.addError(this, error);
		result.endTest(this);
	}
	@Override
	public String toString() {
		return name;
	}
}
