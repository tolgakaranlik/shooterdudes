package com.tolka;

import java.util.Vector;

public class MotionTweenFactory {
	private class MotionTweenData
	{
		private long startedAt;
		private int delay;
		private int repeatInterval;
		private MotionTween tween;
		
		public static final int STATUS_WAITING = 0x0000;
		public static final int STATUS_PROCESSING = 0x0001;
		public static final int STATUS_REPEAT = 0x0003;
		private int state;
		
		public long getStartedAt()
		{
			return startedAt;
		}
		
		public int getDelay()
		{
			return delay;
		}
		
		public int getRepeatInterval()
		{
			return repeatInterval;
		}
		
		public MotionTween getTween()
		{
			return tween;
		}
		
		public void resetStartedAt()
		{
			startedAt = getClockTick();
		}
		
		public int getState()
		{
			return state;
		}
		
		public void setState(int state)
		{
			this.state = state;
		}
		
		public MotionTweenData(int delay, int repeatInterval, MotionTween tween)
		{
			this.state = STATUS_WAITING;
			this.startedAt = getClockTick();
			this.delay = delay;
			this.repeatInterval = repeatInterval;
			this.tween = tween;
		}
	}

	private Vector<MotionTweenData> tweens;
	private float delay = 1.0f;
	private long lastClockTick = -1;

	public static final int NO_REPEAT = -1;
	
	public MotionTweenFactory()
	{
		tweens = new Vector<MotionTweenData>();
	}
	
	public void stopAll()
	{
		tweens.clear();
	}
	
	public int add(MotionTween motionTween, int delay, int repeatInterval)
	{
		tweens.add(new MotionTweenData(delay, repeatInterval, motionTween));
		
		return tweens.size()- 1;
	}
	
	public void setTweenAt(int i, MotionTween motionTween, int delay, int repeatInterval)
	{
		tweens.set(i, new MotionTweenData(delay, repeatInterval, motionTween));
	}
	
	public void update(float delay)
	{
		this.delay = delay;
		
		for(int i = 0; i< tweens.size(); i++)
		{
			switch(tweens.get(i).getState())
			{
			case MotionTweenData.STATUS_WAITING:
				if(tweens.get(i).getStartedAt() + tweens.get(i).getDelay() <= getClockTick())
				{
					tweens.get(i).getTween().reset();
					tweens.get(i).setState(MotionTweenData.STATUS_PROCESSING);
				}

				break;
			case MotionTweenData.STATUS_PROCESSING:
				if(!tweens.get(i).getTween().update(delay))
				{
					tweens.get(i).getTween().reset();
					tweens.get(i).setState(MotionTweenData.STATUS_REPEAT);
					tweens.get(i).resetStartedAt();
				}

				break;
			case MotionTweenData.STATUS_REPEAT:
				if(tweens.get(i).getRepeatInterval() != -1)
				{
					if(tweens.get(i).getStartedAt() + tweens.get(i).getRepeatInterval() <= getClockTick())
					{
						tweens.get(i).getTween().reset();
						tweens.get(i).setState(MotionTweenData.STATUS_PROCESSING);
						tweens.get(i).resetStartedAt();
					}
	
					break;
				}
			}
		}
	}
	
	private long getClockTick()
	{
		long m = System.currentTimeMillis();
		if(lastClockTick == -1)
		{
			lastClockTick = m;
		}
		
		lastClockTick += (long)((m - lastClockTick) / delay);
		return lastClockTick;
	}
}
