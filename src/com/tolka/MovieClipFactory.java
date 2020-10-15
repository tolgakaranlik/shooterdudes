package com.tolka;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

public class MovieClipFactory {
	public class MovieClipInfo
	{
		public MovieClip clip;
		public static final int STATUS_STOPED = 0x0000;
		public static final int STATUS_RUNNING = 0x0001;
		private int state;
		private String id;
		private long curFrame;
		private long lastProcessedFrame;
		private RunAfterFunction runAfter;
		
		public int getState()
		{
			return state;
		}
		
		public long getCurrentFrame()
		{
			return curFrame;
		}
		
		public void setCurrentFrame(long curFrame)
		{
			this.curFrame = curFrame;
		}
		
		public long getLastProcessedFrame()
		{
			return lastProcessedFrame;
		}
		
		public void setLastProcessedFrame(int lastProcessedFrame)
		{
			this.lastProcessedFrame = lastProcessedFrame;
		}
		
		public void setState(int state)
		{
			this.state = state;
		}
		
		public MovieClip getClip()
		{
			return clip;
		}
		
		public String getId()
		{
			return id;
		}
		
		public void setRunAfter(RunAfterFunction runAfter)
		{
			this.runAfter = runAfter;
		}
		
		public MovieClipInfo(String id, MovieClip clip)
		{
			state = STATUS_STOPED;
			curFrame = 0;
			lastProcessedFrame = -1;
			this.clip = clip;
			this.id = id;
			this.runAfter = null;
		}

		public void setPosition(int x, int y) {
			clip.setPosition(x, y);
		}
	}
	
	private long lastClockTick;
	private String debugData = "";
	public Vector<MovieClipInfo> movieClips;
	
	public String getDebugData()
	{
		return debugData;
	}
	
	public MovieClipFactory()
	{
		movieClips = new Vector<MovieClipInfo>();
	}
	
	public void runAfterPlay(String id, RunAfterFunction f)
	{
		for(int i = 0; i< movieClips.size(); i++)
		{
			if(movieClips.get(i).id.equals(id))
			{
				movieClips.get(i).setRunAfter(f);
				break;
			}
		}
	}
	
	public void play(String id)
	{
		lastClockTick = getClockTick();

		for(int i = 0; i< movieClips.size(); i++)
		{
			if(movieClips.get(i).getId().equals(id))
			{
				movieClips.get(i).setCurrentFrame(0);
				movieClips.get(i).setState(MovieClipInfo.STATUS_RUNNING);
				break;
			}
		}
	}
	
	public void setPosition(String id, int x, int y)
	{
		for(int i = 0; i< movieClips.size(); i++)
		{
			if(movieClips.get(i).id.equals(id))
			{
				movieClips.get(i).setPosition(x, y);
				break;
			}
		}
	}
	
	public void stop(String id)
	{
		for(int i = 0; i< movieClips.size(); i++)
		{
			if(movieClips.get(i).id.equals(id))
			{
				movieClips.get(i).setState(MovieClipInfo.STATUS_STOPED);
				break;
			}
		}
	}
	
	public void stopAll()
	{
		for(int i = 0; i< movieClips.size(); i++)
		{
			movieClips.get(i).setState(MovieClipInfo.STATUS_STOPED);
		}
	}
	
	public void update(float delay) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		for(int i = 0; i< movieClips.size(); i++)
		{
			MovieClip clip = movieClips.get(i).getClip();
			if(clip == null || movieClips.get(i).getState() == MovieClipInfo.STATUS_STOPED)
			{
				// whatta?
				continue;
			}
			
			long t = getClockTick();
			long diff = (long)((t- lastClockTick) / delay);
			long curFrame = movieClips.get(i).getCurrentFrame()+ diff;
			if(curFrame >= clip.getSpriteFactory().sprites.size()* clip.getDelay())
			{
				if(clip.getRepetition() > 0 && clip.getCurRepetition() >= clip.getRepetition())
				{
					debugData += "stoped\n";
					clip.getSpriteFactory().invisibleAll();

					clip.setCurRepetition(0);
					movieClips.get(i).setState(MovieClipInfo.STATUS_STOPED);
					if(movieClips.get(i).runAfter != null)
					{
						movieClips.get(i).runAfter.run();
					}
				} else {
					debugData += "rewind ("+ clip.getCurRepetition() +" of "+ clip.getRepetition() +")\n";
					clip.setCurRepetition(clip.getCurRepetition()+ 1);
					curFrame %= clip.getSpriteFactory().sprites.size()* clip.getDelay();
					//clip.getSpriteFactory().invisibleAll();
				}
			}
			else
			{
				int id = (int)Math.floor(curFrame/ clip.getDelay());

				clip.getSpriteFactory().invisibleAll();
				clip.getSpriteFactory().setVisible(id, true);
			}

			movieClips.get(i).setCurrentFrame(curFrame);
			clip.getSpriteFactory().draw();
		}
		
		lastClockTick = getClockTick();
	}
	
	public boolean add(String id, MovieClip clip)
	{
		for(int i = 0; i< movieClips.size(); i++)
		{
			if(movieClips.get(i).getId().equals(id))
			{
				movieClips.get(i).clip = clip;
				return false;
			}
		}

		movieClips.add(new MovieClipInfo(id, clip));
		return true;
	}
	
	private long getClockTick()
	{
		return System.currentTimeMillis();
		/*
		long m = System.currentTimeMillis();
		if(mClockTick == -1)
		{
			mClockTick = m;
		}
		
		return mClockTick + (long)((m - mClockTick) / delay);*/
	}
}
