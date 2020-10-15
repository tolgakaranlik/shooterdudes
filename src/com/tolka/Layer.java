package com.tolka;

public class Layer {
	public int id;
	
	public MotionTweenFactory tweenFactory;
	public SpriteFactory spriteFactory;
	public MovieClipFactory movieClipFactory; 
	public TextFactory textFactory;
	
	public Layer(int id)
	{
		this.id = id;

		tweenFactory = new MotionTweenFactory();
		spriteFactory = new SpriteFactory();
		movieClipFactory = new MovieClipFactory();
		textFactory = new TextFactory();
	}
}
