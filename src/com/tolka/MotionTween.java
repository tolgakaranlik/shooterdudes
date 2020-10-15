package com.tolka;

public class MotionTween {
	public final static int ATTR_X = 0x9001;
	public final static int ATTR_Y = 0x9002;
	public final static int ATTR_ALPHA = 0x9003;
	public final static int ATTR_SCALE = 0x9004;
	public final static int ATTR_ROTATION = 0x9005;
	
	private int attr;
	private double first;
	private double last;
	private double duration;
	private Sprite sprite;
	private long startedAt; 
	private float delay = 1.0f;
	private long lastClockTick = -1;

	public void reset()
	{
		this.startedAt = getClockTick();
	}
	
	public MotionTween(int attr, Sprite sprite, double first, double last, double duration)
	{
		this.attr = attr;
		this.sprite = sprite;
		this.first = first;
		this.last = last;
		this.duration = duration;
		
		this.startedAt = getClockTick();
	}
	
	public Boolean update(float delay)
	{
		this.delay = delay;
		
		// returns true when action is going on
		long milisec = getClockTick();
		if(startedAt+ duration <= milisec)
		{
			return false;
		}
		
		switch(attr)
		{
			case ATTR_X:
				sprite.setPosition((int)(first + (last- first)* (milisec- startedAt)/ duration), sprite.getY());
				break;
			case ATTR_Y:
				sprite.setPosition(sprite.getX(), (int)(first + (last- first)* (milisec- startedAt)/ duration));
				break;
			case ATTR_ALPHA:
				sprite.setAlpha((float)(first + (last- first)* ((double)(milisec- startedAt))/ duration));
				break;
			case ATTR_SCALE:
				break;
			case ATTR_ROTATION:
				break;
			default:
				return false;
		}
		
		return true;
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
