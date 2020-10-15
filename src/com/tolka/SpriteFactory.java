package com.tolka;

import java.io.IOException;
import java.util.Vector;

import android.graphics.Bitmap;

public class SpriteFactory {
	public static class SpriteData {
		private Sprite sprite;
		private String id;
		private Boolean visible;
		
		public Sprite getSprite()
		{
			return sprite;
		}
		
		public String getId()
		{
			return id;
		}
		
		public Boolean getVisible()
		{
			return visible;
		}
		
		public void setVisible(Boolean visible)
		{
			this.visible = visible;
		}
		
		public SpriteData(Sprite sprite, String id)
		{
			this.sprite = sprite;
			this.visible = true;
			this.id = id;
		}

		public void setSprite(Sprite sprite) {
			this.sprite = sprite;
		}
	}
	
	public Vector<SpriteData> sprites;
	
	public SpriteFactory()
	{
		sprites = new Vector<SpriteData>();
	}
	
	public void invisibleAll()
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			sprites.get(i).setVisible(false);
		}
	}
	
	public void setPosition(String id, float x, float y)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				sprites.get(i).getSprite().setPosition((int)x, (int)y);
				break;
			}
		}
	}
	
	public void setSprite(String id, Sprite sprite)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				sprites.get(i).setSprite(sprite);
				break;
			}
		}
	}
	
	public Bitmap getBitmapFromSprite(String id)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				try {
					return sprites.get(i).getSprite().getBitmap();
				} catch (IOException e) {
					return null;
				}
			}
		}
		
		return null;
	}
	
	public void setWidth(String id, int width)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				sprites.get(i).getSprite().setWidth(width);
				break;
			}
		}
	}
	
	public void setAlpha(String id, float alpha)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				sprites.get(i).getSprite().setAlpha(alpha);
				break;
			}
		}
	}
	
	public void setScale(String id, float scale)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				sprites.get(i).getSprite().setScale(scale);
				break;
			}
		}
	}
	
	public void setPosition(int i, float x, float y)
	{
		sprites.get(i).getSprite().setPosition((int)x, (int)y);
	}

	public void setVisible(String id, Boolean visible)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				sprites.get(i).setVisible(visible);
				break;
			}
		}
	}
	public int getX(String id)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				return sprites.get(i).getSprite().getX();
			}
		}
		
		return 0;
	}
	public int getY(String id)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				return sprites.get(i).getSprite().getY();
			}
		}
		
		return 0;
	}
	public void setVisible(int order, Boolean visible)
	{
		sprites.get(order).setVisible(visible);
	}
	public Boolean add(String id, Sprite sprite)
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				return false;
			}
		}
		
		sprites.add(new SpriteData(sprite, id));
		return true;
	}
	public void draw()
	{
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getVisible())
			{
				sprites.get(i).getSprite().draw();
			}
		}
	}

	public boolean getVisible(String id) {
		for(int i = 0; i< sprites.size(); i++)
		{
			if(sprites.get(i).getId().equals(id))
			{
				return sprites.get(i).getVisible();
			}
		}
		
		return false;
	}
}
