package com.tolka;

public class MovieClip {
	private SpriteFactory sprites;
	private int delay;
	private int repetition;
	private int curRepetition;

	public MovieClip(int delay)
	{
		sprites = new SpriteFactory();
		this.delay = delay;
		this.repetition = -1;
		this.curRepetition = 1;
	}

	public int getDelay()
	{
		return delay;
	}

	public void setCurRepetition(int curRepetition)
	{
		this.curRepetition = curRepetition;
	}

	public int getCurRepetition()
	{
		return curRepetition;
	}

	public void setRepetition(int repetition)
	{
		this.repetition = repetition;
	}

	public int getRepetition()
	{
		return repetition;
	}

	public void setPosition(float x, float y)
	{
		for(int i = 0; i< sprites.sprites.size(); i++)
		{
			sprites.setPosition(i, x, y);
		}
	}
	
	public SpriteFactory getSpriteFactory()
	{
		return sprites;
	}
}
