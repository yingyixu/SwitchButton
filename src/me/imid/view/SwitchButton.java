package me.imid.view;

import me.imid.movablecheckbox.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.CheckBox;

public class SwitchButton extends CheckBox {

	private Paint mPaint;
	private ViewParent mParent;
	private PorterDuffXfermode porterDuffXfermode;
	private Bitmap bottom;
	private Bitmap btn;
	private Bitmap pressedBtn;
	private Bitmap normalBtn;
	private Bitmap frame;
	private Bitmap mask;

	private float downY; // 首次按下的Y
	private float downX; // 首次按下的X
	private float btnTopLeftX; // btn的左上角x
	private float btnCurX; // 按钮当前所在x
	private float btnLeftX; // 开关打开时btn所处的x
	private float btnRightX; // 开关关闭时btn所处的x
	private final float expandY = 15; // Y轴方向扩大触控有效区
	private float maskWidth;
	private float maskHeight;
	private float btnWidth;
	private float btnStartX;

	private int mClickTimeout;
	private int mTouchSlop;
	private int mAlpha = 255;

	private boolean mChecked = false;
	private boolean isBroadcasting = false;
	private boolean isTurningOff = false;

	private PerformClickTask mPerformClickTask;
	private OnCheckedChangeListener mOnCheckedChangeListener;

	public SwitchButton(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.checkboxStyle);
	}

	public SwitchButton(Context context) {
		this(context, null);
	}

	public SwitchButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView(context);
	}

	private void initView(Context context) {

		mPaint = new Paint();
		porterDuffXfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

		// get viewConfiguration
		mClickTimeout = ViewConfiguration.getPressedStateDuration()
				+ ViewConfiguration.getTapTimeout();
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

		// get Bitmap
		Resources resources = context.getResources();
		bottom = BitmapFactory.decodeResource(resources, R.drawable.bottom);
		pressedBtn = BitmapFactory.decodeResource(resources,
				R.drawable.btn_pressed);
		normalBtn = BitmapFactory.decodeResource(resources,
				R.drawable.btn_unpressed);
		frame = BitmapFactory.decodeResource(resources, R.drawable.frame);
		mask = BitmapFactory.decodeResource(resources, R.drawable.mask);
		btn = normalBtn;

		btnWidth = pressedBtn.getWidth();
		maskWidth = mask.getWidth();
		maskHeight = mask.getHeight();

		btnRightX = btnWidth / 2;
		btnLeftX = maskWidth - btnWidth / 2;

		btnCurX = mChecked ? btnLeftX : btnRightX;
		btnTopLeftX = getBtnTopLeftX(btnCurX);
	}

	@Override
	public void setEnabled(boolean enabled) {
		mAlpha = enabled ? 255 : 128;
		super.setEnabled(enabled);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

	/**
	 * 内部调用此方法设置checked状态，此方法会延迟执行各种回调函数，保证动画的流畅度
	 * 
	 * @param checked
	 */
	private void setCheckedDelayed(final boolean checked) {
		postDelayed(new Runnable() {

			@Override
			public void run() {
				setChecked(checked);
			}
		}, 10);
	}

	/**
	 * <p>
	 * Changes the checked state of this button.
	 * </p>
	 * 
	 * @param checked
	 *            true to check the button, false to uncheck it
	 */
	@Override
	public void setChecked(boolean checked) {

		if (mChecked == checked) {
			return;
		}

		mChecked = checked;
		btnCurX = checked ? btnLeftX : btnRightX;
		btnTopLeftX = getBtnTopLeftX(btnCurX);
		invalidate();

		// Avoid infinite recursions if setChecked() is called from a listener
		if (isBroadcasting) {
			return;
		}

		isBroadcasting = true;
		if (mOnCheckedChangeListener != null) {
			mOnCheckedChangeListener.onCheckedChanged(SwitchButton.this,
					mChecked);
		}
		isBroadcasting = false;
	}

	/**
	 * Register a callback to be invoked when the checked state of this button
	 * changes.
	 * 
	 * @param listener
	 *            the callback to call on checked state change
	 */
	@Override
	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mOnCheckedChangeListener = listener;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float x = event.getX();
		float y = event.getY();
		final float deltaX = Math.abs(x - downX);
		final float deltaY = Math.abs(y - downY);

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			attemptClaimDrag();
			downX = x;
			downY = y;
			btn = pressedBtn;
			btnStartX = mChecked ? btnLeftX : btnRightX;
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			btnCurX = btnStartX + event.getX() - downX;
			if (btnCurX >= btnRightX) {
				btnCurX = btnRightX;
			}
			if (btnCurX <= btnLeftX) {
				btnCurX = btnLeftX;
			}
			isTurningOff = btnCurX > ((btnRightX - btnLeftX) / 2 + btnLeftX);
			btnTopLeftX = getBtnTopLeftX(btnCurX);
			break;
		}
		case MotionEvent.ACTION_UP: {
			btn = normalBtn;
			final float time = event.getEventTime() - event.getDownTime();
			if (deltaY < mTouchSlop && deltaX < mTouchSlop
					&& time < mClickTimeout) { // click
				if (mPerformClickTask == null) {
					mPerformClickTask = new PerformClickTask();
				}
				if (!post(mPerformClickTask)) {
					performClick();
				}
			} else {
				btnAnimation.start(!isTurningOff);
			}
			break;
		}
		}

		invalidate();
		return isEnabled();
	}

	private final class PerformClickTask implements Runnable {
		public void run() {
			performClick();
		}
	}

	@Override
	public boolean performClick() {
		btnAnimation.start(!mChecked);
		return true;
	}

	/**
	 * Tries to claim the user's drag motion, and requests disallowing any
	 * ancestors from stealing events in the drag.
	 */
	private void attemptClaimDrag() {
		mParent = getParent();
		if (mParent != null) {
			mParent.requestDisallowInterceptTouchEvent(true);
		}
	}

	private float getBtnTopLeftX(float btnX) {
		return btnX - btnWidth / 2;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.saveLayerAlpha(
				new RectF(0, expandY, mask.getWidth(), mask.getHeight()
						+ expandY), mAlpha, Canvas.MATRIX_SAVE_FLAG
						| Canvas.CLIP_SAVE_FLAG
						| Canvas.HAS_ALPHA_LAYER_SAVE_FLAG
						| Canvas.FULL_COLOR_LAYER_SAVE_FLAG
						| Canvas.CLIP_TO_LAYER_SAVE_FLAG);

		// 绘制蒙板
		canvas.drawBitmap(mask, 0, expandY, mPaint);
		mPaint.setXfermode(porterDuffXfermode);

		// 绘制底部图片
		canvas.drawBitmap(bottom, btnTopLeftX, expandY, mPaint);
		mPaint.setXfermode(null);

		// 绘制边框
		canvas.drawBitmap(frame, 0, expandY, mPaint);

		// 绘制按钮
		canvas.drawBitmap(btn, btnTopLeftX, expandY, mPaint);
		canvas.restore();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension((int) maskWidth, (int) (maskHeight + 2 * expandY));
	}

	// animation
	private BtnAnimation btnAnimation = new BtnAnimation();

	private class BtnAnimation {

		private static final int MSG_ANIMATE = 1000;
		private static final int ANIMATION_FRAME_DURATION = 1000 / 60;
		private final float INIT_VELOCITY = 400;

		private boolean mAnimating = false;
		private final Handler mHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_ANIMATE:
					doAnimation();
					break;
				}
			};
		};

		private float mAnimationLastTime;
		private float mAnimationPosition;
		private float mAnimatedVelocity;
		private float mAnimatedAcceleration;
		private long mCurrentAnimationTime;

		private void increment() {
			long now = SystemClock.elapsedRealtime();
			float t = (now - mAnimationLastTime) / 1000.0f; // ms -> s
			final float position = mAnimationPosition;
			final float v = mAnimatedVelocity; // px/s
			final float a = mAnimatedAcceleration; // px/s/s
			mAnimationPosition = position + (v * t) + (0.5f * a * t * t);
			mAnimatedVelocity = v + (a * t); // px/s
			mAnimationLastTime = now; // ms
		}

		public void start(boolean doTurnOn) {
			long now = SystemClock.uptimeMillis();
			mAnimationLastTime = now;
			mAnimatedVelocity = doTurnOn ? -INIT_VELOCITY : INIT_VELOCITY;
			mAnimationPosition = btnCurX;
			mCurrentAnimationTime = now + ANIMATION_FRAME_DURATION;
			mAnimating = true;

			mHandler.removeMessages(MSG_ANIMATE);
			mHandler.sendMessageAtTime(mHandler.obtainMessage(MSG_ANIMATE),
					mCurrentAnimationTime);
		}

		private void doAnimation() {
			if (mAnimating) {
				increment();
				if (mAnimationPosition <= btnLeftX) {
					mAnimating = false;
					mAnimationPosition = btnLeftX;
					setCheckedDelayed(true);
				} else if (mAnimationPosition >= btnRightX) {
					mAnimating = false;
					mAnimationPosition = btnRightX;
					setCheckedDelayed(false);
				} else {
					mCurrentAnimationTime += ANIMATION_FRAME_DURATION;
					mHandler.sendMessageAtTime(
							mHandler.obtainMessage(MSG_ANIMATE),
							mCurrentAnimationTime);
				}
				invalidateView(mAnimationPosition);
			}
		}

		private void invalidateView(float position) {
			btnCurX = position;
			btnTopLeftX = getBtnTopLeftX(btnCurX);
			invalidate();
		}

	}
}