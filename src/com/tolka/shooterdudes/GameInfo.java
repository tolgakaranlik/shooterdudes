package com.tolka.shooterdudes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import android.content.Context;

public class GameInfo {
	protected Context context;
	
	protected int money;
	protected int gold;

	protected int shields;
	protected int bombs;
	protected int sandclocks;
	
	protected int shieldDuration;
	protected int bombPower;
	protected int sandclockDuration;
	
	// facebook data ??
	
	public int getMoney()
	{
		return money;
	}
	
	public int getGold()
	{
		return gold;
	}
	
	public int getShields()
	{
		return shields;
	}
	
	public int getBombs()
	{
		return bombs;
	}
	
	public int getSandclocks()
	{
		return sandclocks;
	}
	
	public int getShieldDuration()
	{
		return shieldDuration;
	}
	
	public int getBombPower()
	{
		return bombPower;
	}
	
	public int getSandclockDuration()
	{
		return sandclockDuration;
	}
	
	public void setMoney(int money)
	{
		this.money = money;
		saveData();
	}
	
	public void setGold(int gold)
	{
		this.gold = gold;
		saveData();
	}
	
	public void setShields(int shields)
	{
		this.shields = shields;
		saveData();
	}
	
	public void setBombs(int bombs)
	{
		this.bombs = bombs;
		saveData();
	}
	
	public void setSandclocks(int sandclocks)
	{
		this.sandclocks = sandclocks;
		saveData();
	}
	
	public void setShieldDuration(int shieldDuration)
	{
		this.shieldDuration = shieldDuration;
		saveData();
	}
	
	public void setBombPower(int bombPower)
	{
		this.bombPower = bombPower;
		saveData();
	}
	
	public void setSandclockDuration(int sandclockDuration)
	{
		this.sandclockDuration = sandclockDuration;
		saveData();
	}
	
    protected boolean saveData()
	{
    	try
    	{
			OutputStream output = context.openFileOutput("data.dat", Context.MODE_PRIVATE);
			
			output.write(("gold="+ gold+ "\n").getBytes());
			output.write(("money="+ money+ "\n").getBytes());
			
			output.write(("shields="+ shields+ "\n").getBytes());
			output.write(("bombs="+ bombs+ "\n").getBytes());
			output.write(("sandclocks="+ sandclocks+ "\n").getBytes());
	
			output.write(("shield duration="+ shieldDuration+ "\n").getBytes());
			output.write(("bomb power="+ bombPower+ "\n").getBytes());
			output.write(("sandclock duration="+ sandclockDuration+ "\n").getBytes());
	
			output.close();
    	} catch(Throwable t)
    	{
    		return false;
    	}
		
		return true;
	}
	
    public String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
          sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public String getStringFromFile (String filePath) throws Exception {
        InputStream fin = context.openFileInput(filePath);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();        
        return ret;
    }
    
	protected boolean loadData() throws Throwable
	{
		String fileData = null;

		try {
			fileData = getStringFromFile("data.dat");
		} catch (Exception e) {
		}

		if(fileData == null)
		{
			saveData();

			try {
				fileData = getStringFromFile("data.dat");
			} catch (Exception e) {
			}
		}

		String[] lines = fileData.split("\n");

		for(int i = 0; i< lines.length; i++)
		{
			String[] contents = lines[i].split("=");
			
			if(contents.length == 2)
			{
				switch(contents[0])
				{
				case "gold":
					setGold(Integer.parseInt(contents[1]));
					break;
				case "money":
					setMoney(Integer.parseInt(contents[1]));
					break;
				case "shields":
					setShields(Integer.parseInt(contents[1]));
					break;
				case "bombs":
					setBombs(Integer.parseInt(contents[1]));
					break;
				case "sandclocks":
					setSandclocks(Integer.parseInt(contents[1]));
					break;
				case "shield duration":
					setShieldDuration(Integer.parseInt(contents[1]));
					break;
				case "bomb power":
					setBombPower(Integer.parseInt(contents[1]));
					break;
				case "sandclock duration":
					setSandclockDuration(Integer.parseInt(contents[1]));
					break;
				}
			}
		}
		
		return true;
	}
	
	public GameInfo(Context context) throws Throwable
	{
		this.context = context;

		money = 5000;
		gold = 5;
		
		bombs = 3;
		shields = 3;
		sandclocks = 3;
		
		shieldDuration = 0;
		bombPower = 0;
		sandclockDuration = 0;

		if(!loadData())
		{
			throw new Exception("Cannot decrypt file");
		}
	}
	
	public String debugReadFileContents()
	{
		String data;
		try {
			data = getStringFromFile("data.dat");
		} catch (Exception e) {
			return null;
		}

		return data;
	}
}
