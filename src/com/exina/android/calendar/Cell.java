package com.exina.android.calendar;

import java.util.Calendar;

import org.bob.school.tools.CalendarTools;

public class Cell {
	private int mDayOfMonth;	// from 1 to 31
	private int mMonth;
	private int mYear;
	private boolean mMark = false;

	public Cell(int dayOfMonth, int month, int year) {
		mDayOfMonth = dayOfMonth;
		mMonth = month;
		mYear = year;
	}

	public int getDayOfMonth() {
		return mDayOfMonth;
	}

	public int getMonth() {
		return mMonth;
	}

	public int getYear() {
		return mYear;
	}

	public void setMark(boolean mark) {
		mMark = mark;
	}

	public boolean isMarked() {
		return mMark;
	}

	public Calendar getDate() {
		Calendar c = Calendar.getInstance();
		c.set(mYear, mMonth, mDayOfMonth);
		CalendarTools.resetTime(c);

		return c; 
	}
}