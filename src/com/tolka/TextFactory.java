package com.tolka;

import java.util.Vector;

public class TextFactory {
	public static class TextData {
		private Text text;
		private String id;
		private Boolean visible;
		
		public Text getText()
		{
			return text;
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
		
		public TextData(Text text, String id)
		{
			this.text = text;
			this.visible = false;
			this.id = id;
		}

		public void setText(Text text) {
			this.text = text;
		}
	}
	
	public Vector<TextData> texts;
	
	public TextFactory()
	{
		texts = new Vector<TextData>();
	}
	
	public void invisibleAll()
	{
		for(int i = 0; i< texts.size(); i++)
		{
			texts.get(i).setVisible(false);
		}
	}
	
	public void setPosition(String id, float x, float y)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				texts.get(i).getText().setPosition((int)x, (int)y);
				break;
			}
		}
	}
	public void setSprite(String id, Text text)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				texts.get(i).setText(text);
				break;
			}
		}
	}
	public float getScale(String id)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return texts.get(i).getText().getScale();
			}
		}
		
		return -1;
	}
	public void setScale(String id, float scale)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				texts.get(i).getText().setScale(scale);
				break;
			}
		}
	}
	public void setMessage(String id, String message)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				texts.get(i).getText().setMessage(message);
				break;
			}
		}
	}
	
	public String getErrorMessage(String id)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return texts.get(i).getText().getError();
			}
		}

		return "Text not found";
	}
	public void setPosition(int i, float x, float y)
	{
		texts.get(i).getText().setPosition((int)x, (int)y);
	}
	public void setVisible(String id, Boolean visible)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				texts.get(i).setVisible(visible);
				break;
			}
		}
	}
	public int getX(String id)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return texts.get(i).getText().getPosX();
			}
		}
		
		return 0;
	}
	public int getY(String id)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return texts.get(i).getText().getPosY();
			}
		}
		
		return 0;
	}
	public void setVisible(int order, Boolean visible)
	{
		texts.get(order).setVisible(visible);
	}
	public Boolean add(String id, Text text)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return false;
			}
		}
		
		texts.add(new TextData(text, id));
		return true;
	}
	public void draw()
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getVisible())
			{
				texts.get(i).getText().draw();
			}
		}
	}

	public int getTotalWidth(String id)
	{
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return texts.get(i).getText().getTotalWidth();
			}
		}
		
		return -1;
	}
	
	public boolean getVisible(String id) {
		for(int i = 0; i< texts.size(); i++)
		{
			if(texts.get(i).getId().equals(id))
			{
				return texts.get(i).getVisible();
			}
		}
		
		return false;
	}
}
