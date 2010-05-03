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

public final class LauncherConstants {

	// NAMESPACE
	public static final String NAMESPACE            = "bndtools.runtime.junit";
	
	public static final String PROP_REPORTER        = NAMESPACE + ".reporter";
	public static final String PROP_KEEP_ALIVE      = NAMESPACE + ".keepAlive";
	public static final String PROP_START_TIMEOUT   = NAMESPACE + ".startTimeout";
	public static final String PROP_THREADPOOL_SIZE = NAMESPACE + ".threadPoolSize";
	
	private LauncherConstants() {} // Prevents instantiation
}
