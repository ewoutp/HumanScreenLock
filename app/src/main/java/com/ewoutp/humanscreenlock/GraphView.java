package com.ewoutp.humanscreenlock;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GraphView extends View {

	private Bitmap bitmap;
	private final Canvas bmCanvas = new Canvas();
	private final Paint paint = new Paint();
	private final DashPathEffect dash = new DashPathEffect(new float[] { 4, 8, 4, 8 }, 0);
	
	public GraphView(Context context) {
		super(context);
	}
	
	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		bitmap = Bitmap.createBitmap(w, h, Config.RGB_565);
		bmCanvas.setBitmap(bitmap);		
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		if (bitmap == null)
			return;
			
		List<Double> listX = LockService.listX;
		List<Double> listY = LockService.listY;
		List<Double> listZ = LockService.listZ;
		List<Double> listF = LockService.listF;
		
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		float middle = height / 2;
		float stepX = (float)width / LockService.listLength; 
		double max = 1.0;
			
		int sz = listF.size();
		for (int i = 0; i < sz; i++) {
			max = Math.max(max, Math.abs(listX.get(i)));
			max = Math.max(max, Math.abs(listY.get(i)));
			max = Math.max(max, Math.abs(listZ.get(i)));
			max = Math.max(max, Math.abs(listF.get(i)));
		}
		max = Math.ceil(max);
		max = max * 1.1;

		float scY = (float) -(middle / max);
		bmCanvas.setMatrix(null);
		bmCanvas.translate(0, middle);
		bmCanvas.scale(1.0f, scY);
		
		Paint p = paint;
		p.setColor(Color.GRAY);
		p.setAntiAlias(true);
		p.setStrokeWidth(0);
		p.setStyle(Paint.Style.STROKE);
		// Fill background
		bmCanvas.drawColor(0xFFFFFFFF);
		// Draw y=0 line
		bmCanvas.drawLine(0, 0, width, 0, p);
		// Draw y=... lines
		for (int y = 1; y <= max; y++) {
			bmCanvas.drawLine(0, y, width, y, p);			
			bmCanvas.drawLine(0, -y, width, -y, p);			
		}
		// Draw force threshold line
		p.setColor(Color.RED);
		p.setPathEffect(dash);
		bmCanvas.drawLine(0, LockService.forceThreshold, width, LockService.forceThreshold, p);
		p.setPathEffect(null);

		for (int i = 0; i < sz; i++) {
			int prevI = Math.max(0, i-1);
			float prevX = prevI * stepX;
			float curX = i * stepX;
			p.setStrokeWidth(0);
			p.setColor(Color.BLUE);
			bmCanvas.drawLine(prevX, listX.get(prevI).floatValue(), curX, listX.get(i).floatValue(), p);
			p.setColor(Color.RED);
			bmCanvas.drawLine(prevX, listY.get(prevI).floatValue(), curX, listY.get(i).floatValue(), p);
			p.setColor(Color.GREEN);
			bmCanvas.drawLine(prevX, listZ.get(prevI).floatValue(), curX, listZ.get(i).floatValue(), p);
			p.setColor(Color.BLACK);
			p.setStrokeWidth(0.5f / scY);
			bmCanvas.drawLine(prevX, listF.get(prevI).floatValue(), curX, listF.get(i).floatValue(), p);
		}
		
		canvas.drawBitmap(bitmap, 0, 0, p);				
	}
}
