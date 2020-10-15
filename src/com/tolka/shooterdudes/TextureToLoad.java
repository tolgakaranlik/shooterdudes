package com.tolka.shooterdudes;

public class TextureToLoad {
	protected int layer;
	protected String id;
	protected String fileName;
	protected int width;
	protected int height;
	protected int posx;
	protected int posy;
	protected boolean visible;

	public int getLayer()
	{
		return layer;
	}
	
	public String getId()
	{
		return id;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public int getPosX()
	{
		return posx;
	}
	
	public int getPosY()
	{
		return posy;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public boolean getVisible()
	{
		return visible;
	}
	
	public TextureToLoad(int layer, String id, String fileName, int width, int height, int posx, int posy, boolean visible)
	{
		this.layer = layer;
		this.id = id;
		this.fileName = fileName;
		this.width = width;
		this.height = height;
		this.posx = posx;
		this.posy = posy;
		this.visible = visible;
	}
}
