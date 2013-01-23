package com.exina.android.calendar;

import java.util.HashSet;
import java.util.Set;


public class CalendarAdapter {
    private Set<Long> mMarkedDates = new HashSet<Long>();

	public CalendarAdapter(Set<Long> markedDates) {
		mMarkedDates = markedDates;
	}

	public void setMarkedDates(Set<Long> markedDates) {
    	mMarkedDates = markedDates;
    }

    public void addMarkedDate(long markedDate) {
    	mMarkedDates.add(markedDate);
    }

    public boolean containsMarkedDate(long markedDate) {
    	return mMarkedDates.contains(markedDate);
    }
}
