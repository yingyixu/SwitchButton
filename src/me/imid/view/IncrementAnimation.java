package me.imid.view;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

public abstract class IncrementAnimation {
	protected static final int MSG_ANIMATE = 1000;
	protected static final int ANIMATION_FRAME_DURATION = 1000 / 60;
	protected boolean mAnimating = false;
	protected final Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_ANIMATE:
				doAnimation();
				break;
			}
		};
	};
	protected float mAnimationLastTime;
	protected float mAnimationPosition;
	protected float mAnimatedVelocity;
	protected float mAnimatedAcceleration;
	protected long mCurrentAnimationTime;

	public void setmAnimating(boolean b) {
		mAnimating = b;
	}

	public boolean getmAnimating() {
		return mAnimating;
	}

	protected abstract void doAnimation();

	protected void incrementAnimation() {
		long now = SystemClock.elapsedRealtime();;
		float t = (now - mAnimationLastTime) / 1000.0f; // ms -> s
		final float position = mAnimationPosition;
		final float v = mAnimatedVelocity; // px/s
		final float a = mAnimatedAcceleration; // px/s/s
		mAnimationPosition = position + (v * t) + (0.5f * a * t * t);
		mAnimatedVelocity = v + (a * t); // px/s
		mAnimationLastTime = now; // ms
	}

	protected abstract void moveView(float position);
}
