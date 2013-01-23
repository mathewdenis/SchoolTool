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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class CellView {
	private static final String TAG = "CellView";
	private Cell mCellModel;
	protected Rect mBound = null;
	protected Paint mPaint = new Paint(Paint.SUBPIXEL_TEXT_FLAG
            |Paint.ANTI_ALIAS_FLAG);
	int dx, dy;

	public CellView(int dayOfMon, int month, int year, Rect rect, float textSize, boolean bold) {
		mCellModel = new Cell(dayOfMon, month, year);
		mBound = rect;
		mPaint.setTextSize(textSize/*26f*/);
		mPaint.setColor(Color.BLACK);
		if(bold) mPaint.setFakeBoldText(true);
		
		dx = (int) mPaint.measureText(String.valueOf(mCellModel.getDayOfMonth())) / 2;
		dy = (int) (-mPaint.ascent() + mPaint.descent()) / 2;
	}
	
	public CellView(int dayOfMon, int month, int year, Rect rect, float textSize) {
		this(dayOfMon, month, year, rect, textSize, false);
	}
	
	protected void draw(Canvas canvas) {
		Paint backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.LTGRAY);
		backgroundPaint.setAlpha(128);
		if(mCellModel.isMarked())
			canvas.drawRect(mBound, backgroundPaint);
		canvas.drawText(String.valueOf(mCellModel.getDayOfMonth()),
				mBound.centerX() - dx, mBound.centerY() + dy, mPaint);
		
	}
	
	public boolean hitTest(int x, int y) {
		return mBound.contains(x, y); 
	}
	
	public Rect getBound() {
		return mBound;
	}

	public Cell getModel() {
		return mCellModel;
	}

	public String toString() {
		return String.valueOf(mCellModel.getDayOfMonth())+"("+mBound.toString()+")";
	}
	
}

