package com.tolka.shooterdudes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.tolka.Sprite;

import android.content.Context;

public class StageInfo {
	public enum FrameCharacterActionType {None, Walk, Fire};

	@SuppressWarnings("unused")
	public class Character
	{
		public class KeyFrame
		{
			String fileName;
			Sprite sprite;
			
			public void setSprite(Sprite sprite)
			{
				this.sprite = sprite;
			}
			
			public Sprite getSprite()
			{
				return sprite;
			}
			
			public KeyFrame(String fileName)
			{
				this.fileName = fileName;
				this.sprite = null;
			}
		}
		public class Movement
		{
			private String type;
			private int framedelay;
			private int width;
			private int height;
			private int stepSize;
			private Vector<KeyFrame> keyframes;
			
			public Vector<KeyFrame> getKeyframes()
			{
				return keyframes;
			}
			
			public int getWidth()
			{
				return width;
			}
			
			public int getHeight()
			{
				return height;
			}
			
			public int getFrameDelay()
			{
				return framedelay;
			}
			
			public String getType()
			{
				return type;
			}
			
			public int getStepSize()
			{
				return stepSize;
			}
			
			public void setFrameDelay(int framedelay)
			{
				this.framedelay = framedelay;
			}
			
			public Movement(String type, int framedelay, int width, int height, int stepSize)
			{
				this.type = type;
				this.framedelay = framedelay;
				this.width = width;
				this.height = height;
				this.stepSize = stepSize;
				
				keyframes = new Vector<KeyFrame>();
			}
		}

		private String id;
		private String type;
		private int layer;
		private String shootAction;
		private String shootDrop;
		private int points = 0;
		private int killhits = -1; // -1 means indestructible
		private int foe = 1;
		private float scale = 0.0f;
		private Vector<Movement> movements;
		
		public float getScale()
		{
			return scale;
		}
		
		public void setScale(float scale)
		{
			this.scale = scale;
		}
		
		public Vector<Movement> getMovements()
		{
			return movements;
		}
		
		public int getPoints()
		{
			return points;
		}
		
		public int getKillHits()
		{
			return killhits;
		}
		
		public int getFoe()
		{
			return foe;
		}
		
		public String getId()
		{
			return id;
		}
		
		public String getType()
		{
			return type;
		}
		
		public int getLayer()
		{
			return layer;
		}
		
		public String getShootAction()
		{
			return shootAction;
		}
		
		private Boolean readFile(String fileName) throws Exception
		{
			String data = "";
			try {
			    InputStreamReader isr = new InputStreamReader(context.getAssets().open(fileName));
			    char[] inputBuffer = new char[MAX_BUFFER];
			    isr.read(inputBuffer);
			    
			    data = new String(inputBuffer).replace("\n", "").replace("\r", "").replace("\t", "");
			    isr.close();
			}
			catch (FileNotFoundException e3) {
		    	return false;
			}
			catch (IOException e) {
		    	return false;
			}
			
			XmlPullParserFactory factory = null;
			try {
			    factory = XmlPullParserFactory.newInstance();
			}
			catch (XmlPullParserException e2) {
		    	return false;
			}
			factory.setNamespaceAware(true);
			XmlPullParser xpp = null;
			try {
			    xpp = factory.newPullParser();
			}
			catch (XmlPullParserException e2) {
		    	return false;
			}
			try {
			    xpp.setInput(new StringReader(data));
			}
			catch (XmlPullParserException e1) {
		    	return false;
			}
			int eventType = 0;
			try {
			    eventType = xpp.getEventType();
			}
			catch (XmlPullParserException e1) {
		    	return false;
			}
			
			while (eventType != XmlPullParser.END_DOCUMENT){
			    if (eventType == XmlPullParser.START_TAG) {
			    	// Parse stage info
			    	if(xpp.getName().equals("info"))
			    	{
			    		if(!parseCharacterInfo(xpp))
			    		{
			    			return false;
			    		}
			    	} else
				    	
			    	// Parse characters
			    	if(xpp.getName().equals("movement"))
			    	{
			    		if(!parseMovements(xpp))
			    		{
			    			return false;
			    		}
			    	}

			    	// Parse character movements
			    	if(xpp.getName().equals("keyframe"))
			    	{
			    		if(!parseKeyframes(xpp))
			    		{
			    			return false;
			    		}
			    	}
			    }

			    try {
			        eventType = xpp.next();
			    }
			    catch (XmlPullParserException e) {
			    	return false;
			    }
			    catch (IOException e) {
			        return false;
			    }
			}

			return true;
		}
		
		private boolean parseKeyframes(XmlPullParser xpp) {
			for(int i = 0; i< xpp.getAttributeCount(); i++)
			{
				String file = "";

				switch(xpp.getAttributeName(i))
				{
				case "file":
					file = xpp.getAttributeValue(i);
					break;
				default:
					return false;
				}
				
				KeyFrame keyFrame = new KeyFrame(file);
				currentMovement.keyframes.add(keyFrame);
			}

			return true;
		}

		private Movement currentMovement;
		
		private boolean parseMovements(XmlPullParser xpp) throws XmlPullParserException, IOException {
			String type = "";
			int framedelay = 0;
			int width = 0;
			int height = 0;
			int stepsize = 0;

            for(int i = 0; i< xpp.getAttributeCount(); i++)
            {
            	switch(xpp.getAttributeName(i))
            	{
            	case "type":
            		type = xpp.getAttributeValue(i);
            		break;
            	case "framedelay":
            		framedelay = Integer.parseInt(xpp.getAttributeValue(i));
            		break;
            	case "width":
            		width = Integer.parseInt(xpp.getAttributeValue(i));
            		break;
            	case "height":
            		height = Integer.parseInt(xpp.getAttributeValue(i));
            		break;
            	case "stepsize":
            		stepsize = Integer.parseInt(xpp.getAttributeValue(i));
            		break;
            	}
            }

            Movement movement = new Movement(type, framedelay, width, height, stepsize);
            movements.add(movement);

			currentMovement = movement;
		    try {
		        int eventType = xpp.next();
		    }
		    catch (XmlPullParserException e) {
		        return false;
		    }
		    catch (IOException e) {
		        return false;
		    }

			return true;
		}

		private boolean parseCharacterInfo(XmlPullParser xpp) {
			for(int i = 0; i< xpp.getAttributeCount(); i++)
			{
				switch(xpp.getAttributeName(i))
				{
				case "points":
					this.points = Integer.parseInt(xpp.getAttributeValue(i));
					break;
				case "killhits":
					this.killhits = Integer.parseInt(xpp.getAttributeValue(i));
					break;
				case "foe":
					this.foe = Integer.parseInt(xpp.getAttributeValue(i));
					break;
				default:
					return false;
				}
			}

			return true;
		}

		public Character(String id, String type, int layer, String shootAction)
		{
			this.id = id;
			this.type = type;
			this.layer = layer;
			this.shootAction = shootAction;
			this.shootDrop = "";
			this.scale = 1.0f;
			movements = new Vector<Movement>();
			currentMovement = null;
			
			try {
				readFile("characters/"+ type+ ".xml");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		public Character(String id, String type, int layer, String shootAction, String shootDrop, float scale)
		{
			this.id = id;
			this.type = type;
			this.layer = layer;
			this.shootAction = shootAction;
			this.shootDrop = shootDrop;
			this.scale = scale;
			movements = new Vector<Movement>();
			currentMovement = null;
			
			try {
				readFile("characters/"+ type+ ".xml");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public class Building {
		public class Visual
		{
			private Sprite sprite;
			private Sprite hitSprite;
			private String fileName;
			private String hit;
			private int width;
			private int height;
			private int afterShot;
			
			public Sprite getSprite()
			{
				return sprite;
			}
			
			public Sprite getHitSprite()
			{
				return hitSprite;
			}
			
			public String getHit()
			{
				return hit;
			}
			
			public void setHit(String hit)
			{
				this.hit = hit;
			}
			
			public String getFileName()
			{
				return fileName;
			}
			
			public int getWidth()
			{
				return width;
			}
			
			public int getHeight()
			{
				return height;
			}
			
			public int getAfterShot()
			{
				return afterShot;
			}
			
			public void setSprite(Sprite sprite)
			{
				this.sprite = sprite;
			}
			
			public void setHitSprite(Sprite hitSprite)
			{
				this.hitSprite = hitSprite;
			}
			
			public Visual(String fileName, String hit, int afterShot, int width, int height)
			{
				this.fileName = fileName;
				this.width = width;
				this.height = height;
				this.afterShot = afterShot;
				this.sprite = null;
				this.hitSprite = null;
				this.hit = hit;
			}
		}
		
		private String id;
		private String type;
		private int posx;
		private int posy;
		private int scale;
		private int layer;
		private int points = 0;
		private int killhits = -1; // -1 means indestructible
		private int foe = 1;
		private int destructionDelay = 100;
		private String destructionSound = "crash1";
		private String afterDestruction = "";
		
		public int getDestructionDelay()
		{
			return destructionDelay;
		}
		
		public String getDesturctionSound()
		{
			return destructionSound;
		}
		
		public String getAfterDestruction()
		{
			return afterDestruction;
		}
		
		public String getId()
		{
			return id;
		}
		
		public String getType()
		{
			return type;
		}
		
		public int getPosX()
		{
			return posx;
		}
		
		public int getPosY()
		{
			return posy;
		}
		
		public int getScale()
		{
			return scale;
		}
		
		public int getLayer()
		{
			return layer;
		}
		
		public int getPoints()
		{
			return points;
		}
		
		public int getKillHits()
		{
			return killhits;
		}
		
		public int getFoe()
		{
			return foe;
		}
		
		private Boolean readFile(String fileName) throws Exception
		{
			String data = "";
			try {
			    InputStreamReader isr = new InputStreamReader(context.getAssets().open(fileName));
			    char[] inputBuffer = new char[MAX_BUFFER];
			    isr.read(inputBuffer);
			    
			    data = new String(inputBuffer).replace("\n", "").replace("\r", "").replace("\t", "");
			    isr.close();
			}
			catch (FileNotFoundException e3) {
		    	return false;
			}
			catch (IOException e) {
		    	return false;
			}
			
			XmlPullParserFactory factory = null;
			try {
			    factory = XmlPullParserFactory.newInstance();
			}
			catch (XmlPullParserException e2) {
		    	return false;
			}
			factory.setNamespaceAware(true);
			XmlPullParser xpp = null;
			try {
			    xpp = factory.newPullParser();
			}
			catch (XmlPullParserException e2) {
		    	return false;
			}
			try {
			    xpp.setInput(new StringReader(data));
			}
			catch (XmlPullParserException e1) {
		    	return false;
			}
			int eventType = 0;
			try {
			    eventType = xpp.getEventType();
			}
			catch (XmlPullParserException e1) {
		    	return false;
			}
			
			while (eventType != XmlPullParser.END_DOCUMENT){
			    if (eventType == XmlPullParser.START_TAG) {
			    	// Parse stage info
			    	if(xpp.getName().equals("info"))
			    	{
			    		if(!parseBuildingInfo(xpp))
			    		{
			    			return false;
			    		}
			    	} else

				    	// Parse characters
				    	if(xpp.getName().equals("visuals"))
				    	{
				    		if(!parseVisuals(xpp))
				    		{
				    			return false;
				    		}
				    	}

			    	// Parse destruction
			    	if(xpp.getName().equals("destruction"))
			    	{
			    		for(int i = 0; i< xpp.getAttributeCount(); i++)
			    		{
			    			switch(xpp.getAttributeName(i)){
			    			case "delay":
			    				destructionDelay = Integer.parseInt(xpp.getAttributeValue(i));
			    				break;
			    			case "sound":
			    				destructionSound = xpp.getAttributeValue(i);
			    				break;
			    			case "after":
			    				afterDestruction = xpp.getAttributeValue(i);
			    			}
			    		}

			    		if(!parseDestruction(xpp))
			    		{
			    			return false;
			    		}
			    	}
			    }

			    try {
			        eventType = xpp.next();
			    }
			    catch (XmlPullParserException e) {
			    	return false;
			    }
			    catch (IOException e) {
			        return false;
			    }
			}

			return true;
		}
		
		private boolean parseDestruction(XmlPullParser xpp) throws XmlPullParserException, IOException {
		    int eventType = xpp.next();
			
			while (eventType != XmlPullParser.END_DOCUMENT){
			    if (eventType == XmlPullParser.START_TAG) {
			    	if(xpp.getName().equals("visual"))
			    	{
			    		String fileName = "";
			    		String hit = "";

			            for(int i = 0; i< xpp.getAttributeCount(); i++)
			            {
			            	switch(xpp.getAttributeName(i))
			            	{
			            	case "src":
			            		fileName = xpp.getAttributeValue(i);
			            		break;
			            	case "hit":
			            		hit = xpp.getAttributeValue(i);
			            		break;
			            	}
			            }

			            Visual visual = new Visual(fileName, hit, 0, 1, 1);
			            desctruction.add(visual);
			    	} else
			    	{
			    		break;
			    	}
		    	}
			    
			    try {
			        eventType = xpp.next();
			    }
			    catch (XmlPullParserException e) {
			        return false;
			    }
			    catch (IOException e) {
			        return false;
			    }
			}

			return true;
		}

		private boolean parseVisuals(XmlPullParser xpp) throws Exception {
		    int eventType = xpp.next();
			
			while (eventType != XmlPullParser.END_DOCUMENT){
			    if (eventType == XmlPullParser.START_TAG) {
			    	if(xpp.getName().equals("visual"))
			    	{
			    		String fileName = "";
			    		String hit = "";
			    		int aftershot = 0;
			    		int width = 0;
			    		int height = 0;

			            for(int i = 0; i< xpp.getAttributeCount(); i++)
			            {
			            	switch(xpp.getAttributeName(i))
			            	{
			            	case "src":
			            		fileName = xpp.getAttributeValue(i);
			            		break;
			            	case "hit":
			            		hit = xpp.getAttributeValue(i);
			            		break;
			            	case "aftershot":
			            		aftershot = Integer.parseInt(xpp.getAttributeValue(i));
			            		break;
			            	case "width":
			            		width = Integer.parseInt(xpp.getAttributeValue(i));
			            		break;
			            	case "height":
			            		height = Integer.parseInt(xpp.getAttributeValue(i));
			            		break;
			            	}
			            }

			            Visual visual = new Visual(fileName, hit, aftershot, width, height);
			            visuals.add(visual);
			    	} else
			    	{
			    		break;
			    	}
		    	}
			    
			    try {
			        eventType = xpp.next();
			    }
			    catch (XmlPullParserException e) {
			        return false;
			    }
			    catch (IOException e) {
			        return false;
			    }
			}

			return true;
		}

		private boolean parseBuildingInfo(XmlPullParser xpp) {
			for(int i = 0; i< xpp.getAttributeCount(); i++)
			{
				switch(xpp.getAttributeName(i))
				{
				case "points":
					this.points = Integer.parseInt(xpp.getAttributeValue(i));
					break;
				case "killhits":
					this.killhits = Integer.parseInt(xpp.getAttributeValue(i));
					break;
				case "foe":
					this.foe = Integer.parseInt(xpp.getAttributeValue(i));
					break;
				default:
					return false;
				}
			}

			return true;
		}

		private int currHits;
		
		public Vector<Visual> visuals;
		public Vector<Visual> desctruction;
		
		public Building(String id, String type, int posx, int posy, int scale, int layer)
		{
			this.id = id;
			this.type = type;
			this.posx = posx;
			this.posy = posy;
			this.scale = scale;
			this.layer = layer;
			this.currHits = 0;
			visuals = new Vector<Visual>();
			desctruction = new Vector<Visual>();
			
			try {
				if(!readFile("buildings/"+ type+ ".xml"))
				{
					readFile("objects/"+ type+ ".xml");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public int getCurrHits()
		{
			return currHits;
		}
		
		public void incrementReceivedHits(int damage) {
			currHits += damage;
			if(currHits > killhits)
			{
				currHits = killhits;
			}
		}
		
		public void incrementReceivedHits() {
			currHits++;			
		}
	}
	public class FrameCharacterAction
	{
		private FrameCharacterActionType actionType;
		private String walkType;
		private int walkSteps;
		private int fireWait;
		private int direction;
		
		public FrameCharacterActionType getActionType()
		{
			return actionType;
		}
		
		public void setWalkType(String walkType)
		{
			this.walkType = walkType;
		}
		
		public void setWalkSteps(int walkSteps)
		{
			this.walkSteps = walkSteps;
		}
		
		public void setDirection(int direction)
		{
			this.direction = direction;
		}
		
		public void setFireWait(int fireWait)
		{
			this.fireWait = fireWait;
		}
		
		public String getWalkType(String walkType)
		{
			return this.walkType;
		}
		
		public int getWalkSteps()
		{
			return walkSteps;
		}
		
		public int getFireWait()
		{
			return fireWait;
		}
		
		public int getDirection()
		{
			return direction;
		}
		
		public FrameCharacterAction(FrameCharacterActionType actionType)
		{
			this.actionType = actionType;
			this.walkType = "constant";
			this.walkSteps = 1;
			this.direction = 1;
			this.fireWait = 5000;
		}
	}
	public class FrameCharacter
	{
		private String id;
		private int appearx;
		private int appeary;
		private int currentx;
		private int currenty;
		private Vector<FrameCharacterAction> actions;
		private int currentAction;
		
		public String getId()
		{
			return id;
		}
		
		public int getAppearX()
		{
			return appearx;
		}
		
		public int getAppearY()
		{
			return appeary;
		}
		
		public Vector<FrameCharacterAction> getActions()
		{
			return actions;
		}
		
		public void setCurrentAction(int action)
		{
			this.currentAction = action;
		}
		
		public int getCurrentAction()
		{
			return currentAction;
		}
		
		public void toInitialState()
		{
			this.currentx = appearx;
			this.currenty = appeary;
			this.currentAction= 0; 
		}
		
		public FrameCharacter(String id, int appearx, int appeary)
		{
			this.id = id;
			this.appearx = appearx;
			this.appeary = appeary;
			actions = new Vector<FrameCharacterAction>();

			toInitialState();
		}

		public int getCurrentX()
		{
			return currentx;
		}
		
		public int getCurrentY()
		{
			return currenty;
		}
		
		public void setCurrentPosition(int currentx, int currenty) {
			this.currentx = currentx;
			this.currenty = currenty;
		}
	}
	public class FrameCommand{
		private String type;
		private int target;
		
		public String getType()
		{
			return type;
		}
		
		public int getTarget()
		{
			return target;
		}
		
		public FrameCommand(String type, int target)
		{
			this.type = type;
			this.target = target;
		}
	}
	public class Frame
	{
		private int time;
		private Vector<FrameCharacter> characters;
		private Vector<FrameCommand> commands;
		
		public int getTime()
		{
			return time;
		}
		
		public Vector<FrameCharacter> getCharacters()
		{
			return characters;
		}

		public Vector<FrameCommand> getCommands()
		{
			return commands;
		}

		public Frame(int time)
		{
			this.time = time;
			characters = new Vector<FrameCharacter>();
			commands = new Vector<FrameCommand>();
		}
	}
	
	private int targetFoe;
	private int maxDepth;
	private String backgroundImage;
	private String backgroundColor;
	private Context context;
	private Vector<Character> characters;
	private Vector<Building> buildings;
	private Vector<Building> objects;
	private Vector<Frame> frames;
	
	public Vector<Character> getCharacters()
	{
		return characters;
	}
	
	public Vector<Building> getBuildings()
	{
		return buildings;
	}
	
	public Vector<Building> getObjects()
	{
		return objects;
	}
	
	public Vector<Frame> getFrames()
	{
		return frames;
	}
	
	public int getTargetFoe()
	{
		return targetFoe;
	}
	
	public int getMaxDepth()
	{
		return maxDepth;
	}
	
	public String getBackgroundImage()
	{
		return backgroundImage;
	}
	
	public String getBackgroundColor()
	{
		return backgroundColor;
	}
	
	// 128K max buffer!
	private final int MAX_BUFFER = 128* 1024;
	
	private void parseFile(String fileName) throws Exception
	{
		String data = "";
		try {
		    InputStreamReader isr = new InputStreamReader(context.getAssets().open("stages/stage_1_1.xml"));
		    char[] inputBuffer = new char[MAX_BUFFER];
		    isr.read(inputBuffer);
		    
		    data = new String(inputBuffer).replace("\n", "").replace("\r", "").replace("\t", "");
		    isr.close();
		}
		catch (FileNotFoundException e3) {
		    e3.printStackTrace();
		}
		catch (IOException e) {
		    e.printStackTrace();
		}
		
		XmlPullParserFactory factory = null;
		try {
		    factory = XmlPullParserFactory.newInstance();
		}
		catch (XmlPullParserException e2) {
		    e2.printStackTrace();
		}
		factory.setNamespaceAware(true);
		XmlPullParser xpp = null;
		try {
		    xpp = factory.newPullParser();
		}
		catch (XmlPullParserException e2) {
		    e2.printStackTrace();
		}
		try {
		    xpp.setInput(new StringReader(data));
		}
		catch (XmlPullParserException e1) {
		    e1.printStackTrace();
		}
		int eventType = 0;
		try {
		    eventType = xpp.getEventType();
		}
		catch (XmlPullParserException e1) {
		    e1.printStackTrace();
		}
		
		while (eventType != XmlPullParser.END_DOCUMENT){
		    if (eventType == XmlPullParser.START_TAG) {
		    	// Parse stage info
		    	if(xpp.getName().equals("info"))
		    	{
		    		parseStageInfo(xpp);
		    	} else
			    	
		    	// Parse characters
		    	if(xpp.getName().equals("characters"))
		    	{
		    		parseCharacters(xpp);
		    	}
		    	
		    	// Parse buildings
		    	if(xpp.getName().equals("buildings"))
		    	{
		    		parseBuildings(xpp);
		    	}
		    	
		    	// Parse objects
		    	if(xpp.getName().equals("objects"))
		    	{
		    		parseObjects(xpp);
		    	}
		    	
		    	// Parse time line
		    	if(xpp.getName().equals("timeline"))
		    	{
		    		parseTimeLine(xpp);
		    	}
		    }

		    try {
		        eventType = xpp.next();
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}
	
	private void parseTimeLine(XmlPullParser xpp) throws Exception {
		int eventType = xpp.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT){
		    if (eventType == XmlPullParser.START_TAG) {
		    	if(xpp.getName().equals("frame"))
		    	{
		    		int time = 0;

		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "time":
		            		time = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	}
		            }

		            Frame frame = new Frame(time);
		            parseFrameContents(xpp, frame);

		            frames.add(frame);
		    	} else
		    	{
		    		break;
		    	}
	    	}
		    
		    try {
		        eventType = xpp.next();
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private void parseFrameContents(XmlPullParser xpp, Frame frame) throws Exception {
		int eventType = xpp.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT){
		    if (eventType == XmlPullParser.START_TAG) {
		    	if(xpp.getName().equals("character"))
		    	{
		    		String id = "";
		    		int appearx = 0;
		    		int appeary = 0;

		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "id":
		            		id = xpp.getAttributeValue(i);
		            		break;
		            	case "appearx":
		            		appearx = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "appeary":
		            		appeary = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	}
		            }

		            FrameCharacter frameCharacter = new FrameCharacter(id, appearx, appeary);
		            parseFrameCharacterActions(xpp, frameCharacter);

		            frame.characters.add(frameCharacter);
		    	}
		    	else if(xpp.getName().equals("command"))
		    	{
	            	String type = "";
	            	int target = 0;
	            	
		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "type":
		            		type = xpp.getAttributeValue(i);
		            		break;
		            	case "target":
		            		target = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	}
		            }
		            
		            frame.commands.add(new FrameCommand(type, target));
		    	}
		    	else {
		    		break;
		    	}
	    	}
		    
		    try {
		        eventType = xpp.next();
		        
		        if(eventType != XmlPullParser.TEXT && xpp.getName().equals("frame") && eventType == XmlPullParser.END_TAG)
		        {
		        	break;
		        }
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private void parseFrameCharacterActions(XmlPullParser xpp, FrameCharacter frameCharacter) throws Exception {
		int eventType = xpp.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT){
		    if (eventType == XmlPullParser.START_TAG) {
		    	if(xpp.getName().equals("walk") || xpp.getName().equals("fire"))
		    	{
		    		FrameCharacterActionType actionType = FrameCharacterActionType.None;
		    		switch(xpp.getName())
		    		{
		    		case "walk":
		    			actionType = FrameCharacterActionType.Walk;
		    			break;
		    		case "fire":
		    			actionType = FrameCharacterActionType.Fire;
		    			break;
		    		}

		            FrameCharacterAction frameCharacterAction = new FrameCharacterAction(actionType);

		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "type":
		            		frameCharacterAction.setWalkType(xpp.getAttributeValue(i));
		            		break;
		            	case "steps":
		            		frameCharacterAction.setWalkSteps(Integer.parseInt(xpp.getAttributeValue(i)));
		            		break;
		            	case "wait":
		            		frameCharacterAction.setFireWait(Integer.parseInt(xpp.getAttributeValue(i)));
		            		break;
		            	}
		            }

		            frameCharacter.getActions().add(frameCharacterAction);
		    	} else
		    	{
		    		break;
		    	}
	    	}
		    
		    try {
		        eventType = xpp.next();
		        
		        if(eventType != XmlPullParser.TEXT && xpp.getName().equals("character") && eventType == XmlPullParser.END_TAG)
		        {
		        	break;
		        }
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private void parseObjects(XmlPullParser xpp) throws Exception {
		int eventType = xpp.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT){
		    if (eventType == XmlPullParser.START_TAG) {
		    	if(xpp.getName().equals("object"))
		    	{
		    		String type = "N/A";
		    		int posx = 0;
		    		int posy = 0;
		    		int scale = 1;
		    		int layer = 50;

		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "type":
		            		type = xpp.getAttributeValue(i);
		            		break;
		            	case "posx":
		            		posx = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "posy":
		            		posy = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "scale":
		            		scale = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "layer":
		            		layer = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	}
		            }

		            Building building = new Building("obj"+ (objects.size()+ 1), type, posx, posy, scale, layer);
		            objects.add(building);
		    	} else
		    	{
		    		break;
		    	}
	    	}
		    
		    try {
		        eventType = xpp.next();
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private void parseBuildings(XmlPullParser xpp) throws Exception {
		int eventType = xpp.next();
		
		while (eventType != XmlPullParser.END_DOCUMENT){
		    if (eventType == XmlPullParser.START_TAG) {
		    	if(xpp.getName().equals("building"))
		    	{
		    		String id = "N/A";
		    		String type = "N/A";
		    		int posx = 0;
		    		int posy = 0;
		    		int scale = 1;
		    		int layer = 50;

		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "id":
		            		id = xpp.getAttributeValue(i);
		            		break;
		            	case "type":
		            		type = xpp.getAttributeValue(i);
		            		break;
		            	case "posx":
		            		posx = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "posy":
		            		posy = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "scale":
		            		scale = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "layer":
		            		layer = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	}
		            }

		            Building building = new Building(id, type, posx, posy, scale, layer);
		            buildings.add(building);
		    	} else
		    	{
		    		break;
		    	}
	    	}
		    
		    try {
		        eventType = xpp.next();
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private void parseCharacters(XmlPullParser xpp) throws Exception {
		int eventType = 0;
		try {
		    eventType = xpp.next();
		}
		catch (XmlPullParserException e1) {
		    e1.printStackTrace();
		}
		
		while (eventType != XmlPullParser.END_DOCUMENT){
			if (eventType == XmlPullParser.START_TAG) {

		    	if(xpp.getName().equals("character"))
		    	{
		    		String id = "N/A";
		    		String type = "N/A";
		    		int layer = 50;
		    		String shootAction = "N/A";
		    		String shootDrop = "";
		    		float scale = 0;

		            for(int i = 0; i< xpp.getAttributeCount(); i++)
		            {
		            	switch(xpp.getAttributeName(i))
		            	{
		            	case "id":
		            		id = xpp.getAttributeValue(i);
		            		break;
		            	case "type":
		            		type = xpp.getAttributeValue(i);
		            		break;
		            	case "layer":
		            		layer = Integer.parseInt(xpp.getAttributeValue(i));
		            		break;
		            	case "shootaction":
		            		shootAction = xpp.getAttributeValue(i);
		            		break;
		            	case "scale":
		            		scale = Float.parseFloat(xpp.getAttributeValue(i));
		            		break;
		            	case "shootdrop":
		            		shootDrop = xpp.getAttributeValue(i);
		            		break;
		            	}
		            }

		            Character character = new Character(id, type, layer, shootAction, shootDrop, scale);
		            characters.add(character);
		    	} else
		    	{
		    		break;
		    	}
		    }

		    try {
		        eventType = xpp.next();
		    }
		    catch (XmlPullParserException e) {
		        e.printStackTrace();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		}
	}

	private void parseStageInfo(XmlPullParser xpp) {
        for(int i = 0; i< xpp.getAttributeCount(); i++)
        {
        	switch(xpp.getAttributeName(i))
        	{
        	case "targetfoe":
        		targetFoe = Integer.parseInt(xpp.getAttributeValue(i));
        		break;
        	case "maxdepth":
        		maxDepth = Integer.parseInt(xpp.getAttributeValue(i));
        		break;
        	case "background":
        		backgroundImage = xpp.getAttributeValue(i);
        		break;
        	case "bgcolor":
        		backgroundColor = xpp.getAttributeValue(i);
        		break;
        	}
        }
	}

	public StageInfo(String fileName, Context context)
	{
		targetFoe = 0;
		maxDepth = 0;
		backgroundImage = "";
		backgroundColor = "";
		this.context = context;
		characters = new Vector<Character>();
		buildings = new Vector<Building>();
		objects = new Vector<Building>();
		frames = new Vector<Frame>();
		
		try {
			parseFile(fileName);
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
