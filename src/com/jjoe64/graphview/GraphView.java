package com.jjoe64.graphview;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.jjoe64.graphview.compatible.ScaleGestureDetector;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs.
 * This is the abstract base class for all graphs. Extend this class and implement {@link #drawData(Canvas, float, float, float, double, double, double, double, float)} to display a custom graph.
 * Use {@link LineGraphView} for creating a line chart.
 *
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 *
 * Copyright (C) 2011 Jonas Gehring
 * Licensed under the GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 */
abstract public class GraphView extends LinearLayout {
	static final private class GraphViewConfig {
		static final float BORDER = 20;
		static final float VERTICAL_LABEL_WIDTH = 100;
		static final float HORIZONTAL_LABEL_HEIGHT = 80;
	}

	private class GraphViewContentView extends View {
		private float lastTouchEventX;
		private float graphwidth;

		/**
		 * @param context
		 */
		public GraphViewContentView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			// normal
			paint.setStrokeWidth(0);

			float border = GraphViewConfig.BORDER;
			float horstart = 0;
			float height = getHeight();
			float width = getWidth() - 1;
			double maxY = getMaxY();
			double minY = getMinY();
			double diffY = maxY - minY;
			double maxX = getMaxX();
			double minX = getMinX();
			double diffX = maxX - minX;
			float graphheight = height - (2 * border);
			graphwidth = width;

			if (horlabels == null) {
				horlabels = generateHorlabels(graphwidth);
			}
			if (verlabels == null) {
				verlabels = generateVerlabels(graphheight);
			}

			// vertical lines
			paint.setTextAlign(Align.LEFT);
			int vers = verlabels.length - 1;
			for (int i = 0; i < verlabels.length; i++) {
				paint.setColor(Color.DKGRAY);
				float y = ((graphheight / vers) * i) + border;
				canvas.drawLine(horstart, y, width, y, paint);
			}

			// horizontal labels + lines
			int hors = horlabels.length - 1;
			for (int i = 0; i < horlabels.length; i++) {
				paint.setColor(Color.DKGRAY);
				float x = ((graphwidth / hors) * i) + horstart;
				canvas.drawLine(x, height - border, x, border, paint);
				paint.setTextAlign(Align.CENTER);
				if (i==horlabels.length-1)
					paint.setTextAlign(Align.RIGHT);
				if (i==0)
					paint.setTextAlign(Align.LEFT);
				paint.setColor(Color.WHITE);
				canvas.drawText(horlabels[i], x, height - 4, paint);
			}

			paint.setTextAlign(Align.CENTER);
			canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

			if (maxY != minY) {
				// blue version
				paint.setARGB(255, 0, 119, 204);
				paint.setStrokeCap(Paint.Cap.ROUND);
				paint.setStrokeWidth(3);

				drawData(canvas, graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart);
			}
		}

		private void onMoveGesture(float f) {
			// view port update
			if (viewportSize != 0) {
				viewportStart -= f*viewportSize/graphwidth;

				// minimal and maximal view limit
				if (viewportStart < values[0].valueX) {
					viewportStart = values[0].valueX;
				} else if (viewportStart+viewportSize > values[values.length -1].valueX) {
					viewportStart = values[values.length -1].valueX - viewportSize;
				}

				// labels have to be regenerated
				horlabels = null;
				verlabels = null;
				viewVerLabels.invalidate();
			}
			invalidate();
		}

		/**
		 * @param event
		 */
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (!isScrollable()) {
				return super.onTouchEvent(event);
			}

			boolean handled = false;
			// first scale
			if (scalable && scaleDetector != null) {
				scaleDetector.onTouchEvent(event);
				handled = scaleDetector.isInProgress();
			}
			if (!handled) {
				// if not scaled, scroll
				if ((event.getAction() & MotionEvent.ACTION_DOWN) == MotionEvent.ACTION_DOWN) {
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_UP) == MotionEvent.ACTION_UP) {
					lastTouchEventX = 0;
					handled = true;
				}
				if ((event.getAction() & MotionEvent.ACTION_MOVE) == MotionEvent.ACTION_MOVE) {
					if (lastTouchEventX != 0) {
						onMoveGesture(event.getX() - lastTouchEventX);
					}
					lastTouchEventX = event.getX();
					handled = true;
				}
			}
			return handled;
		}

		public boolean isUserInteracting() {
			return lastTouchEventX != 0;
		}
	}

	/**
	 * one data set for the graph
	 */
	static public class GraphViewData {
		double valueX;
		double valueY;
		public GraphViewData(double valueX, double valueY) {
			super();
			this.valueX = valueX;
			this.valueY = valueY;
		}
	}

	private class VerLabelsView extends View {
		/**
		 * @param context
		 */
		public VerLabelsView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 10));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			// normal
			paint.setStrokeWidth(0);

			float border = GraphViewConfig.BORDER;
			float height = getHeight();
			float graphheight = height - (2 * border);

			if (verlabels == null) {
				verlabels = generateVerlabels(graphheight);
			}

			// vertical labels
			paint.setTextAlign(Align.LEFT);
			int vers = verlabels.length - 1;
			for (int i = 0; i < verlabels.length; i++) {
				float y = ((graphheight / vers) * i) + border;
				paint.setColor(Color.WHITE);
				canvas.drawText(verlabels[i], 0, y, paint);
			}
		}
	}

	protected final Paint paint;
	private final GraphViewContentView contentView;
	private GraphViewData[] values;
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private boolean scrollable;
	private double viewportStart;
	private double viewportSize;
	private final View viewVerLabels;
	private ScaleGestureDetector scaleDetector;
	private boolean scalable;
	private NumberFormat numberformatter;

	/**
	 *
	 * @param context
	 * @param values <b>must be sorted by valueX ASC</b>
	 * @param title [optional]
	 * @param horlabels [optional] if null, labels were generated automatically
	 * @param verlabels [optional] if null, labels were generated automatically
	 */
	public GraphView(Context context, GraphViewData[] values, String title, String[] horlabels, String[] verlabels) {
		super(context);
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		if (values == null)
			values = new GraphViewData[0];
		else
			this.values = values;
		if (title == null)
			title = "";
		else
			this.title = title;

		this.horlabels = horlabels;
		this.verlabels = verlabels;

		paint = new Paint();

		viewVerLabels = new VerLabelsView(context);
		addView(viewVerLabels);

		contentView = new GraphViewContentView(context);
		addView(contentView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
	}

	protected GraphViewData[] _values() {
		if (viewportStart == 0 && viewportSize == 0) {
			// all data
			return values;
		} else {
			// viewport
			List<GraphViewData> listData = new ArrayList<GraphViewData>();
			for (int i=0; i<values.length; i++) {
				if (values[i].valueX >= viewportStart) {
					if (values[i].valueX > viewportStart+viewportSize) {
						listData.add(values[i]); // one more for nice scrolling
						break;
					} else {
						listData.add(values[i]);
					}
				} else {
					if (listData.isEmpty()) {
						listData.add(values[i]);
					}
					listData.set(0, values[i]); // one before, for nice scrolling
				}
			}
			return listData.toArray(new GraphViewData[listData.size()]);
		}
	}

	abstract public void drawData(Canvas canvas, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart);

	/**
	 * formats the label
	 * can be overwritten
	 * @param value x and y values
	 * @param isValueX if false, value y wants to be formatted
	 * @return value to display
	 */
	protected String formatLabel(double value, boolean isValueX) {
		if (numberformatter == null) {
			numberformatter = NumberFormat.getNumberInstance();
			double highestvalue = getMaxY();
			double lowestvalue = getMinY();
			if (highestvalue - lowestvalue < 0.1) {
				numberformatter.setMaximumFractionDigits(6);
			} else if (highestvalue - lowestvalue < 1) {
				numberformatter.setMaximumFractionDigits(4);
			} else if (highestvalue - lowestvalue < 20) {
				numberformatter.setMaximumFractionDigits(3);
			} else if (highestvalue - lowestvalue < 100) {
				numberformatter.setMaximumFractionDigits(1);
			} else {
				numberformatter.setMaximumFractionDigits(0);
			}
		}
		return numberformatter.format(value);
	}

	private String[] generateHorlabels(float graphwidth) {
		int numLabels = (int) (graphwidth/GraphViewConfig.VERTICAL_LABEL_WIDTH);
		String[] labels = new String[numLabels+1];
		double min = getMinX();
		double max = getMaxX();
		for (int i=0; i<=numLabels; i++) {
			labels[i] = formatLabel(min + ((max-min)*i/numLabels), true);
		}
		return labels;
	}

	synchronized private String[] generateVerlabels(float graphheight) {
		int numLabels = (int) (graphheight/GraphViewConfig.HORIZONTAL_LABEL_HEIGHT);
		String[] labels = new String[numLabels+1];
		double min = getMinY();
		double max = getMaxY();
		for (int i=0; i<=numLabels; i++) {
			labels[numLabels-i] = formatLabel(min + ((max-min)*i/numLabels), false);
		}
		return labels;
	}

	private double getMaxX() {
		// if viewport is set, use this
		if (viewportSize != 0) {
			return viewportStart+viewportSize;
		} else {
			// otherwise use the max x value
			// values must be sorted by x, so the last value has the largest X value
			return values[values.length-1].valueX;
		}
	}

	private double getMaxY() {
		GraphViewData[] values = _values();
		double largest = Integer.MIN_VALUE;
		for (int i = 0; i < values.length; i++)
			if (values[i].valueY > largest)
				largest = values[i].valueY;
		return largest;
	}

	private double getMinX() {
		// if viewport is set, use this
		if (viewportSize != 0) {
			return viewportStart;
		} else {
			// otherwise use the max x value
			// values must be sorted by x, so the first value has the smallest X value
			return _values()[0].valueX;
		}
	}

	private double getMinY() {
		GraphViewData[] values = _values();
		double smallest = Integer.MAX_VALUE;
		for (int i = 0; i < values.length; i++)
			if (values[i].valueY < smallest)
				smallest = values[i].valueY;
		return smallest;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	/**
	 * this forces scrollable = true
	 * @param scalable
	 */
	synchronized public void setScalable(boolean scalable) {
		this.scalable = scalable;
		if (scalable == true && scaleDetector == null) {
			scrollable = true; // automatically forces this
			scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				@Override
				public boolean onScale(ScaleGestureDetector detector) {
					double newSize = viewportSize*detector.getScaleFactor();
					double diff = newSize-viewportSize;
					viewportStart += diff/2;
					viewportSize -= diff;
					if (diff < 0) {
						// viewportStart must not be < minX
						if (viewportStart < values[0].valueX) {
							viewportStart = values[0].valueX;
						}

						// viewportStart + viewportSize must not be > maxX
						double overlap = viewportStart + viewportSize - values[values.length-1].valueX;
						if (overlap > 0) {
							// scroll left
							if (viewportStart-overlap > values[0].valueX) {
								viewportStart -= overlap;
							} else {
								// maximal scale
								viewportStart = values[0].valueX;
								viewportSize = values[values.length-1].valueX - viewportStart;
							}
						}
					}
					verlabels = null;
					horlabels = null;
					numberformatter = null;
					invalidate();
					viewVerLabels.invalidate();
					return true;
				}
			});
		}
	}

	/**
	 * the user can scroll (horizontal) the graph. This is only useful if you use a viewport {@link #setViewPort(double, double)} which doesn't displays all data.
	 * @param scrollable
	 */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	/**
	 * set's the viewport for the graph.
	 * @param start x-value
	 * @param size
	 */
	public void setViewPort(double start, double size) {
		viewportStart = start;
		viewportSize = size;

		//Force a new view port to regenerate the axis
		horlabels = null;
		verlabels = null;
	}

	public double getViewPortStart() {
		return viewportStart;
	}

	public double getViewPortSize() {
		return viewportSize;
	}

	public boolean isUserInteracting() {
		return contentView.isUserInteracting();
	}
}
