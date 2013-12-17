package org.jbpm.util;

public class DateDbTestUtil {
	private static DateDbTestUtil instance;

	private DateDbTestUtil() {}

	public static DateDbTestUtil getInstance() {
		if(instance == null) {
			instance = new DateDbTestUtil();
		}
		return instance;
	}

	/**
	 * This method returns the number of seconds by absolutely ignoring the
	 * milliseconds. No ceiling or flooring is done.
	 * 
	 * @param date
	 * @return Date converted to seconds.
	 */
	public long convertDateToSeconds(java.util.Date date) {
		return ((date.getTime()) - (date.getTime() % 1000)) / 1000;
	}

}
