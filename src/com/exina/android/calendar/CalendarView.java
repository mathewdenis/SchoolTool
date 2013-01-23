/*
 * Copyright (C) 2011 Chris Gao <chris@exina.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exina.android.calendar;

import java.util.Calendar;

import org.bob.school.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.MonthDisplayHelper;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class CalendarView extends ImageView {
    private static int WEEK_TOP_MARGIN = 0;
    private static int WEEK_LEFT_MARGIN = 0;
    private static int CELL_WIDTH = 57;
    private static int CELL_HEIGH = 53;
    private static int CELL_MARGIN_TOP = 22;
    private static int CELL_MARGIN_LEFT = 2;
    private static float CELL_TEXT_SIZE;
    
	@SuppressWarnings("unused")
	private static final String TAG = "CalendarView"; 
	private Calendar mToday = null;
	private Calendar mThisMonth = null;
    private Drawable mWeekTitle = null;
    private CellView mTodayCell = null;
    private CalendarAdapter mCalAdapter = null;
    private CellView[][] mCells = new CellView[6][7];
    private OnCellTouchListener mOnCellTouchListener = null;
    MonthDisplayHelper mHelper;
    Drawable mDecoration = null;

	private Animation mLInAni, mLOutAni, mRInAni, mROutAni;
    
	public interface OnCellTouchListener {
    	public void onTouch(Cell cell);
    }

	public CalendarView(Context context) {
		this(context, null);
	}
	
	public CalendarView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CalendarView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mDecoration = context.getResources().getDrawable(R.drawable.typeb_calendar_today);

		mLInAni = AnimationUtils.loadAnimation(getContext(), R.anim.push_left_in);
        mLOutAni = AnimationUtils.loadAnimation(getContext(), R.anim.push_left_out);
        mRInAni = AnimationUtils.loadAnimation(getContext(), R.anim.push_right_in);
        mROutAni = AnimationUtils.loadAnimation(getContext(), R.anim.push_right_out);

		initCalendarView();
	}
	
	private void initCalendarView() {
		mToday = Calendar.getInstance();
		mThisMonth = Calendar.getInstance();

		// prepare static vars
		Resources res = getResources();

		CELL_TEXT_SIZE = res.getDimension(R.dimen.cell_text_size);
		// set background
		setImageResource(R.drawable.cal_background);
		mWeekTitle = res.getDrawable(R.drawable.calendar_week);

		mHelper = new MonthDisplayHelper(mToday.get(Calendar.YEAR), mToday.get(Calendar.MONTH));
	}
	
	private void initCells() {
	    class _calendar {
	    	public int day;
	    	public boolean thisMonth;
	    	public _calendar(int d, boolean b) {
	    		day = d;
	    		thisMonth = b;
	    	}
	    };

		_calendar tmp[][] = new _calendar[6][7];
	    Cell c;
	    
	    for(int i=0; i<tmp.length; i++) {
	    	int n[] = mHelper.getDigitsForRow(i);
	    	for(int d=0; d<n.length; d++)
	    		tmp[i][d] = new _calendar(n[d], mHelper.isWithinCurrentMonth(i,d));
	    }

	    Calendar today = Calendar.getInstance();
	    int thisDay = 0;
		if (mHelper.getYear() == today.get(Calendar.YEAR)
				&& mHelper.getMonth() == today.get(Calendar.MONTH))
			thisDay = today.get(Calendar.DAY_OF_MONTH);

		mHelper.previousMonth();
		boolean n = false;
		// build cells
		mTodayCell = null;
		Rect Bound = new Rect(CELL_MARGIN_LEFT, CELL_MARGIN_TOP, CELL_WIDTH
				+ CELL_MARGIN_LEFT, CELL_HEIGH + CELL_MARGIN_TOP);
		for (int week = 0; week < mCells.length; week++) {
			for (int day = 0; day < mCells[week].length; day++) {
				if (tmp[week][day].thisMonth) {
					if (!n) {
						n = true;
						mHelper.nextMonth();
					}
					if (day == 0 || day == 6)
						mCells[week][day] = new RedCell(tmp[week][day].day,
								mHelper.getMonth(), mHelper.getYear(),
								new Rect(Bound), CELL_TEXT_SIZE);
					else
						mCells[week][day] = new CellView(tmp[week][day].day,
								mHelper.getMonth(), mHelper.getYear(),
								new Rect(Bound), CELL_TEXT_SIZE);
				} else {
					if (n) {
						n = false;
						mHelper.nextMonth();
					}
					mCells[week][day] = new GrayCell(tmp[week][day].day,
							mHelper.getMonth(), mHelper.getYear(), new Rect(
									Bound), CELL_TEXT_SIZE);
				}

				Bound.offset(CELL_WIDTH + 1, 0); // move to next column

				if(mCalAdapter != null) {
					// set mark if date is in marker-set
					c = mCells[week][day].getModel();
 					c.setMark(mCalAdapter.containsMarkedDate(c.getDate()
 							.getTimeInMillis()));
				}

				// get today
				if (tmp[week][day].day == thisDay && tmp[week][day].thisMonth) {
					mTodayCell = mCells[week][day];
					mDecoration.setBounds(mTodayCell.getBound());
				}
			}

			Bound.offset(0, CELL_HEIGH); // move to next row and first column
			Bound.left = CELL_MARGIN_LEFT;
			Bound.right = CELL_MARGIN_LEFT+CELL_WIDTH;
		}		

		if(!n)
			mHelper.previousMonth();
	}
	
	@Override
	public void onLayout(boolean changed, int left, int top, int right, int bottom) {
		mWeekTitle.setBounds(WEEK_LEFT_MARGIN, WEEK_TOP_MARGIN, WEEK_LEFT_MARGIN+mWeekTitle.getMinimumWidth(), WEEK_TOP_MARGIN+mWeekTitle.getMinimumHeight());
		initCells();
		super.onLayout(changed, left, top, right, bottom);
	}
	
    public void setTimeInMillis(long milliseconds) {
    	mToday.setTimeInMillis(milliseconds);
    	initCells();
    	this.invalidate();
    }
        
    public int getYear() {
    	return mHelper.getYear();
    }
    
    public int getMonth() {
    	return mHelper.getMonth();
    }
    
    private void nextMonth() {
    	mHelper.nextMonth();
    	mThisMonth.add(Calendar.MONTH, 1);
    }
    
    private void previousMonth() {
    	mHelper.previousMonth();
    	mThisMonth.add(Calendar.MONTH, -1);
    }

    private void redraw() {
    	initCells();
    	invalidate();
    }

    public boolean lastDay(int day) {
    	return mHelper.getNumberOfDaysInMonth()==day;
    }
    
    public void goToday() {
    	mHelper = new MonthDisplayHelper(mToday.get(Calendar.YEAR), mToday.get(Calendar.MONTH));
    	mThisMonth = Calendar.getInstance();
    	initCells();
    	invalidate();
    }
    
    /** Get a date in the currently displayed month.
     * @return A calendar object of a date in the currently displayed month
     */
    public Calendar getDate() {
    	return mThisMonth;
    }
    

    public boolean handleTouchEvent(MotionEvent event) {
    	boolean consumed = false;
    	if(mOnCellTouchListener!=null){
	    	for(CellView[] week : mCells) {
				for(CellView day : week) {
					if(day.hitTest((int)event.getX(), (int)event.getY())) {
						mOnCellTouchListener.onTouch(day.getModel());
						consumed = true;
					}						
				}
			}
    	}
    	if(consumed)
    		postInvalidate();

    	return consumed;
    }

    public void setOnCellTouchListener(OnCellTouchListener p) {
		mOnCellTouchListener = p;
	}

    public void setAdapter(CalendarAdapter calAdapter) {
    	mCalAdapter = calAdapter;
    }

    public CalendarAdapter getAdapter() {
    	return mCalAdapter;
    }

	@Override
	protected void onDraw(Canvas canvas) {
		// draw background
		super.onDraw(canvas);
		mWeekTitle.draw(canvas);
		
		// draw cells
		for(CellView[] week : mCells) {
			for(CellView day : week) {
				day.draw(canvas);			
			}
		}
		
		// draw today
		if(mDecoration!=null && mTodayCell!=null) {
			mDecoration.draw(canvas);
		}
	}
	
	public class GrayCell extends CellView {
		public GrayCell(int dayOfMon, int month, int year, Rect rect, float s) {
			super(dayOfMon, month, year, rect, s);
			mPaint.setColor(Color.LTGRAY);
		}			
	}
	
	private class RedCell extends CellView {
		public RedCell(int dayOfMon, int month, int year, Rect rect, float s) {
			super(dayOfMon, month, year, rect, s);
			mPaint.setColor(0xdddd0000);
		}			
		
	}

	/** Change the month by a slide animation
	 * @param increment Increment the month iff true
	 */
	public void changeMonth(final boolean increment) {
		if(increment) {
			nextMonth();
			startAnimation(mLOutAni);
		}
		else {
    		previousMonth();
			startAnimation(mROutAni);
		}

		// wait for animation to finish, then execute the rest
		postDelayed(new Runnable() {
		    @Override
		    public void run() {
				redraw();
		    	if(increment) {
					startAnimation(mLInAni);
		    	}
				else {
					startAnimation(mRInAni);
				}
		    }
		}, getResources().getInteger(R.integer.calendarAnimationTime));
	}


}