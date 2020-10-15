package com.tolka.shooterdudes;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Random;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.tolka.Layer;
import com.tolka.MotionTween;
import com.tolka.MovieClip;
import com.tolka.RunAfterFunction;
import com.tolka.Sprite;
import com.tolka.SpriteFactory.SpriteData;
import com.tolka.Text;
import com.tolka.shooterdudes.StageInfo.Building;
import com.tolka.shooterdudes.StageInfo.FrameCharacter;
import com.tolka.shooterdudes.StageInfo.FrameCharacterAction;
import com.tolka.shooterdudes.StageInfo.FrameCommand;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.Toast;

public class GlRenderer implements Renderer {
	public static final int MAX_SURPRISECHEST_ITEMS = 20;
	public static final int YBIAS = 210;

	public static final int GAMEMODE_LOADTEXTURES_PREPARE = 0x0010;
	public static final int GAMEMODE_LOADTEXTURES = 0x0020;
	public static final int GAMEMODE_COMPANY = 0x0030;
	public static final int GAMEMODE_INTRO = 0x0040;
	public static final int GAMEMODE_INTRO_SELECTGAME = 0x0050;
	public static final int GAMEMODE_INTRO_SELECTLEVEL = 0x0060;
	public static final int GAMEMODE_INTRO_SELECTSUBLEVEL = 0x0070;
	public static final int GAMEMODE_LOAD_LEVEL = 0x0080;
	public static final int GAMEMODE_PLAY_LEVEL1 = 0x0090;

	protected float[] mapDiffLvl1X = {0,-247,-165,-434,-311,-433,-604,-525,-696,-959,-810,-710,-1135,-1219,-950,-987,-1189,-1383,-1499,-1393,-1288,-1533,-1595,-1417,-1300,-1486,-1657,-1866,-1972,-1880,-1706,-1777,-1683,-1873,-2008};
	protected float[] mapDiffLvl1Y = {-440,-262,-506,-293,-77,64,-165,-369,-504,-383,-95,-292,-424,-239,-62,53,-46,-186,-384,-316,-390,-481,-266,-106,19,66,14,53,-71,-268,-176,-83,-445,-468,-350};

	private int m_width = 0;
	private int m_height = 0;
	private Context context;
	private float m_ratio = 1;
	private float crossairX = 0;
	private float crossairY = 0;
	private StageInfo stageInfo;
	private GL10 gl;
	private int currFrame = 0;
	private long lastClockTick = -1;
	private long lastShootAt;
	private int currWeapon;
	private int currFoe;
	private int targetFoe;

	protected Random rand = new Random();
	protected int score;
	protected int scoreToSet = -1;
	protected int moneyToSet = -1;
	protected int goldToSet = -1;
	protected int livesToSet = -1;
	protected int sandclocksToSet = -1;
	protected int shieldsToSet = -1;
	protected int bombsToSet = -1;
	protected int maxLives = 5;
	protected int currLives = 5;
	protected int bombPowerToSet = -1;
	protected int shieldDurationToSet = -1;
	protected int sandclockDurationToSet = -1;
	protected boolean firstTime = true;

	public static final int WEAPON_REVOLVER = 0;
	public static final int WEAPON_REVOLVER_DELAY = 500;
	
	public static final int MAX_SHOOT_FRAMES = 5;
	
	public int remainingShots;
	public int GameMode;
	public Vector<Sprite> levelSprites;
	public Vector<Layer> layers;
	public Vector<TextureToLoad> texturesToLoad;
	
	protected final static int LOADINGSTEP_PREPARE = 0x0001; 
	protected final static int LOADINGSTEP_OBJECTS = 0x0002; 
	protected final static int LOADINGSTEP_BUILDINGS = 0x0003; 
	protected final static int LOADINGSTEP_CHARACTERS = 0x0004; 
	
	protected long tapScreenStart;
	
	protected int loadingObjectCount;
	protected int loadingBuildingCount;
	protected int loadingCharacterCount;	
	protected int loadingObjectCurrent;
	protected int loadingBuildingCurrent;
	protected int loadingCharacterCurrent;
	protected int loadingProgress = 0;
	protected int loadingStep = LOADINGSTEP_PREPARE;

	protected int boostOldY;
	protected boolean autoScrollMode;
	protected int boostDiff;
	protected long boostScrollStart;
	protected final int BOOST_SCROLL_DELAY = 50;
	protected int surpriseChestItem = -1;
	
	protected int selectedLevel;
	protected int selectedSublevel;
	protected Boolean levelLoaded = false;
    protected int totalProcessed = 0;
    protected String timeLineLog = "";
    protected boolean canHandleTouch;
    protected int currentTab;
    protected int currentLevelTab;
    protected boolean mapLoadedLevel1 = false;
    protected boolean mapLoadedAttempt1 = false;
	protected int subLevelCount[] = {34, 0, 0, 0};
	protected int subLevelCurrent[] = {0, 0, 0, 0}; // current sublevel for all levels
    protected GameInfo gameInfo;
    protected boolean texturesCreated = false;
    
    protected boolean shieldMode = false;
    protected boolean sandclockMode = false;
    protected long shieldModeActivatedAt = 0;
    protected long sandclockModeActivatedAt = 0;
	
    protected int gameModeToSet = GAMEMODE_COMPANY;
    protected int temporaryGameMode = -1;
    protected PayPalConfiguration config;
    protected boolean shouldReloadTextures = true;
    
    public void addGold(int gold)
    {
    	this.goldToSet = gameInfo.getGold()+ gold;
    }
    public void addMoney(int money)
    {
    	this.moneyToSet = gameInfo.getMoney()+ money;
    }
    
    public GlRenderer(Context context, int width, int height, PayPalConfiguration config) {
		//stageInfo = new StageInfo("stages/stage_1_1.xml", context);
    	this.config = config;

    	texturesToLoad = new Vector<TextureToLoad>();
		try
		{
			gameInfo = new GameInfo(context);
			goldToSet = gameInfo.gold;
			moneyToSet = gameInfo.money;
		} catch(Throwable ex)
		{
			timeLineLog = "Init: "+ ex.getMessage();
		}

		this.m_height = height;
		this.m_width = width;
		this.m_ratio = width/ 1100.0f; // 0.83f;
		this.context = context;
		zeroLayers();

		this.gl = null;

		setGameMode(GlRenderer.GAMEMODE_COMPANY);
	}	
	
	public void setGameMode(int mode)
	{
		switch(mode)
		{
		case GAMEMODE_LOADTEXTURES_PREPARE:
			invisibleAllTextures();
			findLayerById(100).spriteFactory.setVisible("LOADINGFIRST", true);
			findLayerById(101).spriteFactory.setVisible("PROGRESS", true);

			break;
		case GAMEMODE_LOADTEXTURES:
			invisibleAllTextures();
			findLayerById(100).spriteFactory.setVisible("TOLKA", false);
			findLayerById(100).spriteFactory.setVisible("LOADINGFIRST", true);
			findLayerById(101).spriteFactory.setVisible("PROGRESS", true);

			break;
		case GAMEMODE_COMPANY:
			invisibleAllTextures();
			tapScreenStart = getClockTick();
			findLayerById(100).spriteFactory.setVisible("TOLKA", true);

			break;
		case GAMEMODE_INTRO:
			invisibleAllTextures();
			gameModeToSet = GameMode;
			tapScreenStart = getClockTick();
			findLayerById(100).spriteFactory.setVisible("INTRO", true);

	    	break;
		case GAMEMODE_INTRO_SELECTGAME:
			invisibleAllTextures();
			gameModeToSet = GameMode;

	    	findLayerById(100).spriteFactory.setVisible("INTRODARK", true);
			findLayerById(101).spriteFactory.setVisible("MENU1", true);

			break;
		case GAMEMODE_INTRO_SELECTLEVEL:
			invisibleAllTextures();
			gameModeToSet = GameMode;
			findLayerById(131).spriteFactory.setVisible("UISTATS", true);
			findLayerById(99).spriteFactory.setVisible("WOODSHOLE", true);
			findLayerById(90).spriteFactory.setVisible("FRAMEBG", true);
			findLayerById(120).spriteFactory.setVisible("FRAME1", true);
			findLayerById(103).spriteFactory.setVisible("SMALLOGO", true);

			findLayerById(131).textFactory.setPosition("MONEY", 235* m_ratio, 7* m_ratio);
			findLayerById(131).textFactory.setVisible("MONEY", true);
			findLayerById(131).textFactory.setPosition("GOLD", 145* m_ratio, 7* m_ratio);
			findLayerById(131).textFactory.setVisible("GOLD", true);

			displayMainMenuTabs();
			break;
		case GAMEMODE_INTRO_SELECTSUBLEVEL:
			gameModeToSet = GameMode;

			//displayMainMenuTabs();
			break;
		case GAMEMODE_PLAY_LEVEL1:
			gameModeToSet = GameMode;
			findLayerById(99).textFactory.setPosition("SCORE", 5* m_ratio, 65* m_ratio);
			findLayerById(99).textFactory.setVisible("SCORE", true);
			findLayerById(103).textFactory.setVisible("SMALLOGO", false);

			remainingShots = 6;
			currLives = maxLives;

			break;
		}
		
		GameMode = mode;
	}
	
	private void invisibleAllTextures() {
		for(int i = 0; i< layers.size(); i++)
		{
			layers.get(i).spriteFactory.invisibleAll();
			layers.get(i).textFactory.invisibleAll();
			layers.get(i).movieClipFactory.stopAll();
		}
		
	}
	private void displayLevelTabs()
	{
		findLayerById(101).spriteFactory.setVisible("LTAB1", true);
		findLayerById(101).spriteFactory.setVisible("LTAB1HOT", false);

		findLayerById(101).spriteFactory.setVisible("LTAB2", true);
		findLayerById(101).spriteFactory.setVisible("LTAB2HOT", false);

		findLayerById(101).spriteFactory.setVisible("LTAB3", true);
		findLayerById(101).spriteFactory.setVisible("LTAB3HOT", false);
		
		// Turn off tab 1 stuff
    	findLayerById(91).spriteFactory.setVisible("MAP1", false);
		findLayerById(101).spriteFactory.setVisible("WAIT0", false);

    	findLayerById(101).movieClipFactory.stop("SUBLEVEL_TARGET");

		findLayerById(100).spriteFactory.setVisible("SELECTOR1_1", false);
		findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", false);
		findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_1_1", false);
		findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", false);
		findLayerById(101).spriteFactory.setVisible("PLAYALPHA", false);

		findLayerById(121).spriteFactory.setVisible("ARROW_LEFT", false);
		findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_ALPHA", false);
		findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT", false);
		findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_ALPHA", false);
		hideTownNames();
		
		findLayerById(91).spriteFactory.setVisible("MAP1MINI", false);
		findLayerById(121).spriteFactory.setVisible("TOWNNAME_1_1", false);
		
		// Turn off tab 2 stuff
		// Turn off tab 3 stuff

		switch(currentLevelTab)
		{
		case 0:
			findLayerById(101).spriteFactory.setVisible("LTAB1", false);
			findLayerById(101).spriteFactory.setVisible("LTAB1HOT", true);

	    	findLayerById(91).spriteFactory.setVisible("MAP1", true);
			findLayerById(101).spriteFactory.setVisible("WAIT0", false);

	    	findLayerById(101).movieClipFactory.play("SUBLEVEL_TARGET");

			findLayerById(100).spriteFactory.setVisible("SELECTOR1_1", true);
			findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", true);
			findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_1_1", true);
			findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", true);
			findLayerById(101).spriteFactory.setVisible("PLAYALPHA", true);

			findLayerById(121).spriteFactory.setVisible("ARROW_LEFT", false);
			findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_ALPHA", true);
			findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT", true);
			findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_ALPHA", false);
			
			findLayerById(121).spriteFactory.setVisible("TOWNNAME_1_1", true);

			break;
		case 1:
			findLayerById(101).spriteFactory.setVisible("LTAB2", false);
			findLayerById(101).spriteFactory.setVisible("LTAB2HOT", true);
			
			break;
		case 2:
			findLayerById(101).spriteFactory.setVisible("LTAB3", false);
			findLayerById(101).spriteFactory.setVisible("LTAB3HOT", true);
			
			findLayerById(91).spriteFactory.setVisible("MAP1MINI", true);
			
			break;
		}
	}
	private void displayMainMenuTabs() {
		findLayerById(101).spriteFactory.setVisible("TAB1", true);
		findLayerById(101).spriteFactory.setVisible("TAB1HOT", false);

		findLayerById(101).spriteFactory.setVisible("TAB2", true);
		findLayerById(101).spriteFactory.setVisible("TAB2HOT", false);

		findLayerById(101).spriteFactory.setVisible("TAB3", true);
		findLayerById(101).spriteFactory.setVisible("TAB3HOT", false);

		findLayerById(101).spriteFactory.setVisible("TAB4", true);
		findLayerById(101).spriteFactory.setVisible("TAB4HOT", false);

		// Turn off tab 1 stuff
		findLayerById(101).spriteFactory.setVisible("LEVEL1", false);
		findLayerById(101).spriteFactory.setVisible("LEVEL2", false);
		findLayerById(101).spriteFactory.setVisible("LEVEL3", false);
		findLayerById(101).spriteFactory.setVisible("LEVEL4", false);
		findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_LVL", false);
		findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_LVL", false);
		
		findLayerById(101).spriteFactory.setVisible("WAIT0", false);

		// Turn off tab 2 stuff
    	findLayerById(91).textFactory.setVisible("SHIELD_DISP", false);
    	findLayerById(91).textFactory.setVisible("BOMB_DISP", false);
    	findLayerById(91).textFactory.setVisible("SANDCLOCK_DISP", false);

		findLayerById(93).textFactory.setVisible("UPGRADE_SHIELD_PRICE", false);
		findLayerById(93).textFactory.setVisible("UPGRADE_BOMB_PRICE", false);
		findLayerById(93).textFactory.setVisible("UPGRADE_SANDCLOCK_PRICE", false);

		findLayerById(92).spriteFactory.setVisible("BTN_BUY_SHIELD", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_BOMB", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SANDCLOCK", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SHIELD_HOT", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_BOMB_HOT", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SANDCLOCK_HOT", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SURPRISECHEST", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SURPRISECHEST_HOT", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD_HOT", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB_HOT", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK", false);
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK_HOT", false);

    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL1", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL2", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL3", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL4", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL5", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL6", false);

    	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL1", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL2", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL3", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL4", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL5", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL6", false);

    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL1", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL2", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL3", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL4", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL5", false);
    	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL6", false);

    	findLayerById(91).spriteFactory.setVisible("BOOST_SHIELD", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_BOMB", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_SANDCLOCK", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_SURPRISECHEST", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_SHIELD_UPGRADE", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_BOMB_UPGRADE", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_SANDCLOCK_UPGRADE", false);

		// Turn off tab 3 stuff
    	findLayerById(91).spriteFactory.setVisible("STORE_MONEY_10000", false);
		findLayerById(91).spriteFactory.setVisible("STORE_MONEY_20000", false);
		findLayerById(91).spriteFactory.setVisible("STORE_MONEY_50000", false);
		findLayerById(91).spriteFactory.setVisible("STORE_MONEY_100000", false);
		findLayerById(91).spriteFactory.setVisible("STORE_GOLD_10", false);
		findLayerById(91).spriteFactory.setVisible("STORE_GOLD_20", false);
		findLayerById(91).spriteFactory.setVisible("STORE_GOLD_50", false);
		findLayerById(91).spriteFactory.setVisible("STORE_GOLD_100", false);

		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_10000", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_10000_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_20000", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_20000_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_50000", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_50000_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_100000", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_100000_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_10", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_10_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_20", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_20_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_50", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_50_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_100", false);
		findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_100_HOT", false);
		
		// Turn off tab 4 stuff
		findLayerById(91).spriteFactory.setVisible("BOOST_EARN_1000", false);
		findLayerById(91).spriteFactory.setVisible("BOOST_EARN_2GOLD", false);

		findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_1000", false);
		findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_1000_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_2GOLD", false);
		findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_2GOLD_HOT", false);

		switch(currentTab)
		{
		case 0:
			findLayerById(101).spriteFactory.setVisible("TAB1", false);
			findLayerById(101).spriteFactory.setVisible("TAB1HOT", true);

			if(!mapLoadedLevel1 && !mapLoadedAttempt1)
			{
				findLayerById(101).spriteFactory.setVisible("LEVEL1", true);
				findLayerById(101).spriteFactory.setVisible("LEVEL2", true);
				findLayerById(101).spriteFactory.setVisible("LEVEL3", true);
				findLayerById(101).spriteFactory.setVisible("LEVEL4", true);
				
				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_LVL", true);
				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_LVL", true);
			} else {
				findLayerById(101).spriteFactory.setVisible("LEVEL1", false);
				findLayerById(101).spriteFactory.setVisible("LEVEL2", false);
				findLayerById(101).spriteFactory.setVisible("LEVEL3", false);
				findLayerById(101).spriteFactory.setVisible("LEVEL4", false);

				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_LVL", false);
				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_LVL", false);
			}

			break;
		case 1:
			findLayerById(101).spriteFactory.setVisible("TAB2", false);
			findLayerById(101).spriteFactory.setVisible("TAB2HOT", true);

	    	findLayerById(91).textFactory.setVisible("SHIELD_DISP", true);
	    	findLayerById(91).textFactory.setVisible("BOMB_DISP", true);
	    	findLayerById(91).textFactory.setVisible("SANDCLOCK_DISP", true);

	    	findLayerById(93).textFactory.setVisible("UPGRADE_SHIELD_PRICE", true);
			findLayerById(93).textFactory.setVisible("UPGRADE_BOMB_PRICE", true);
			findLayerById(93).textFactory.setVisible("UPGRADE_SANDCLOCK_PRICE", true);

	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SHIELD", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_BOMB", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SANDCLOCK", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SHIELD_HOT", false);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_BOMB_HOT", false);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SANDCLOCK_HOT", false);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SURPRISECHEST", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_BUY_SURPRISECHEST_HOT", false);
	    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD_HOT", false);
	    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB_HOT", false);
	    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK", true);
	    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK_HOT", false);

	    	if(gameInfo.getShieldDuration()> 0) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL1", true); }
	    	if(gameInfo.getShieldDuration()> 1) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL2", true); }
	    	if(gameInfo.getShieldDuration()> 2) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL3", true); }
	    	if(gameInfo.getShieldDuration()> 3) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL4", true); }
	    	if(gameInfo.getShieldDuration()> 4) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL5", true); }
	    	if(gameInfo.getShieldDuration()> 5) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL6", true); }

	    	if(gameInfo.getBombPower()> 0) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL1", true); }
	    	if(gameInfo.getBombPower()> 1) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL2", true); }
	    	if(gameInfo.getBombPower()> 2) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL3", true); }
	    	if(gameInfo.getBombPower()> 3) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL4", true); }
	    	if(gameInfo.getBombPower()> 4) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL5", true); }
	    	if(gameInfo.getBombPower()> 5) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL6", true); }

	    	if(gameInfo.getSandclockDuration()> 0) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL1", true); }
	    	if(gameInfo.getSandclockDuration()> 1) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL2", true); }
	    	if(gameInfo.getSandclockDuration()> 2) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL3", true); }
	    	if(gameInfo.getSandclockDuration()> 3) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL4", true); }
	    	if(gameInfo.getSandclockDuration()> 4) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL5", true); }
	    	if(gameInfo.getSandclockDuration()> 5) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL6", true); }

	    	findLayerById(91).spriteFactory.setVisible("BOOST_SHIELD", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_BOMB", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_SANDCLOCK", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_SURPRISECHEST", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_SHIELD_UPGRADE", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_BOMB_UPGRADE", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_SANDCLOCK_UPGRADE", true);
			
			
			break;
		case 2:
			findLayerById(101).spriteFactory.setVisible("TAB3", false);
			findLayerById(101).spriteFactory.setVisible("TAB3HOT", true);

	    	findLayerById(91).spriteFactory.setVisible("STORE_MONEY_10000", true);
			findLayerById(91).spriteFactory.setVisible("STORE_MONEY_20000", true);
			findLayerById(91).spriteFactory.setVisible("STORE_MONEY_50000", true);
			findLayerById(91).spriteFactory.setVisible("STORE_MONEY_100000", true);
			findLayerById(91).spriteFactory.setVisible("STORE_GOLD_10", true);
			findLayerById(91).spriteFactory.setVisible("STORE_GOLD_20", true);
			findLayerById(91).spriteFactory.setVisible("STORE_GOLD_50", true);
			findLayerById(91).spriteFactory.setVisible("STORE_GOLD_100", true);

			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_10000", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_10000_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_20000", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_20000_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_50000", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_50000_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_100000", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_100000_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_10", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_10_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_20", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_20_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_50", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_50_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_100", true);
			findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_100_HOT", false);

			break;
		case 3:
			findLayerById(101).spriteFactory.setVisible("TAB4", false);
			findLayerById(101).spriteFactory.setVisible("TAB4HOT", true);

	    	findLayerById(91).spriteFactory.setVisible("BOOST_EARN_1000", true);
			findLayerById(91).spriteFactory.setVisible("BOOST_EARN_2GOLD", true);
			findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_1000", true);
			findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_1000_HOT", false);
			findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_2GOLD", true);
			findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_2GOLD_HOT", false);

			break;
		}
	}

	private Layer findLayerById(int id) {
		for(int i = 0; i< layers.size(); i++)
		{
			if(layers.get(i).id == id)
			{
				return layers.get(i); 
			}
		}

		return null;
	}
	private Layer findLayerById(int id, boolean createIfNotExists) {
		for(int i = 0; i< layers.size(); i++)
		{
			if(layers.get(i).id == id)
			{
				return layers.get(i); 
			}
		}

		Layer result = null; 
		if(createIfNotExists)
		{
			result = new Layer(id);
			layers.add(result);
		}
		
		return result;
	}
	public void sortLayers()
	{
		for(int i = 0; i< layers.size(); i++)
		{
			for(int j = i+ 1; j< layers.size(); j++)
			{
				if(layers.get(i).id> layers.get(j).id){
					Layer l = layers.get(i);
					layers.set(i, layers.get(j));
					layers.set(j, l);
				}
			}
		}
	}

	private boolean loadStage(String fileName) {
		Layer l = null;

		switch(loadingStep)
		{
		case LOADINGSTEP_PREPARE:
			stageInfo = new StageInfo("stages/"+ fileName, context);
			l = findLayerById(0);
			l.spriteFactory.add("BACKGROUND", new Sprite(stageInfo.getBackgroundImage(), m_width, (int)Math.floor(600* m_ratio), gl, context, m_height));

			loadingObjectCount = stageInfo.getObjects().size();
			loadingBuildingCount = stageInfo.getBuildings().size();
			loadingCharacterCount = stageInfo.getCharacters().size();
			
			loadingObjectCurrent = 0;
			loadingBuildingCurrent = 0;
			loadingCharacterCurrent = 0;

			loadingStep = LOADINGSTEP_OBJECTS;
			loadingProgress = 0;

			break;
		case LOADINGSTEP_OBJECTS:
			l = findLayerById(stageInfo.getObjects().get(loadingObjectCurrent).getLayer());
			if(l == null)
			{
				l = new Layer(stageInfo.getObjects().get(loadingObjectCurrent).getLayer());
				layers.add(l);
			}

			l.spriteFactory.add(stageInfo.getObjects().get(loadingObjectCurrent).getId(), new Sprite(stageInfo.getObjects().get(loadingObjectCurrent).visuals.get(0).getFileName(), (int)(stageInfo.getObjects().get(loadingObjectCurrent).visuals.get(0).getWidth()* m_ratio), (int)(stageInfo.getObjects().get(loadingObjectCurrent).visuals.get(0).getHeight()* m_ratio), gl, context, m_height));
			l.spriteFactory.setVisible(stageInfo.getObjects().get(loadingObjectCurrent).getId(), true);
			l.spriteFactory.setPosition(stageInfo.getObjects().get(loadingObjectCurrent).getId(), (int)Math.floor(stageInfo.getObjects().get(loadingObjectCurrent).getPosX()* m_ratio), (int)Math.floor(stageInfo.getObjects().get(loadingObjectCurrent).getPosY()* m_ratio));

			loadingObjectCurrent++;
			loadingProgress = (int)Math.floor(784* (loadingObjectCurrent* 1.0f)/ (loadingObjectCount+ loadingBuildingCount+ loadingCharacterCount));

			if(loadingObjectCurrent >= loadingObjectCount)
			{
				loadingStep = LOADINGSTEP_BUILDINGS;
			}
			
			break;
		case LOADINGSTEP_BUILDINGS:
			l = findLayerById(stageInfo.getBuildings().get(loadingBuildingCurrent).getLayer());
			if(l == null)
			{
				l = new Layer(stageInfo.getBuildings().get(loadingBuildingCurrent).getLayer());
				layers.add(l);
			}

			// visuals
			for(int j = 0; j< stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.size(); j++)
			{
				Sprite sprite = new Sprite(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getFileName(), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getWidth()* m_ratio), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getHeight()* m_ratio), gl, context, m_height);
				stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).setSprite(sprite);

				if(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getAfterShot() == 0)
				{
					//timeLineLog += stageInfo.getBuildings().get(loadingBuildingCurrent).getId()+ " :: "+ stageInfo.getBuildings().get(loadingBuildingCurrent).getLayer()+ "\n";
					l.spriteFactory.add(stageInfo.getBuildings().get(loadingBuildingCurrent).getId(), sprite);
					l.spriteFactory.setVisible(stageInfo.getBuildings().get(loadingBuildingCurrent).getId(), true);
					l.spriteFactory.setPosition(stageInfo.getBuildings().get(loadingBuildingCurrent).getId(), (int)Math.floor(stageInfo.getBuildings().get(loadingBuildingCurrent).getPosX()* m_ratio), (int)Math.floor(stageInfo.getBuildings().get(loadingBuildingCurrent).getPosY()* m_ratio));
				}

				sprite = new Sprite(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getHit(), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getWidth()* m_ratio), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getHeight()* m_ratio), gl, context, m_height);
				stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).setHitSprite(sprite);
				//timeLineLog += stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(j).getHit()+ "\n";
			}
			
			// destruction animation
			for(int j = 0; j< stageInfo.getBuildings().get(loadingBuildingCurrent).desctruction.size(); j++)
			{
				Sprite sprite = new Sprite(stageInfo.getBuildings().get(loadingBuildingCurrent).desctruction.get(j).getFileName(), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(0).getWidth()* m_ratio), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(0).getHeight()* m_ratio), gl, context, m_height);
				stageInfo.getBuildings().get(loadingBuildingCurrent).desctruction.get(j).setSprite(sprite);
			}

			// after destruction visual 
	    	Sprite sprite = new Sprite(stageInfo.getBuildings().get(loadingBuildingCurrent).getAfterDestruction(), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(0).getWidth()* m_ratio), (int)(stageInfo.getBuildings().get(loadingBuildingCurrent).visuals.get(0).getHeight()* m_ratio), gl, context, m_height);
	    	l.spriteFactory.add(stageInfo.getBuildings().get(loadingBuildingCurrent).getId()+ "_DESTROYED", sprite);
	    	l.spriteFactory.setVisible(stageInfo.getBuildings().get(loadingBuildingCurrent).getId()+ "_DESTROYED", false);
			
			loadingBuildingCurrent++;
			loadingProgress = (int)Math.floor(784* ((loadingObjectCurrent+ loadingBuildingCurrent)* 1.0f)/ (loadingObjectCount+ loadingBuildingCount+ loadingCharacterCount));

			if(loadingBuildingCurrent >= loadingBuildingCount)
			{
				loadingStep = LOADINGSTEP_CHARACTERS;
			}

			break;
		case LOADINGSTEP_CHARACTERS:
			for(int j = 0; j< stageInfo.getCharacters().get(loadingCharacterCurrent).getMovements().size(); j++)
			{
				for(int h = 0; h< stageInfo.getCharacters().get(loadingCharacterCurrent).getMovements().get(j).getKeyframes().size(); h++)
				{
					sprite = new Sprite(stageInfo.getCharacters().get(loadingCharacterCurrent).getMovements().get(j).getKeyframes().get(h).fileName,
							(int)Math.floor(stageInfo.getCharacters().get(loadingCharacterCurrent).getMovements().get(j).getWidth()* m_ratio),
							(int)Math.floor(stageInfo.getCharacters().get(loadingCharacterCurrent).getMovements().get(j).getHeight()* m_ratio),
							gl, context, m_height);

					stageInfo.getCharacters().get(loadingCharacterCurrent).getMovements().get(j).getKeyframes().get(h).setSprite(sprite);
				}
			}
			
			loadingCharacterCurrent++;
			loadingProgress = (int)Math.floor(784* ((loadingCharacterCurrent+ loadingObjectCurrent+ loadingBuildingCurrent)* 1.0f)/ (loadingObjectCount+ loadingBuildingCount+ loadingCharacterCount));

			if(loadingCharacterCurrent >= loadingCharacterCount)
			{
				currWeapon = WEAPON_REVOLVER;
				lastShootAt = getClockTick();
				setLives();

				loadingStep = LOADINGSTEP_PREPARE;
				return true;
			}
			
			break;
		}
		
		return false;
	}
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		this.gl = gl;
		if(!texturesCreated)
		{
			createTextures(gl);
			texturesCreated = true;
			setGameMode(GAMEMODE_LOADTEXTURES_PREPARE);
		}
		
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		
		gl.glEnable(GL10.GL_ALPHA_TEST );
		gl.glAlphaFunc(GL10.GL_GREATER, 0 );
	    gl.glEnable(GL10.GL_BLEND);
	    gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); 
	}
	
    public void onDrawFrame(GL10 gl) {
		if(levelLoaded)
		{
			int c = Color.parseColor(stageInfo.getBackgroundColor());
			gl.glClearColor(((c>>16)&0xff)/ 255.0f, ((c>>8)&0xff)/ 255.0f, (c&0xFF)/ 255.0f, 1.0f);
	        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		}

		if(goldToSet != -1)
		{
			setGold(goldToSet, findLayerById(131).textFactory.getVisible("GOLD"));
			goldToSet = -1;
		}

		if(sandclocksToSet != -1)
		{
			findLayerById(91).textFactory.setMessage("SANDCLOCK_DISP", Integer.toString(sandclocksToSet));
			gameInfo.setSandclocks(sandclocksToSet);

			sandclocksToSet = -1;
		}

		if(shieldsToSet != -1)
		{
    		findLayerById(91).textFactory.setMessage("SHIELD_DISP", ""+ Integer.toString(shieldsToSet));
    		gameInfo.setShields(shieldsToSet);

    		shieldsToSet = -1;
		}
		
		if(bombsToSet != -1)
		{
    		findLayerById(91).textFactory.setMessage("BOMB_DISP", Integer.toString(bombsToSet));
    		gameInfo.setBombs(bombsToSet);

    		bombsToSet = -1;
		}

		if(bombPowerToSet != -1)
		{
			int price = 500;
			switch(gameInfo.getBombPower()+ 1)
			{
			case 1: price = 1000; break;
			case 2: price = 2000; break;
			case 3: price = 5000; break;
			case 4: price = 10000; break;
			case 5: price = 20000; break;
			}

			findLayerById(93).textFactory.setMessage("UPGRADE_BOMB_PRICE", ""+ price);

        	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL1", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL2", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL3", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL4", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL5", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL6", false);

        	if(bombPowerToSet> 0) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL1", true); }
        	if(bombPowerToSet> 1) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL2", true); }
        	if(bombPowerToSet> 2) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL3", true); }
        	if(bombPowerToSet> 3) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL4", true); }
        	if(bombPowerToSet> 4) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL5", true); }
        	if(bombPowerToSet> 5) { findLayerById(92).spriteFactory.setVisible("PRGRSS_BOMB_LVL6", true); }

			gameInfo.setBombPower(bombPowerToSet);
			bombPowerToSet = -1;
		}

		if(shieldDurationToSet != -1)
		{
			int price = 500;
			switch(gameInfo.getShieldDuration()+ 1)
			{
			case 1: price = 1000; break;
			case 2: price = 2000; break;
			case 3: price = 5000; break;
			case 4: price = 10000; break;
			case 5: price = 20000; break;
			}

			findLayerById(93).textFactory.setMessage("UPGRADE_SHIELD_PRICE", ""+ price);

        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL1", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL2", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL3", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL4", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL5", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL6", false);

        	if(shieldDurationToSet> 0) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL1", true); }
        	if(shieldDurationToSet> 1) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL2", true); }
        	if(shieldDurationToSet> 2) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL3", true); }
        	if(shieldDurationToSet> 3) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL4", true); }
        	if(shieldDurationToSet> 4) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL5", true); }
        	if(shieldDurationToSet> 5) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SHIELD_LVL6", true); }

        	gameInfo.setShieldDuration(shieldDurationToSet);
        	shieldDurationToSet = -1;
		}

		if(sandclockDurationToSet != -1)
		{
			int price = 500;
			switch(gameInfo.getSandclockDuration()+ 1)
			{
			case 1: price = 1000; break;
			case 2: price = 2000; break;
			case 3: price = 5000; break;
			case 4: price = 10000; break;
			case 5: price = 20000; break;
			}

			findLayerById(93).textFactory.setMessage("UPGRADE_SANDCLOCK_PRICE", ""+ price);

        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL1", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL2", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL3", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL4", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL5", false);
        	findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL6", false);

        	if(sandclockDurationToSet> 0) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL1", true); }
        	if(sandclockDurationToSet> 1) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL2", true); }
        	if(sandclockDurationToSet> 2) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL3", true); }
        	if(sandclockDurationToSet> 3) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL4", true); }
        	if(sandclockDurationToSet> 4) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL5", true); }
        	if(sandclockDurationToSet> 5) { findLayerById(92).spriteFactory.setVisible("PRGRSS_SANDCLOCK_LVL6", true); }

        	gameInfo.setSandclockDuration(sandclockDurationToSet);
        	sandclockDurationToSet = -1;
		}

		if(moneyToSet != -1)
		{
			setMoney(moneyToSet, findLayerById(131).textFactory.getVisible("MONEY"));
			moneyToSet = -1;
		}
		
		if(scoreToSet != -1)
		{
			setScore(score);
			scoreToSet = -1;
		}
		
		if(livesToSet != -1)
		{
			setLives(livesToSet);
			livesToSet = -1;
		}

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
        this.gl = gl;

        gl.glLoadIdentity();

        switch(GameMode)
        {
	        case GAMEMODE_COMPANY:
	        	if(tapScreenStart+ 3000 <= getClockTick())
				{
					findLayerById(100).spriteFactory.setVisible("TOLKATAP", true);
				}

				break;
	        case GAMEMODE_INTRO:
				findLayerById(100).spriteFactory.setVisible("TOLKATAP", false);
				if(tapScreenStart+ 1000 <= getClockTick())
				{
			    	findLayerById(100).spriteFactory.setVisible("INTRO_TITLE", true);
				}

				if(tapScreenStart+ 3000 <= getClockTick())
				{
			    	findLayerById(100).spriteFactory.setVisible("INTRO_TAP", true);
				}

				if(tapScreenStart+ 4000 <= getClockTick())
				{
			    	findLayerById(100).spriteFactory.setVisible("INTRO_TAP", false);
				}

				if(tapScreenStart+ 5000 <= getClockTick())
				{
			    	findLayerById(100).spriteFactory.setVisible("INTRO_TAP", true);
				}

				if(tapScreenStart+ 6000 <= getClockTick())
				{
			    	findLayerById(100).spriteFactory.setVisible("INTRO_TAP", false);
				}

				if(tapScreenStart+ 7000 <= getClockTick())
				{
			    	findLayerById(100).spriteFactory.setVisible("INTRO_TAP", true);
				}

	        case GAMEMODE_INTRO_SELECTGAME:
		    	//findLayerById(100).spriteFactory.setVisible("INTRO_TITLE", false);
		    	//findLayerById(100).spriteFactory.setVisible("INTRO_TAP", false);

		    	break;
	        case GAMEMODE_INTRO_SELECTLEVEL:
	        	break;
			case GAMEMODE_LOAD_LEVEL:
		    	findLayerById(100).spriteFactory.setVisible("INTRO_TITLE", false);
		    	findLayerById(100).spriteFactory.setVisible("INTRO_TAP", false);

				break;
			case GAMEMODE_PLAY_LEVEL1:
				if(!levelLoaded)
				{
			    	currLives = maxLives;
					setScore(0);
					setLives(currLives);
					currFoe = 0;
					targetFoe = stageInfo.getTargetFoe();
					sortLayers();
					
					findLayerById(0).spriteFactory.setVisible("BACKGROUND", true);
					findLayerById(100).spriteFactory.setVisible("INTRO", false);
					findLayerById(101).spriteFactory.setVisible("CROSSAIR", true);

					levelLoaded = true;
				}
				
				//timeLineLog += findLayerById(10).spriteFactory.getVisible("saloon1")?"visible":"NOT visible";
				int c = Color.parseColor(stageInfo.getBackgroundColor());
				gl.glClearColor(((c>>16)&0xff)/ 255.0f, ((c>>8)&0xff)/ 255.0f, (c&0xFF)/ 255.0f, 1.0f);
		        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		        break;
        }

        try {
			drawGraphics(gl);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
    
    private void handleTimeLine() {
		// Determine current ms
    	if(lastClockTick == -1)
    	{
    		lastClockTick = getClockTick();
    	}
    	
    	long diff = getClockTick()- lastClockTick;
    	lastClockTick = getClockTick();
    	if(sandclockMode)
    	{
    		diff /= 2;
    	}
    	
    	for(int i = 0; i< stageInfo.getFrames().size(); i++)
    	{
    		int time = stageInfo.getFrames().get(i).getTime();
    		if(time >= currFrame && time <= currFrame+ diff)
    		{
    			// process frame characters
				for(int k = 0; k< stageInfo.getFrames().get(i).getCharacters().size(); k++)
				{
					FrameCharacter character = stageInfo.getFrames().get(i).getCharacters().get(k);
					
					for(int q = 0; q< stageInfo.getCharacters().size(); q++)
					{
		    			//timeLineLog += "processing character "+ character.getId() + "\n";
						com.tolka.shooterdudes.StageInfo.Character charInfo = stageInfo.getCharacters().get(q);
						if(charInfo.getId().equals(character.getId()))
						{
							executeAction(character, charInfo);
							break;
						}
					}
				}

				// process frame commands
				for(int k = 0; k< stageInfo.getFrames().get(i).getCommands().size(); k++)
				{
					FrameCommand command = stageInfo.getFrames().get(i).getCommands().get(k);
					
					switch(command.getType())
					{
					case "gotoTime":
						currFrame = command.getTarget();
						diff = 0;
						break;
					}
				}
    		}
    	}

    	currFrame += diff;
	}

    private void executeAction(final FrameCharacter character, final com.tolka.shooterdudes.StageInfo.Character charInfo) {
		int u = character.getCurrentAction();
		final int appearX = character.getCurrentX();
		final int appearY = character.getCurrentY();

		if(u >= character.getActions().size())
		{
			timeLineLog += "\n"+ u+ " is larger than size "+ character.getActions().size()+ " ("+ character.getId() +")\n";
			character.toInitialState();
			return;
		}

		FrameCharacterAction action = character.getActions().get(u);
		if(action.getWalkSteps() < 0)
		{
			action.setDirection(-1);
		} else {
			action.setDirection(+1);
		}

		switch(action.getActionType())
		{
		case Fire:
			if(currLives <= 0)
			{
				character.setCurrentAction(character.getCurrentAction() + 1);
				executeAction(character, charInfo);
				break;
			}

			//timeLineLog += "fire prepare for "+ character.getId()+ "\n";
			for(int g = 0; g< charInfo.getMovements().size(); g++)
			{
				if(charInfo.getMovements().get(g).getType().equals("fire prepare"))
				{
			    	Layer layer = findLayerById(charInfo.getLayer(), true);

            		final MovieClip clip = new MovieClip(action.getFireWait()/ charInfo.getMovements().size());
					for(int r = 0; r< charInfo.getMovements().get(g).getKeyframes().size(); r++)
					{
						Sprite sprite = charInfo.getMovements().get(g).getKeyframes().get(r).getSprite();
						sprite.setFlip(false);
						if(action.getDirection()< 0)
						{
							sprite.setFlip(true);
							//sprite.flip();
						}

						clip.getSpriteFactory().add(character.getId()+ "_FIRE_PREPARE_"+ r, sprite);
					}

			    	clip.setRepetition(1);

			    	layer.movieClipFactory.add(character.getId()+ "_FIRE_PREPARE", clip);
			    	layer.movieClipFactory.setPosition(character.getId()+ "_FIRE_PREPARE", (int)Math.floor(appearX* m_ratio), (int)Math.floor(appearY* m_ratio));
			    	layer.movieClipFactory.play(character.getId()+ "_FIRE_PREPARE");
			    	
					//timeLineLog += "Fire prepare for "+ charInfo.getId()+ " starts at "+ (int)Math.floor(appearX* m_ratio)+ "\n";
			    	RunAfterFunction f = new RunAfterFunction() {
		                @Override
			    		public void run()
			    		{
		                	if(character.getCurrentAction()+ 1 >= character.getActions().size())
		                	{
		            			character.toInitialState();
		                	} else {
		            			for(int g = 0; g< charInfo.getMovements().size(); g++)
		            			{
		            				if(charInfo.getMovements().get(g).getType().equals("fire shoot"))
		            				{
		                        		final MovieClip clip = new MovieClip(charInfo.getMovements().get(g).getFrameDelay());
		            			    	Layer layer = findLayerById(charInfo.getLayer(), true);

		            					for(int r = 0; r< charInfo.getMovements().get(g).getKeyframes().size(); r++)
		            					{
		            						Sprite sprite = charInfo.getMovements().get(g).getKeyframes().get(r).getSprite();
		            						clip.getSpriteFactory().add(character.getId()+ "_FIRE_SHOOT_"+ r, sprite);
		            					}

		            			    	clip.setRepetition(1);

		            			    	if(shieldMode)
		            			    	{
			            					MediaPlayer mp = MediaPlayer.create(context, R.raw.ricochet);
			            			        mp.setOnCompletionListener(new OnCompletionListener() {
			            			            @Override
			            			            public void onCompletion(MediaPlayer mp) {
			            			                mp.release();
			            			            }
	
			            			        });
			            			        mp.start();

			            			        findLayerById(121).spriteFactory.setVisible("NODAMAGE", true);
	    		                	    	findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(121, "NODAMAGE"), 1.0f, 0.0f, 300.0), 0, -1);
		            			    	} else {
			            					MediaPlayer mp = MediaPlayer.create(context, R.raw.shotgun);
			            			        mp.setOnCompletionListener(new OnCompletionListener() {
			            			            @Override
			            			            public void onCompletion(MediaPlayer mp) {
			            			                mp.release();
			            			            }
	
			            			        });
			            			        mp.start();

			            			        findLayerById(121).spriteFactory.setVisible("DAMAGE", true);
	    		                	    	findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(121, "DAMAGE"), 1.0f, 0.0f, 300.0), 0, -1);
			    							livesToSet = --currLives;
	    		    						if(currLives == 0)
	    		    						{
	    		    							//currLives = 0;
	    		    							
	    		    							// level lost
	        		                	    	//findLayerById(121).spriteFactory.setAlpha("GAMEEND", 0.1f);
	        		                	    	findLayerById(121).spriteFactory.setVisible("GAMEEND", true);
	        		                	    	findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(121, "GAMEEND"), 0.0f, 1.0f, 2000.0), 0, -1);
	    		    							//return;
	        		                	    	//findLayerById(122).spriteFactory.setVisible("BGWOODS", true);
	        		                	    	//findLayerById(122).spriteFactory.setPosition("BGWOODS", 0.0f, -680.0f * m_ratio);
	        		                	    	//findLayerById(122).tweenFactory.add(new MotionTween(MotionTween.ATTR_Y, findSpriteById(122, "BGWOODS"), -680.0f * m_ratio, 0.0f, 1000.0), 2000, -1);
	        		                	    	hasEnded = true;
	        		                	    	findLayerById(122).spriteFactory.setVisible("GAMELOST", true);
	        		                	    	findLayerById(122).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(122, "GAMELOST"), 0.0f, 1.0f, 2000.0), 0, -1);
	    		    						}
		            			    	}

    		                	    	layer.movieClipFactory.add(character.getId()+ "_FIRE_SHOOT", clip);
				    			    	layer.movieClipFactory.setPosition(character.getId()+ "_FIRE_SHOOT", (int)Math.floor(appearX* m_ratio), (int)Math.floor(appearY* m_ratio));
				    			    	layer.movieClipFactory.play(character.getId()+ "_FIRE_SHOOT");
		
				    			    	RunAfterFunction ra = new RunAfterFunction() {
				    		                @Override
				    			    		public void run()
				    			    		{
		    		                	    	findLayerById(121).spriteFactory.setVisible("DAMAGE", false);
		    		                	    	findLayerById(121).spriteFactory.setVisible("NODAMAGE", false);
				    		                	
				    		        			if(character.getCurrentAction()+ 1< character.getActions().size())
				    		        			{
				    		        				character.setCurrentAction(character.getCurrentAction()+ 1);
				    		        				executeAction(character, charInfo);
				    		        			} else {
				    		        				character.toInitialState();
				    		        			}
				    			    		}
				    			    	};
		
			    		                layer.movieClipFactory.runAfterPlay(character.getId()+ "_FIRE_SHOOT", ra);
		            				}
		            			}
		                	}
			    		}
			    	};

			    	layer.movieClipFactory.runAfterPlay(character.getId()+ "_FIRE_PREPARE", f);
			    	break;
				}
			}
			break;
		case Walk:
			for(int g = 0; g< charInfo.getMovements().size(); g++)
			{
				if(charInfo.getMovements().get(g).getType().equals("walk"))
				{
					MovieClip clip = new MovieClip(charInfo.getMovements().get(g).getFrameDelay());
			    	Layer layer = findLayerById(charInfo.getLayer(), true);

					for(int r = 0; r< charInfo.getMovements().get(g).getKeyframes().size(); r++)
					{
						Sprite sprite = charInfo.getMovements().get(g).getKeyframes().get(r).getSprite();
						sprite.setFlip(false);
						if(action.getDirection()< 0)
						{
							sprite.setFlip(true);
						}

						clip.getSpriteFactory().add(character.getId()+ "_WALK_"+ r, sprite);
						double firstX = Math.floor(appearX* m_ratio);
						double lastX = Math.floor((appearX+ charInfo.getMovements().get(g).getStepSize()* action.getWalkSteps())* m_ratio* sprite.getScaleX());
						double duration = Math.abs(charInfo.getMovements().get(g).getKeyframes().size()* action.getWalkSteps()* charInfo.getMovements().get(g).getFrameDelay());
						layer.tweenFactory.add(new MotionTween(MotionTween.ATTR_X, sprite, firstX, lastX, duration), 0, -1);
					}

			    	clip.setRepetition(Math.abs(action.getWalkSteps()));

			    	layer.movieClipFactory.add(character.getId()+ "_WALK", clip);
			    	layer.movieClipFactory.setPosition(character.getId()+ "_WALK", (int)Math.floor(appearX* m_ratio), (int)Math.floor(appearY* m_ratio));
			    	layer.movieClipFactory.play(character.getId()+ "_WALK");
			    	
			    	final int _appearX = (int)Math.floor(appearX+ charInfo.getMovements().get(g).getStepSize()* action.getWalkSteps());
			    	final int _appearY = appearY;

					RunAfterFunction f = new RunAfterFunction() {
		                @Override
			    		public void run()
			    		{
		                	//character.getCurrentAction()
		                	if(character.getCurrentAction()+ 1 >= character.getActions().size())
		                	{
		            			character.toInitialState();
		                	} else {
		                		character.setCurrentPosition(_appearX, _appearY);
		                		character.setCurrentAction(character.getCurrentAction()+ 1);
		                		executeAction(character, charInfo);
		                	}
			    		}
			    	};
			    	
			    	layer.movieClipFactory.runAfterPlay(character.getId()+ "_WALK", f);
					break;
				}
			}
			
			break;
		case None:
			break;
		}
	}

    public void setGold(int gold)
    {
    	setGold(gold, true);
    }
    public void setGold(int gold, boolean display)
    {
    	this.gameInfo.setGold(gold);
    	
		String text = String.format("%03d", Integer.parseInt(Integer.toString(gold)));
		Layer uiLayer = findLayerById(131);
		if(uiLayer == null)
		{
			return;
		}

		uiLayer.textFactory.setMessage("GOLD", text);
		uiLayer.textFactory.setVisible("GOLD", display);
    }
    public void setMoney(int money)
    {
    	setMoney(money, true);
    }
    public void setMoney(int money, boolean display)
    {
    	this.gameInfo.setMoney(money);
    	
		String text = String.format("%06d", Integer.parseInt(Integer.toString(money)));
		Layer uiLayer = findLayerById(131);
		if(uiLayer == null)
		{
			return;
		}

		uiLayer.textFactory.setMessage("MONEY", text);
		uiLayer.textFactory.setVisible("MONEY", display);
    }
    public void setScore(int score)
    {
    	setScore(score, true);
    }
	public void setScore(int score, boolean display)
    {
    	this.score = score;
    	
		String text = String.format("%07d", Integer.parseInt(Integer.toString(score)));
		Layer uiLayer = findLayerById(99);
		if(uiLayer == null)
		{
			return;
		}
		
		uiLayer.textFactory.setMessage("SCORE", text);
		uiLayer.textFactory.setVisible("SCORE", display);
    }
	public void setLives(int lives)
	{
		Layer uiLayer = findLayerById(99);
		if(uiLayer == null)
		{
			return;
		}
		
		for(int i = maxLives; i > lives; i--)
		{
			uiLayer.spriteFactory.setVisible("LIVES_"+ i + "_FULL", false);
			uiLayer.spriteFactory.setVisible("LIVES_"+ i + "_EMPTY", true);
		}
	}
    public void setLives()
    {
		Layer uiLayer = findLayerById(99);
		if(uiLayer == null)
		{
			return;
		}
		
    	for(int i = 0; i< maxLives; i++)
    	{
    		String drawable = "ui_heart_full.png";
			Sprite sprite = new Sprite("ui/" + drawable, (int)(22* m_ratio), (int)(22* m_ratio), gl, context, m_height);
	    	sprite.setPosition((int)((20+ i* 24)* m_ratio), (int)(96* m_ratio));
	    	
	    	if(!uiLayer.spriteFactory.add("LIVES_"+ (i + 1) + "_FULL", sprite))
	    	{
	    		uiLayer.spriteFactory.setSprite("LIVES_"+ (i + 1) + "_FULL", sprite);
	    	}

	    	uiLayer.spriteFactory.setVisible("LIVES_"+ (i + 1) + "_FULL", true);
    	}
		
    	for(int i = 0; i< maxLives; i++)
    	{
    		String drawable = "ui_heart_empty.png";
			Sprite sprite = new Sprite("ui/" + drawable, (int)(22* m_ratio), (int)(22* m_ratio), gl, context, m_height);
	    	sprite.setPosition((int)((20+ i* 24)* m_ratio), (int)(96* m_ratio));
	    	
	    	if(!uiLayer.spriteFactory.add("LIVES_"+ (i + 1) + "_EMPTY", sprite))
	    	{
	    		uiLayer.spriteFactory.setSprite("LIVES_"+ (i + 1) + "_EMPTY", sprite);
	    	}

	    	uiLayer.spriteFactory.setVisible("LIVES_"+ (i + 1) + "_EMPTY", false);
    	}
    }
    public void displayScore()
    {
		String text = String.format("%07d", Integer.parseInt(Integer.toString(score)));
		Layer uiLayer = findLayerById(99);
		for(int i = 0; i< text.length(); i++)
		{
	    	uiLayer.spriteFactory.setVisible("SCORE_"+ i, true);
		}    	
    }
    public void hideScore()
    {
		String text = String.format("%07d", Integer.parseInt(Integer.toString(score)));
		Layer uiLayer = findLayerById(99);
		for(int i = 0; i< text.length(); i++)
		{
	    	uiLayer.spriteFactory.setVisible("SCORE_"+ i, false);
		}    	
    }
	public void onSurfaceChanged(GL10 gl, int width, int height) {
    	height = (height == 0) ? 1 : height;

		if(!shouldReloadTextures)
		{
			return;
		}
		
    	zeroLayers();
		
		textureCountLoaded = 0;
		createTextures(gl);
		//setGameMode(GAMEMODE_LOADTEXTURES_PREPARE);

		setGameMode(GameMode);
		
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		
		gl.glEnable(GL10.GL_ALPHA_TEST );
		gl.glAlphaFunc(GL10.GL_GREATER, 0 );
	    gl.glEnable(GL10.GL_BLEND);
	    gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);

	    shouldReloadTextures = false;

		gl.glViewport(0, 0, m_width, m_height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glOrthof (0, width, 0, height, -1f, 1f);
        try {
			drawGraphics(gl);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

	private boolean hasEnded = false;
	
	private void restartGame()
	{
		hasEnded = false;
    	GameMode = GAMEMODE_LOAD_LEVEL;
    	currFrame = 0;
    	findLayerById(121).spriteFactory.setVisible("GAMEEND", false);
    	findLayerById(122).spriteFactory.setVisible("GAMEWON", false);
    	findLayerById(122).spriteFactory.setVisible("GAMELOST", false);
	}	

	private void zeroLayers() {
		this.layers = new Vector<Layer>();
		this.layers.add(new Layer(0));	 // stage background
		this.layers.add(new Layer(90));  // frame bg
		this.layers.add(new Layer(91));  // level map
		this.layers.add(new Layer(92));  // Overlay buttons
		this.layers.add(new Layer(93));  // Overlay button texts
		this.layers.add(new Layer(99));  // user interface
		this.layers.add(new Layer(100)); // main menu
		this.layers.add(new Layer(101)); // crossair / ui elements
		this.layers.add(new Layer(103)); // ui over frame contents
		this.layers.add(new Layer(120)); // ui frame
		this.layers.add(new Layer(121)); // over map selector items
		this.layers.add(new Layer(122)); // death anim BG

		this.layers.add(new Layer(130)); // dark background
		this.layers.add(new Layer(131)); // dialog background
		this.layers.add(new Layer(132)); // dialog items

		this.layers.add(new Layer(900)); // debug data
	}


	protected int textureCountLoaded;
	protected int textureCountToLoad;

	private void drawGraphics(GL10 gl) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
		switch(GameMode)
		{
		case GAMEMODE_LOADTEXTURES_PREPARE:
			findLayerById(100).spriteFactory.setVisible("LOADINGFIRST", true);
			findLayerById(101).spriteFactory.setVisible("PROGRESS", true);

			textureCountLoaded = 0;
			loadingProgress = 0;

			setGameMode(GAMEMODE_LOADTEXTURES);
			break;
		case GAMEMODE_LOADTEXTURES:
			if(textureCountLoaded >= textureCountToLoad)
			{
				setGameMode(gameModeToSet);  // GAMEMODE_COMPANY;
				//timeLineLog = "Game mode is "+ GameMode;
				break;
			}
			
			int layer = texturesToLoad.get(textureCountLoaded).getLayer();
			String id = texturesToLoad.get(textureCountLoaded).getId();
			String fileName = texturesToLoad.get(textureCountLoaded).getFileName();
			int width = texturesToLoad.get(textureCountLoaded).getWidth();
			int height = texturesToLoad.get(textureCountLoaded).getHeight();
			int left = texturesToLoad.get(textureCountLoaded).getPosX();
			int top = texturesToLoad.get(textureCountLoaded).getPosY();
			boolean visible = texturesToLoad.get(textureCountLoaded).getVisible();

			addSpriteToLayer(layer, id, fileName, width, height, left, top, gl, visible);
			textureCountLoaded++;

			loadingProgress = (int)Math.floor(784* (textureCountLoaded* 1.0f)/ (textureCountToLoad));
			findLayerById(101).spriteFactory.setWidth("PROGRESS", (int)Math.floor(loadingProgress* m_ratio));
			break;
		case GAMEMODE_LOAD_LEVEL:
			//invisibleAllTextures();
	    	hideTownNames();

	    	findLayerById(100).spriteFactory.setVisible("LOADING", true);
	    	findLayerById(101).spriteFactory.setVisible("PROGRESS", true);
	    	findLayerById(103).spriteFactory.setVisible("SMALLOGO", true);

	    	if(loadStage("stage_1_1.xml"))
			{
		    	setGameMode(GAMEMODE_PLAY_LEVEL1);
			}

	    	findLayerById(101).spriteFactory.setWidth("PROGRESS", (int)Math.floor(loadingProgress* m_ratio));
			break;
		case GAMEMODE_PLAY_LEVEL1:
	    	findLayerById(100).spriteFactory.setVisible("LOADING", false);
	    	findLayerById(101).spriteFactory.setVisible("PROGRESS", false);
			findLayerById(0).spriteFactory.setVisible("BACKGROUND", true);
	    	findLayerById(103).spriteFactory.setVisible("SMALLOGO", false);

			handleTimeLine();
			handleUI();

			break;
		case GAMEMODE_INTRO_SELECTLEVEL:
			switch(currentTab)
			{
			case 1:
				if(autoScrollMode)
				{
					long time = getClockTick(); 
					if(time- boostScrollStart >= BOOST_SCROLL_DELAY)
					{
						autoScrollMode = false;
						boostDiff /= 2;
					
						if(boostDiff == 0)
						{
							break;
						}
	
	    				if((int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ boostDiff)> 130*m_ratio)
						{
	    					break;
						}
	    				
	    				if((int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ boostDiff)< -290*m_ratio)
						{
	    					break;
						}
	    				
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SHIELD", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SHIELD")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_BOMB", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_BOMB")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SANDCLOCK", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SANDCLOCK")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SHIELD_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SHIELD_HOT")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_BOMB_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_BOMB_HOT")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SANDCLOCK_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SANDCLOCK_HOT")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SURPRISECHEST", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SURPRISECHEST")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SURPRISECHEST_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SURPRISECHEST_HOT")+ boostDiff));

	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL1", (int)(283* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL1")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL2", (int)(348* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL2")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL3", (int)(413* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL3")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL4", (int)(478* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL4")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL5", (int)(543* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL5")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL6", (int)(608* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL6")+ boostDiff));

	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL1", (int)(283* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL1")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL2", (int)(348* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL2")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL3", (int)(413* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL3")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL4", (int)(478* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL4")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL5", (int)(543* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL5")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL6", (int)(608* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL6")+ boostDiff));

	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL1", (int)(283* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL1")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL2", (int)(348* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL2")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL3", (int)(413* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL3")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL4", (int)(478* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL4")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL5", (int)(543* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL5")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL6", (int)(608* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL6")+ boostDiff));

	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SHIELD", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SHIELD")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SHIELD_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SHIELD_HOT")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_BOMB", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_BOMB")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_BOMB_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_BOMB_HOT")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SANDCLOCK", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SANDCLOCK")+ boostDiff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SANDCLOCK_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SANDCLOCK_HOT")+ boostDiff));

	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SHIELD", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_BOMB", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SANDCLOCK", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SURPRISECHEST", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SURPRISECHEST")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SHIELD_UPGRADE", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD_UPGRADE")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_BOMB_UPGRADE", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB_UPGRADE")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SANDCLOCK_UPGRADE", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK_UPGRADE")+ boostDiff));
	    		    	
	    				findLayerById(93).textFactory.setPosition("UPGRADE_SHIELD_PRICE", (int)(843* m_ratio), (int)(findLayerById(93).textFactory.getY("UPGRADE_SHIELD_PRICE")+ boostDiff));
	    				findLayerById(93).textFactory.setPosition("UPGRADE_BOMB_PRICE", (int)(843* m_ratio), (int)(findLayerById(93).textFactory.getY("UPGRADE_BOMB_PRICE")+ boostDiff));
	    				findLayerById(93).textFactory.setPosition("UPGRADE_SANDCLOCK_PRICE", (int)(843* m_ratio), (int)(findLayerById(93).textFactory.getY("UPGRADE_SANDCLOCK_PRICE")+ boostDiff));

	    		    	findLayerById(91).textFactory.setPosition("SHIELD_DISP", (int)(405* m_ratio), (int)(findLayerById(91).textFactory.getY("SHIELD_DISP")+ boostDiff));
	    		    	findLayerById(91).textFactory.setPosition("BOMB_DISP", (int)(405* m_ratio), (int)(findLayerById(91).textFactory.getY("BOMB_DISP")+ boostDiff));
	    		    	findLayerById(91).textFactory.setPosition("SANDCLOCK_DISP", (int)(405* m_ratio), (int)(findLayerById(91).textFactory.getY("SANDCLOCK_DISP")+ boostDiff));

	    		    	boostScrollStart = getClockTick();
						autoScrollMode = true;
					}
				}
				
				break;
			case 2:
				if(autoScrollMode)
				{
					long time = getClockTick(); 
					if(time- boostScrollStart >= BOOST_SCROLL_DELAY)
					{
						autoScrollMode = false;
						boostDiff /= 2;
					
						if(boostDiff == 0)
						{
							break;
						}
	
	    				if((int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ boostDiff)> 130*m_ratio)
						{
	    					break;
						}

	    				if((int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ boostDiff)< -390*m_ratio)
						{
	    					break;
						}
	    				
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_10000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_20000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_20000")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_50000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_50000")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_100000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_100000")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_10", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_10")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_20", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_20")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_50", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_50")+ boostDiff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_100", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_100")+ boostDiff));

	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_10000", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_10000")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_10000_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_10000_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_20000", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_20000")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_20000_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_20000_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_50000", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_50000")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_50000_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_50000_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_100000", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_100000")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_100000_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_100000_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_10", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_10")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_10_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_10_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_20", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_20")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_20_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_20_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_50", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_50")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_50_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_50_HOT")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_100", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_100")+ boostDiff);
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_100_HOT", (int)(765* m_ratio), findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_100_HOT")+ boostDiff);

	    		    	boostScrollStart = getClockTick();
						autoScrollMode = true;
					}
				}
				
				break;
			case 3:
				if(autoScrollMode)
				{
					long time = getClockTick(); 
					if(time- boostScrollStart >= BOOST_SCROLL_DELAY)
					{
						autoScrollMode = false;
						boostDiff /= 2;
					
						if(boostDiff == 0)
						{
							break;
						}
	
				    	findLayerById(91).spriteFactory.setPosition("BOOST_EARN_1000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_EARN_1000")+ boostDiff));
						findLayerById(91).spriteFactory.setPosition("BOOST_EARN_2GOLD", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_EARN_2GOLD")+ boostDiff));

						findLayerById(92).spriteFactory.setPosition("BTN_CLAIM_1000", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_CLAIM_1000")+ boostDiff));
						findLayerById(92).spriteFactory.setPosition("BTN_CLAIM_1000_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_CLAIM_1000_HOT")+ boostDiff));
						findLayerById(92).spriteFactory.setPosition("BTN_CLAIM_2GOLD", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_CLAIM_2GOLD")+ boostDiff));
						findLayerById(92).spriteFactory.setPosition("BTN_CLAIM_2GOLD_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_CLAIM_2GOLD_HOT")+ boostDiff));

	    		    	boostScrollStart = getClockTick();
						autoScrollMode = true;
					}
				}

				break;
			}

			break;
		case GAMEMODE_INTRO_SELECTSUBLEVEL:
			switch(selectedLevel)
			{
			case -1:
				break;
			case 0:
				if(!mapLoadedLevel1)
				{
					findLayerById(101).spriteFactory.setVisible("WAIT0", true);
					
					if(mapLoadedAttempt1)
					{
						findLayerById(91).spriteFactory.setPosition("MAP1", (int)Math.floor((1100- 846)* m_ratio/ 2+ mapDiffLvl1X[selectedSublevel]* m_ratio), (int)Math.floor((644- 458+ 70)* m_ratio/ 2+ mapDiffLvl1Y[selectedSublevel]* m_ratio));

						MovieClip clip = new MovieClip(500);
				    	clip.getSpriteFactory().add("target0", findSpriteById(100, "TARGET0")); //new Sprite("target0.png", (int)(213* m_ratio), (int)(213* m_ratio), gl, context, m_height));
				    	clip.getSpriteFactory().add("target1", findSpriteById(100, "TARGET1")); //new Sprite("target1.png", (int)(213* m_ratio), (int)(213* m_ratio), gl, context, m_height));
				    	//clip.setPosition(100,  100);
				    	clip.setRepetition(-1);
				    	findLayerById(101).movieClipFactory.add("SUBLEVEL_TARGET", clip);
				    	findLayerById(101).movieClipFactory.setPosition("SUBLEVEL_TARGET", (int)(275* m_ratio), (int)(260* m_ratio));
				    	findLayerById(101).movieClipFactory.play("SUBLEVEL_TARGET");

				    	displayLevelTabs();

						findLayerById(101).spriteFactory.setVisible("TAB1", false);
						findLayerById(101).spriteFactory.setVisible("TAB1HOT", false);

						findLayerById(101).spriteFactory.setVisible("TAB2", false);
						findLayerById(101).spriteFactory.setVisible("TAB2HOT", false);

						findLayerById(101).spriteFactory.setVisible("TAB3", false);
						findLayerById(101).spriteFactory.setVisible("TAB3HOT", false);

						findLayerById(101).spriteFactory.setVisible("TAB4", false);
						findLayerById(101).spriteFactory.setVisible("TAB4HOT", false);

						mapLoadedLevel1 = true;
					} else
					{
						findLayerById(101).spriteFactory.setVisible("LEVEL1", false);
						findLayerById(101).spriteFactory.setVisible("LEVEL2", false);
						findLayerById(101).spriteFactory.setVisible("LEVEL3", false);
						findLayerById(101).spriteFactory.setVisible("LEVEL4", false);

						findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_LVL", false);
						findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_LVL", false);

						mapLoadedAttempt1 = true;
					}
				}

				break;
			case 1:
				break;
			case 2:
				break;
			case 3:
				break;
			}

			break;
		}

		sortLayers();
        for(int i = 0; i< layers.size(); i++)
		{
			layers.get(i).spriteFactory.draw();
			layers.get(i).tweenFactory.update(sandclockMode ? 2.0f : 1.0f);
			layers.get(i).movieClipFactory.update(sandclockMode ? 2.0f : 1.0f);
			layers.get(i).textFactory.draw();
		}
    }
	
	private void hideTownNames() {
    	for(int i = 1; i <= 35; i++)
    	{
        	findLayerById(121).spriteFactory.setVisible("TOWNNAME_1_"+ i, false);
    	}
	}

	private void handleUI() {
		// bullets
		Layer uiLayer = findLayerById(99);
		
		uiLayer.spriteFactory.setVisible("RELOAD", false);
		for(int i = 0; i< 6; i++)
		{
			uiLayer.spriteFactory.setVisible("BULLET"+ (i+ 1), true);
			uiLayer.spriteFactory.setAlpha("BULLET"+ (i+ 1), 0.30f);
		}
		
		for(int i = 0; i< 6; i++)
		{
			if(remainingShots > i)
			{
				uiLayer.spriteFactory.setAlpha("BULLET"+ (i+ 1), 1.0f);
			}
		}
		
		if(remainingShots <= 0)
		{
	        uiLayer.spriteFactory.setVisible("RELOAD", true);
		}
		
		// crossair
		findLayerById(101).spriteFactory.setVisible("CROSSAIR", currLives > 0);
		findLayerById(101).spriteFactory.setPosition("CROSSAIR", crossairX- 41* m_ratio, crossairY- 36* m_ratio);
		
		// boosts
		uiLayer.spriteFactory.setVisible("UISHIELD", true);
		uiLayer.spriteFactory.setVisible("UIBOMB", true);
		uiLayer.spriteFactory.setVisible("UISANDCLOCK", true);

		if(gameInfo.getBombs() <= 0)
		{
			uiLayer.spriteFactory.setAlpha("UIBOMB", 0.25f);
		} else {
			uiLayer.spriteFactory.setAlpha("UIBOMB", 1.0f);
		}
		
		if(gameInfo.getShields() <= 0)
		{
			uiLayer.spriteFactory.setAlpha("UISHIELD", 0.25f);
		} else {
			uiLayer.spriteFactory.setAlpha("UISHIELD", 1.0f);
		}
		
		if(gameInfo.getSandclocks() <= 0)
		{
			uiLayer.spriteFactory.setAlpha("UISANDCLOCK", 0.25f);
		} else {
			uiLayer.spriteFactory.setAlpha("UISANDCLOCK", 1.0f);
		}
		
		// foe
		uiLayer.spriteFactory.setVisible("FOETITLE", true);
		for(int i = 0; i< 20; i++)
		{
			if(currFoe >= targetFoe* ((i+ 1)/ 20.0f))
			{
				uiLayer.spriteFactory.setVisible("FOEEMPTY"+ (i+ 1), false);
				uiLayer.spriteFactory.setVisible("FOEFULL"+ (i+ 1), true);
			} else {
				uiLayer.spriteFactory.setVisible("FOEEMPTY"+ (i+ 1), true);
				uiLayer.spriteFactory.setVisible("FOEFULL"+ (i+ 1), false);
			}
		}
		
		// shield mode
		if(shieldMode)
		{
			if(shieldModeActivatedAt <= getClockTick())
			{
				float perc = 100 - ((getClockTick() - shieldModeActivatedAt) * 100 / (gameInfo.getShieldDuration() * 1000 + 10500));
				findLayerById(103).spriteFactory.setWidth("PROGRESS_SHIELD", (int)Math.max(1, Math.floor(2.25f* perc* m_ratio)));
				
				if(perc <= 0)
				{
					shieldMode = false;
				}
			}
		}
		
		// sandclock mode
		if(sandclockMode)
		{
			if(sandclockModeActivatedAt <= getClockTick())
			{
				long t = getClockTick();
				float perc = 100 - ((t - sandclockModeActivatedAt) * 100 / (gameInfo.getSandclockDuration() * 1000 + 10500));
				findLayerById(103).spriteFactory.setWidth("PROGRESS_SANDCLOCK", (int)Math.max(1, Math.floor(2.25f* perc* m_ratio)));
				
				if(perc <= 0)
				{
					sandclockMode = false;
				}
			}
		}
	}
	private void handleUINumbers() {
		
	}

	protected void displayMessage(final String message)
	{
		Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}
	public boolean onTouchEvent(MotionEvent e, PaymentProcessor paymentProcess) throws NoSuchMethodException {
	    float x = e.getX();
	    float y = e.getY();

	    //displayMessage(gameInfo.getMoney()+ "");
		if(!timeLineLog.equals(""))
		{
			displayMessage(timeLineLog);
		}
		
	    switch (e.getAction()) {
    		case MotionEvent.ACTION_UP:
	    		switch(GameMode)
	    		{
				case GAMEMODE_INTRO_SELECTGAME:
					if(!canHandleTouch)
					{
						break;
					}
					
					if(x >= (((1100- 290)/ 2.0f)* m_ratio) && x <= (((1100- 290)/ 2.0f)* m_ratio)+ 290* m_ratio &&
						y >= (((644- 550)/ 2.0f+ 140)* m_ratio) && y <= (((644- 550)/ 2.0f+ 260)* m_ratio))
					{
						// single player
						selectedLevel = -1;
						selectedSublevel = 0;
						setGameMode(GAMEMODE_INTRO_SELECTLEVEL);
					} else if(x >= (((1100- 290)/ 2.0f)* m_ratio) && x <= (((1100- 290)/ 2.0f)* m_ratio)+ 290* m_ratio &&
						y >= (((644- 550)/ 2.0f+ 255)* m_ratio) && y <= (((644- 550)/ 2.0f+ 375)* m_ratio))
					{
						// multi player
						displayMessage("Multi player mode is not available in this version");
					} else if(x >= (((1100- 290)/ 2.0f)* m_ratio) && x <= (((1100- 290)/ 2.0f)* m_ratio)+ 290* m_ratio &&
						y >= (((644- 550)/ 2.0f+ 380)* m_ratio) && y <= (((644- 550)/ 2.0f+ 500)* m_ratio))
					{
						// facebook login
						displayMessage("Facebook login is not available in this version");
					}

					break;
				case GAMEMODE_INTRO_SELECTLEVEL:
					int tab1X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(20* m_ratio));
					int tab1Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					int tab1W = (int)(185* m_ratio);
					int tab1H = (int)(71* m_ratio);
					
					int tab2X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(205* m_ratio));
					int tab2Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					int tab2W = (int)(161* m_ratio);
					int tab2H = (int)(71* m_ratio);
					
					int tab3X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(366* m_ratio));
					int tab3Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					int tab3W = (int)(151* m_ratio);
					int tab3H = (int)(71* m_ratio);
					
					int tab4X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(517* m_ratio));
					int tab4Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					int tab4W = (int)(151* m_ratio);
					int tab4H = (int)(71* m_ratio);
					
					if(x >= tab1X && x <= tab1X+ tab1W && y >= tab1Y && y <= tab1Y+ tab1H)
					{
						currentTab = 0;
					}
					
					if(x >= tab2X && x <= tab2X+ tab2W && y >= tab2Y && y <= tab2Y+ tab2H)
					{
						boostOldY = (int)y;
        				autoScrollMode = false;
        				boostDiff = 0;
						currentTab = 1;
					}
					
					if(x >= tab3X && x <= tab3X+ tab3W && y >= tab3Y && y <= tab3Y+ tab3H)
					{
						boostOldY = (int)y;
        				autoScrollMode = false;
        				boostDiff = 0;
						currentTab = 2;
					}
					
					if(x >= tab4X && x <= tab4X+ tab4W && y >= tab4Y && y <= tab4Y+ tab4H)
					{
						currentTab = 3;
					}
					
					switch(currentTab)
					{
					case 0:
						// handle "level select" events
						handleLevelSelectEvents((int)x, (int)y);
						break;
					case 1:
						handleBoostsEvents((int)x, (int)y);
						break;
					case 2:
						handleStoreEvents((int)x, (int)y, paymentProcess);
						break;
					case 3:
						handleEarnGoldEvents((int)x, (int)y);
						break;
					}
					
					displayMainMenuTabs();
					
					break;

				case GAMEMODE_INTRO_SELECTSUBLEVEL:
					tab1X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(20* m_ratio));
					tab1Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					tab1W = (int)(185* m_ratio);
					tab1H = (int)(71* m_ratio);
					
					tab2X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(205* m_ratio));
					tab2Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					tab2W = (int)(161* m_ratio);
					tab2H = (int)(71* m_ratio);
					
					tab3X = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(366* m_ratio));
					tab3Y = (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio));
					tab3W = (int)(151* m_ratio);
					tab3H = (int)(71* m_ratio);
										
					if(x >= tab1X && x <= tab1X+ tab1W && y >= tab1Y && y <= tab1Y+ tab1H)
					{
						currentLevelTab = 0;
					}
					
					if(x >= tab2X && x <= tab2X+ tab2W && y >= tab2Y && y <= tab2Y+ tab2H)
					{
						currentLevelTab = 1;
					}
					
					if(x >= tab3X && x <= tab3X+ tab3W && y >= tab3Y && y <= tab3Y+ tab3H)
					{
						currentLevelTab = 2;
					}
					
			    	if(findLayerById(121).spriteFactory.getVisible("ARROW_LEFT"))
					{
			    		int arrowLeftX = (int)(604* m_ratio); 
			    		int arrowLeftY = (int)(248* m_ratio);
			    		int arrowLeftWidth = (int)(69* m_ratio);
			    		int arrowLeftHeight = (int)(69* m_ratio);

			    		if(x >= arrowLeftX && x <= arrowLeftX + arrowLeftWidth &&
			    			y >= arrowLeftY && y <= arrowLeftY + arrowLeftHeight)
			    		{
			    			hideTownNames();
		    				selectedSublevel--;
		    				if(selectedSublevel< subLevelCount[selectedLevel])
		    				{
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT", true);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_ALPHA", false);
		    				} else {
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT", false);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_ALPHA", true);
		    				}

			    			if(selectedSublevel > 0)
			    			{
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT", true);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_ALPHA", false);

			    				hideAllLevelThumbnails();
			    				if(selectedSublevel <= subLevelCurrent[selectedLevel])
			    				{
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", true);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", false);

				    				findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_"+ (selectedLevel+ 1) +"_"+ (selectedSublevel+ 1), true);
			    				} else {
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", false);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", true);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", true);
			    				}
			    			} else {
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT", false);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_ALPHA", true);
			    				
			    				hideAllLevelThumbnails();
			    				if(selectedSublevel <= subLevelCurrent[selectedLevel])
			    				{
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", true);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", false);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_"+ (selectedLevel+ 1) +"_"+ (selectedSublevel+ 1), true);
			    				} else {
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", false);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", true);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", true);
			    				}
			    			}

			    			mapTravelToNext(selectedSublevel);
			    		}
			    		
	    				findLayerById(121).spriteFactory.setVisible("TOWNNAME_"+ (selectedLevel+ 1)+ "_"+ (selectedSublevel+ 1), true);
					}

			    	if(findLayerById(121).spriteFactory.getVisible("ARROW_RIGHT"))
					{
			    		int arrowRightX = (int)(942* m_ratio); 
			    		int arrowRightY = (int)(248* m_ratio); 
			    		int arrowRightWidth = (int)(69* m_ratio);
			    		int arrowRightHeight = (int)(69* m_ratio);

			    		if(x >= arrowRightX && x <= arrowRightX + arrowRightWidth &&
			    			y >= arrowRightY && y <= arrowRightY + arrowRightHeight)
			    		{
			    			hideTownNames();
		    				selectedSublevel++;
		    				if(selectedSublevel> 0)
		    				{
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT", true);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_ALPHA", false);
		    				} else {
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT", false);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_LEFT_ALPHA", true);
		    				}

			    			if(selectedSublevel< subLevelCount[selectedLevel])
			    			{
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT", true);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_ALPHA", false);

			    				hideAllLevelThumbnails();
			    				if(selectedSublevel <= subLevelCurrent[selectedLevel])
			    				{
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", true);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", false);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_"+ (selectedLevel+ 1) +"_"+ (selectedSublevel+ 1), true);
			    				} else {
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", false);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", true);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", true);
			    				}
			    			} else {
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT", false);
			    				findLayerById(121).spriteFactory.setVisible("ARROW_RIGHT_ALPHA", true);
			    				
			    				hideAllLevelThumbnails();
			    				if(selectedSublevel <= subLevelCurrent[selectedLevel])
			    				{
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", true);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", false);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_"+ (selectedLevel+ 1) +"_"+ (selectedSublevel+ 1), true);
			    				} else {
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYLEVEL", false);
			    			    	findLayerById(101).spriteFactory.setVisible("PLAYALPHA", true);

			    					findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", true);
			    				}
			    			}

			    			mapTravelToNext(selectedSublevel);
			    		}
			    		
	    				findLayerById(121).spriteFactory.setVisible("TOWNNAME_"+ (selectedLevel+ 1)+ "_"+ (selectedSublevel+ 1), true);
					}

			    	if(findLayerById(101).spriteFactory.getVisible("PLAYLEVEL"))
					{
						int playButtonX = (int)(698* m_ratio);
						int playButtonY = (int)(453* m_ratio);
						int playButtonW = (int)(231* m_ratio);
						int playButtonH = (int)(106* m_ratio);

						if(x >= playButtonX && x <= playButtonX+ playButtonW &&
							y >= playButtonY && y <= playButtonY+ playButtonH)
						{
							invisibleAllTextures();
							setGameMode(GAMEMODE_LOAD_LEVEL);
						}
					}
					
			    	break;
				case GAMEMODE_PLAY_LEVEL1:
					if(hasEnded)
					{
						restartGame();
						break;
					}
					
					int posx = findLayerById(99).spriteFactory.getX("UIBOMB"); 
					int posy = findLayerById(99).spriteFactory.getY("UIBOMB");
					int width = (int)(50* m_ratio);
					int height = (int)(63* m_ratio);

					if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
					{
						handleBombclick();
					}

					posx = findLayerById(99).spriteFactory.getX("UISANDCLOCK"); 
					posy = findLayerById(99).spriteFactory.getY("UISANDCLOCK");
					if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
					{
						handleSandclockClick();
					}

					posx = findLayerById(99).spriteFactory.getX("UISHIELD"); 
					posy = findLayerById(99).spriteFactory.getY("UISHIELD");
					if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
					{
						handleShieldClick();
					}

			    	break;
	    		}
    			break;
   
    		case MotionEvent.ACTION_DOWN:
	    		switch(GameMode)
	    		{
	    		case GAMEMODE_COMPANY:
					setGameMode(GlRenderer.GAMEMODE_INTRO);
	    			
	    			break;
	    		case GAMEMODE_INTRO_SELECTLEVEL:
	        		switch(currentTab)
	        		{
	        		case 1:
	        			// boosts
	        			if(!findLayerById(131).spriteFactory.getVisible("SURPRISECHEST_BACKGROUND") &&
	        				!findLayerById(131).spriteFactory.getVisible("NOTENOUGHMONEY_BG") &&
	        				!findLayerById(131).spriteFactory.getVisible("SUCCESS_BG"))
	        			{	        			
							int coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SHIELD")+ (620* m_ratio)); 
							int coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ (15* m_ratio));
							int width = (int)(180* m_ratio);
							int height = (int)(70* m_ratio);						
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_SHIELD_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_SHIELD", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SURPRISECHEST")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SURPRISECHEST")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_SURPRISECHEST_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_SURPRISECHEST", false);
							}
							
							coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_BOMB")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_BOMB_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_BOMB", false);
							}
							
							coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SANDCLOCK")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_SANDCLOCK_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_SANDCLOCK", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SHIELD_UPGRADE")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD_UPGRADE")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_BOMB_UPGRADE")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB_UPGRADE")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SANDCLOCK_UPGRADE")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK_UPGRADE")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK", false);
							}

							boostOldY = (int)y;
	        				autoScrollMode = false;
	        				boostDiff = 0;
	        			}

	        			break;
	        		case 2:
	        			// store
	        			if(!findLayerById(131).spriteFactory.getVisible("NOTENOUGHMONEY_BG") &&
	        				!findLayerById(131).spriteFactory.getVisible("SUCCESS_BG"))
	        			{	        			
							int coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_10000")+ (620* m_ratio)); 
							int coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ (15* m_ratio));
							int width = (int)(180* m_ratio);
							int height = (int)(70* m_ratio);						
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_10000_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_10000", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_20000")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_20000")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_20000_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_20000", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_50000")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_50000")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_50000_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_50000", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_100000")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_100000")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_100000_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_MONEY_100000", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_10")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_10")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_10_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_10", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_20")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_20")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_20_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_20", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_50")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_50")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_50_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_50", false);
							}

							coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_100")+ (620* m_ratio)); 
							coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_100")+ (15* m_ratio));
							if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
							{
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_100_HOT", true);
								findLayerById(92).spriteFactory.setVisible("BTN_BUY_GOLD_100", false);
							}

							boostOldY = (int)y;
	        				autoScrollMode = false;
	        				boostDiff = 0;
	        			}

	        			break;
	        		case 3:
	        			// earn coins
						int coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_EARN_1000")+ (620* m_ratio)); 
						int coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_EARN_1000")+ (15* m_ratio));
						int width = (int)(180* m_ratio);
						int height = (int)(70* m_ratio);						
						if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
						{
							findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_1000_HOT", true);
							findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_1000", false);
						}

						coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_EARN_2GOLD")+ (620* m_ratio)); 
						coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_EARN_2GOLD")+ (15* m_ratio));
						if(x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
						{
							findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_2GOLD_HOT", true);
							findLayerById(92).spriteFactory.setVisible("BTN_CLAIM_2GOLD", false);
						}
	        			
	        			break;
	        		}

	        		break;
	    		case GAMEMODE_INTRO_SELECTGAME:
					canHandleTouch = true;
					
	    			break;
				case GAMEMODE_INTRO:
					currentTab = 0;
					setGameMode(GlRenderer.GAMEMODE_INTRO_SELECTGAME);
					canHandleTouch = false;

					break;
				case GAMEMODE_PLAY_LEVEL1:
					// shoot
					if(currLives <= 0)
					{
						break;
					}
					
					displayScore();

					int posx = findLayerById(99).spriteFactory.getX("UIBOMB"); 
					int posy = findLayerById(99).spriteFactory.getY("UIBOMB");
					int width = (int)(50* m_ratio);
					int height = (int)(63* m_ratio);

					if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
					{
						//displayMessage("Bomb clicked");
						break;
					}

					posx = findLayerById(99).spriteFactory.getX("UISANDCLOCK"); 
					posy = findLayerById(99).spriteFactory.getY("UISANDCLOCK");
					if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
					{
						//displayMessage("Sandlock clicked");
						break;
					}

					posx = findLayerById(99).spriteFactory.getX("UISHIELD"); 
					posy = findLayerById(99).spriteFactory.getY("UISHIELD");
					if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
					{
						//displayMessage("Shield clicked");
						break;
					}

					crossairX = x;
					crossairY = y;

					try
					{
						handleShoot();
					} catch(Exception ex)
					{
						timeLineLog = ex.getMessage();
					}
					break;
	    		}

	    		break;
	        case MotionEvent.ACTION_MOVE:
	        	switch(GameMode)
	        	{
	        	case GAMEMODE_INTRO_SELECTLEVEL:
	        		switch(currentTab)
	        		{
	        		case 1:
	        			// boosts
	        			if(findLayerById(131).spriteFactory.getVisible("SURPRISECHEST_BACKGROUND") ||
	        				findLayerById(131).spriteFactory.getVisible("NOTENOUGHMONEY_BG") ||
	        				findLayerById(131).spriteFactory.getVisible("SUCCESS_BG"))
	        			{	
	        				break;
	        			}

	    				int diff = (int)((y- boostOldY)* 1.5);
	    				boostOldY = (int)y;

	    				if((int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ diff)> 130*m_ratio)
						{
	    					diff = (int)((130*m_ratio)- (findLayerById(91).spriteFactory.getY("BOOST_SHIELD")));
						}
	    				
	    				if((int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ diff)< -290*m_ratio)
						{
	    					diff = (int)((-290*m_ratio)- (findLayerById(91).spriteFactory.getY("BOOST_SHIELD")));
						}
	    				
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SHIELD", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SHIELD")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_BOMB", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_BOMB")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SANDCLOCK", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SANDCLOCK")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SHIELD_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SHIELD_HOT")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_BOMB_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_BOMB_HOT")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SANDCLOCK_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SANDCLOCK_HOT")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SURPRISECHEST", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SURPRISECHEST")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_BUY_SURPRISECHEST_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_SURPRISECHEST_HOT")+ diff));

	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SHIELD", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SHIELD")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SHIELD_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SHIELD_HOT")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_BOMB", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_BOMB")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_BOMB_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_BOMB_HOT")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SANDCLOCK", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SANDCLOCK")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("BTN_UPGRADE_SANDCLOCK_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SANDCLOCK_HOT")+ diff));

	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL1", (int)(283* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL1")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL2", (int)(348* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL2")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL3", (int)(413* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL3")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL4", (int)(478* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL4")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL5", (int)(543* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL5")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SHIELD_LVL6", (int)(608* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SHIELD_LVL6")+ diff));

	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL1", (int)(283* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL1")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL2", (int)(348* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL2")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL3", (int)(413* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL3")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL4", (int)(478* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL4")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL5", (int)(543* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL5")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_BOMB_LVL6", (int)(608* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_BOMB_LVL6")+ diff));

	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL1", (int)(283* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL1")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL2", (int)(348* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL2")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL3", (int)(413* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL3")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL4", (int)(478* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL4")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL5", (int)(543* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL5")+ diff));
	    		    	findLayerById(92).spriteFactory.setPosition("PRGRSS_SANDCLOCK_LVL6", (int)(608* m_ratio), (int)(findLayerById(92).spriteFactory.getY("PRGRSS_SANDCLOCK_LVL6")+ diff));

	    				findLayerById(91).spriteFactory.setPosition("BOOST_SHIELD", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_BOMB", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SANDCLOCK", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SURPRISECHEST", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SURPRISECHEST")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SHIELD_UPGRADE", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD_UPGRADE")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_BOMB_UPGRADE", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB_UPGRADE")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("BOOST_SANDCLOCK_UPGRADE", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK_UPGRADE")+ diff));
	    		    	
	    				findLayerById(93).textFactory.setPosition("UPGRADE_SHIELD_PRICE", (int)(843* m_ratio), (int)(findLayerById(93).textFactory.getY("UPGRADE_SHIELD_PRICE")+ diff));
	    				findLayerById(93).textFactory.setPosition("UPGRADE_BOMB_PRICE", (int)(843* m_ratio), (int)(findLayerById(93).textFactory.getY("UPGRADE_BOMB_PRICE")+ diff));
	    				findLayerById(93).textFactory.setPosition("UPGRADE_SANDCLOCK_PRICE", (int)(843* m_ratio), (int)(findLayerById(93).textFactory.getY("UPGRADE_SANDCLOCK_PRICE")+ diff));

	    		    	findLayerById(91).textFactory.setPosition("SHIELD_DISP", (int)(405* m_ratio), (int)(findLayerById(91).textFactory.getY("SHIELD_DISP")+ diff));
	    		    	findLayerById(91).textFactory.setPosition("BOMB_DISP", (int)(405* m_ratio), (int)(findLayerById(91).textFactory.getY("BOMB_DISP")+ diff));
	    		    	findLayerById(91).textFactory.setPosition("SANDCLOCK_DISP", (int)(405* m_ratio), (int)(findLayerById(91).textFactory.getY("SANDCLOCK_DISP")+ diff));
	    		    	
	    		    	boostDiff = diff;

	    		    	break;
	        		case 2:
	        			if(findLayerById(131).spriteFactory.getVisible("NOTENOUGHMONEY_BG") ||
	        				findLayerById(131).spriteFactory.getVisible("SUCCESS_BG"))
	        			{	
	        				break;
	        			}

	    				diff = (int)((y- boostOldY)* 1.5);
	    				boostOldY = (int)y;

	    				if((int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ diff)> 130*m_ratio)
						{
	    					diff = (int)((130*m_ratio)- (findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")));
						}
	    				
	    				if((int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ diff)< -390*m_ratio)
						{
	    					diff = (int)((-390*m_ratio)- (findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")));
						}

	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_10000", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_10000")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_10000_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_10000_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_20000", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_20000")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_20000_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_20000_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_50000", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_50000")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_50000_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_50000_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_100000", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_100000")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_MONEY_100000_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_MONEY_100000_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_10", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_10")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_10_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_10_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_20", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_20")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_20_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_20_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_50", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_50")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_50_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_50_HOT")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_100", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_100")+ diff));
	    				findLayerById(92).spriteFactory.setPosition("BTN_BUY_GOLD_100_HOT", (int)(765* m_ratio), (int)(findLayerById(92).spriteFactory.getY("BTN_BUY_GOLD_100_HOT")+ diff));

	    				findLayerById(91).spriteFactory.setPosition("STORE_MONEY_10000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_20000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_20000")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_50000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_50000")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_MONEY_100000", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_100000")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_10", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_10")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_20", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_20")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_50", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_50")+ diff));
	    		    	findLayerById(91).spriteFactory.setPosition("STORE_GOLD_100", (int)(145* m_ratio), (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_100")+ diff));

	    		    	boostDiff = diff;
	    		    	break;
	        		}

	        		break;
	        	}

	            break;
	    }

	    return true;
	}

	private void handleShieldClick() {
		if(gameInfo.getShields() <= 0)
		{
			return;
		}
		
		gameInfo.setShields(gameInfo.getShields()- 1);
		int y = (int)(120* m_ratio);
		if(findLayerById(101).spriteFactory.getVisible("IND_SANDCLOCK"))
		{
			y = (int)(200* m_ratio);
		}

		findLayerById(101).spriteFactory.setPosition("IND_SHIELD", -320 * m_ratio, y);
		findLayerById(103).spriteFactory.setPosition("PROGRESS_SHIELD", -265 * m_ratio, y + 15* m_ratio);
		findLayerById(103).spriteFactory.setWidth("PROGRESS_SHIELD", (int)Math.floor(230.0f* m_ratio));
		shieldMode = true;
		shieldModeActivatedAt = getClockTick() + 500;
		
		findLayerById(101).spriteFactory.setAlpha("IND_SHIELD", 0.8f);
		findLayerById(101).spriteFactory.setVisible("IND_SHIELD", true);
		findLayerById(103).spriteFactory.setAlpha("PROGRESS_SHIELD", 0.9f);
		findLayerById(103).spriteFactory.setVisible("PROGRESS_SHIELD", true);
		Sprite sprite = findSpriteById(101, "IND_SHIELD");
		Sprite spritePrg = findSpriteById(103, "PROGRESS_SHIELD");

		findLayerById(101).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, sprite, 
					// first
					Math.floor(-320 * m_ratio),
					// last	
					Math.floor(10 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					0, -1);
		findLayerById(103).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, spritePrg, 
					// first
					Math.floor(-265 * m_ratio),
					// last	
					Math.floor(75 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					0, -1);
		
		findLayerById(101).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, sprite, 
					// first
					Math.floor(10 * m_ratio),
					// last	
					Math.floor(-320 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					10500 + 1000 * gameInfo.getShieldDuration(), -1);
		findLayerById(103).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, spritePrg, 
					// first
					Math.floor(75 * m_ratio),
					// last	
					Math.floor(-265 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					10500 + 1000 * gameInfo.getShieldDuration(), -1);
	}
	private void handleSandclockClick() {
		if(gameInfo.getSandclocks() <= 0)
		{
			return;
		}
		
		gameInfo.setSandclocks(gameInfo.getSandclocks()- 1);
		int y = (int)(120* m_ratio);
		if(findLayerById(101).spriteFactory.getVisible("IND_SHIELD"))
		{
			y = (int)(200* m_ratio);
		}

		findLayerById(101).spriteFactory.setPosition("IND_SANDCLOCK", -320 * m_ratio, y);
		findLayerById(103).spriteFactory.setPosition("PROGRESS_SANDCLOCK", -265 * m_ratio, y + 15* m_ratio);
		findLayerById(103).spriteFactory.setWidth("PROGRESS_SANDCLOCK", (int)Math.floor(230.0f* m_ratio));
		sandclockMode = true;
		sandclockModeActivatedAt = getClockTick() + 500;
		
		findLayerById(101).spriteFactory.setAlpha("IND_SANDCLOCK", 0.8f);
		findLayerById(101).spriteFactory.setVisible("IND_SANDCLOCK", true);
		findLayerById(103).spriteFactory.setAlpha("PROGRESS_SANDCLOCK", 0.9f);
		findLayerById(103).spriteFactory.setVisible("PROGRESS_SANDCLOCK", true);
		Sprite sprite = findSpriteById(101, "IND_SANDCLOCK");
		Sprite spritePrg = findSpriteById(103, "PROGRESS_SANDCLOCK");

		findLayerById(101).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, sprite, 
					// first
					Math.floor(-320 * m_ratio),
					// last	
					Math.floor(10 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					0, -1);
		findLayerById(103).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, spritePrg, 
					// first
					Math.floor(-265 * m_ratio),
					// last	
					Math.floor(75 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					0, -1);
		
		findLayerById(101).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, sprite, 
					// first
					Math.floor(10 * m_ratio),
					// last	
					Math.floor(-320 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					10500 + 1000 * gameInfo.getSandclockDuration(), -1);
		findLayerById(103).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, spritePrg, 
					// first
					Math.floor(75 * m_ratio),
					// last	
					Math.floor(-265 * m_ratio),
					// duration
					500),
					// delay, repeat interval
					10500 + 1000 * gameInfo.getSandclockDuration(), -1);
	}
	private void handleBombclick() {
		if(gameInfo.getBombs() <= 0)
		{
			return;
		}
		
		gameInfo.setBombs(gameInfo.getBombs()- 1);

		MediaPlayer mp = MediaPlayer.create(context, R.raw.bomb);
        mp.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }

        });   
        mp.start();	
        
		// bomb clicked
		for(int i = layers.size()- 1; i >= 0; i--)
		{
			if(layers.get(i).id >= 99 || layers.get(i).id == 0)
			{
				continue;
			}
			
			// 1.Buildings
			for(int j = 0; j< layers.get(i).spriteFactory.sprites.size(); j++)
			{
				String id = layers.get(i).spriteFactory.sprites.get(j).getId();
				if(findBuildingById(id) != null)
				{
					// it is a building
					shootMe(id, gameInfo.getBombPower());
				}
			}

			// 2.Moving things
			for(int j = 0; j< layers.get(i).movieClipFactory.movieClips.size(); j++)
			{
				String id = layers.get(i).movieClipFactory.movieClips.get(j).getId();
				com.tolka.shooterdudes.StageInfo.Character character = findCharacterById(id);
				if(character != null)
				{
					MovieClip clip = layers.get(i).movieClipFactory.movieClips.get(j).getClip();
					for(int t = 0; t< clip.getSpriteFactory().sprites.size(); t++)
					{
						if(clip.getSpriteFactory().sprites.get(t).getVisible())
						{
							//timeLineLog += "shooting character "+ id +"\n";
   							shootMe(id, gameInfo.getBombPower());
							
							break;
						}
					}
				}
			}
		}
	}
	private void handleEarnGoldEvents(int x, int y) {
		int coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_EARN_1000")+ (620* m_ratio)); 
		int coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_EARN_1000")+ (15* m_ratio));
		int width = (int)(180* m_ratio);
		int height = (int)(70* m_ratio);						
		if(findLayerById(92).spriteFactory.getVisible("BTN_CLAIM_1000_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleClaim1000();
		}
		
		coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_EARN_2GOLD")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_EARN_2GOLD")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_CLAIM_2GOLD_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleClaim2Gold();
		}
	}

	private void handleClaim1000()
	{
		//ProgressDialog progress = ProgressDialog.show(this, "dialog title", "dialog message", true);

		moneyToSet = gameInfo.getMoney()+ 1000;
	}

	private void handleClaim2Gold()
	{
		//ProgressDialog progress = ProgressDialog.show(this, "dialog title", "dialog message", true);

		goldToSet = gameInfo.getGold()+ 2;
	}

	private void handleLevelSelectEvents(int x, int y) {
		int level1x = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(110* m_ratio));
		int level1y = (int)Math.floor((644- 311+ 10)* m_ratio/ 2);
		int level1h = (int)(331* m_ratio);
		int level1w = (int)(148* m_ratio);

		int level2x = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(300* m_ratio));
		int level2y = (int)Math.floor((644- 311+ 10)* m_ratio/ 2);
		int level2h = (int)(331* m_ratio);
		int level2w = (int)(148* m_ratio);

		int level3x = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(490* m_ratio));
		int level3y = (int)Math.floor((644- 311+ 10)* m_ratio/ 2);
		int level3h = (int)(331* m_ratio);
		int level3w = (int)(148* m_ratio);

		int level4x = (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(680* m_ratio));
		int level4y = (int)Math.floor((644- 311+ 10)* m_ratio/ 2);
		int level4h = (int)(331* m_ratio);
		int level4w = (int)(148* m_ratio);

		if(x >= level1x && x <= level1x + level1w &&
			y >= level1y && x <= level1y + level1h)
		{
			GameMode = GAMEMODE_INTRO_SELECTSUBLEVEL;
			selectedLevel = 0;
		} else if(x >= level2x && x <= level2x + level2w &&
			y >= level2y && x <= level2y + level2h)
		{
			GameMode = GAMEMODE_INTRO_SELECTSUBLEVEL;
			selectedLevel = 1;
		} else if(x >= level3x && x <= level3x + level3w &&
			y >= level3y && x <= level3y + level3h)
		{
			GameMode = GAMEMODE_INTRO_SELECTSUBLEVEL;
			selectedLevel = 2;
		} else if(x >= level4x && x <= level4x + level4w &&
			y >= level4y && x <= level4y + level4h)
		{
			GameMode = GAMEMODE_INTRO_SELECTSUBLEVEL;
			selectedLevel = 3;
		}
		
    	selectedSublevel = getSelectedSublevel(selectedLevel);
	}

	private void handleStoreEvents(int x, int y, PaymentProcessor paymentProcess)
	{
		int coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_10000")+ (620* m_ratio)); 
		int coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_10000")+ (15* m_ratio));
		int width = (int)(180* m_ratio);
		int height = (int)(70* m_ratio);
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_MONEY_10000_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyMoney(paymentProcess, 10000);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_20000")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_20000")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_MONEY_20000_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyMoney(paymentProcess, 20000);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_50000")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_50000")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_MONEY_50000_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyMoney(paymentProcess, 50000);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_MONEY_100000")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_MONEY_100000")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_MONEY_100000_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyMoney(paymentProcess, 100000);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_10")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_10")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_GOLD_10_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyGold(paymentProcess, 10);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_20")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_20")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_GOLD_20_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyGold(paymentProcess, 20);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_50")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_50")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_GOLD_50_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyGold(paymentProcess, 50);
		}

		coordX = (int)(findLayerById(91).spriteFactory.getX("STORE_GOLD_100")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("STORE_GOLD_100")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_GOLD_100_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			shouldReloadTextures = false;
			handleBuyGold(paymentProcess, 100);
		}

		if(boostDiff != 0)
		{
			autoScrollMode = true;
			boostScrollStart = getClockTick();
		}
	}

	private void handleBuyMoney(PaymentProcessor paymentProcess, int amount)
	{
		switch(amount)
		{
		case 10000:
			paymentProcess.setAmount(new BigDecimal("0.49"));
			paymentProcess.setExplanation("Shooter Dudes $10.000 In Game Money");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		case 20000:
			paymentProcess.setAmount(new BigDecimal("0.99"));
			paymentProcess.setExplanation("Shooter Dudes $20.000 In Game Money");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		case 50000:
			paymentProcess.setAmount(new BigDecimal("1.49"));
			paymentProcess.setExplanation("Shooter Dudes $50.000 In Game Money");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		case 100000:
			paymentProcess.setAmount(new BigDecimal("1.99"));
			paymentProcess.setExplanation("Shooter Dudes $100.000 In Game Money");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		default:
			displayMessage("Invalid amount for buying money: "+ amount);
			break;
		}
	}

	private void handleBuyGold(PaymentProcessor paymentProcess, int amount)
	{
		switch(amount)
		{
		case 10:
			paymentProcess.setAmount(new BigDecimal("0.49"));
			paymentProcess.setExplanation("Shooter Dudes 10 In Game Gold");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		case 20:
			paymentProcess.setAmount(new BigDecimal("0.99"));
			paymentProcess.setExplanation("Shooter Dudes 20 In Game Gold");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		case 50:
			paymentProcess.setAmount(new BigDecimal("1.49"));
			paymentProcess.setExplanation("Shooter Dudes 50 In Game Gold");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		case 100:
			paymentProcess.setAmount(new BigDecimal("1.99"));
			paymentProcess.setExplanation("Shooter Dudes 100 In Game Gold");
			GameMode = GAMEMODE_INTRO_SELECTLEVEL;
			break;
		default:
			displayMessage("Invalid amount for buying gold: "+ amount);
			break;
		}
	}

	private void handleBoostsEvents(int x, int y) {
		if(surpriseChestItem == -1 && findLayerById(131).spriteFactory.getVisible("SURPRISECHEST_BACKGROUND"))
		{
			// determine the type of surprise chest item
			surpriseChestItem = rand.nextInt(MAX_SURPRISECHEST_ITEMS);

	    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_BIG", false);
	    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_TAPTOOPEN", false);
	    	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", true);
	    	findSpriteById(132, "BTNCLOSE").setPosition((int)(465* m_ratio), (int)(480* m_ratio));

	    	String sprite = null;
			switch(surpriseChestItem)
			{
			case 0:
				// 1000 money
				sprite = "SURPRISECHEST_ITEM_MONEY1000";
				moneyToSet = gameInfo.getMoney()+ 1000;
				
				break;
			case 1:
				// 500 money
				sprite = "SURPRISECHEST_ITEM_MONEY500";
				moneyToSet = gameInfo.getMoney()+ 500;
				
				break;
			case 2:
				// 100 money
				sprite = "SURPRISECHEST_ITEM_MONEY100";
				moneyToSet = gameInfo.getMoney()+ 100;
				
				break;
			case 3:
				// 7 money
				sprite = "SURPRISECHEST_ITEM_MONEY7";
				moneyToSet = gameInfo.getMoney()+ 7;
				
				break;
			case 4:
				// 5 sandclocks
				sprite = "SURPRISECHEST_ITEM_SANDCLOCKS5";
				sandclocksToSet = 5+ gameInfo.getSandclocks();
				
				break;
			case 5:
				// 3 sandclocks
				sprite = "SURPRISECHEST_ITEM_SANDCLOCKS3";
				sandclocksToSet = 3+ gameInfo.getSandclocks();
				
				break;
			case 6:
				// 1 sandclocks
				sprite = "SURPRISECHEST_ITEM_SANDCLOCKS1";
				sandclocksToSet = 1+ gameInfo.getSandclocks();
				
				break;
			case 7:
				// 5 bombs
				sprite = "SURPRISECHEST_ITEM_BOMBS5";
				bombsToSet = 5+ gameInfo.getBombs();
				
				break;
			case 8:
				// 3 bombs
				sprite = "SURPRISECHEST_ITEM_BOMBS3";
				bombsToSet = 3+ gameInfo.getBombs();
				
				break;
			case 9:
				// 1 bombs
				sprite = "SURPRISECHEST_ITEM_BOMBS1";
				bombsToSet = 1+ gameInfo.getBombs();
				
				break;
			case 10:
				// 5 shields
				sprite = "SURPRISECHEST_ITEM_SHIELDS5";
				shieldsToSet = 5+ gameInfo.getShields();
				
				break;
			case 11:
				// 3 shields
				sprite = "SURPRISECHEST_ITEM_SHIELDS3";
				shieldsToSet = 3+ gameInfo.getShields();
				
				break;
			case 12:
				// 1 shield
				sprite = "SURPRISECHEST_ITEM_SHIELDS1";
				shieldsToSet = 5+ gameInfo.getShields();
				
				break;
			case 13:
				// 10 gold
				sprite = "SURPRISECHEST_ITEM_GOLD10";
				goldToSet = gameInfo.getGold()+ 10;
				
				break;
			case 14:
				// 3 gold
				sprite = "SURPRISECHEST_ITEM_GOLD3";
				goldToSet = gameInfo.getGold()+ 3;

				break;
			case 15:
				// 2 gold
				sprite = "SURPRISECHEST_ITEM_GOLD2";
				goldToSet = gameInfo.getGold()+ 2;
				
				break;
			case 16:
				// 1 gold
				sprite = "SURPRISECHEST_ITEM_GOLD1";
				goldToSet = gameInfo.getGold()+ 1;
				
				break;
			default:
				// pile of useless rocks
				sprite = "SURPRISECHEST_ITEM_ROCKS";

		    	break;
			}

			if(sprite != null)
			{
		    	findLayerById(132).spriteFactory.setVisible(sprite, true);

		    	canUpdateSurpriseChest = false;
		    	findLayerById(132).tweenFactory.add(
						new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(132, sprite), 
							// first
							0,
							// last	
							1,
							// duration
							1000),
							// delay, repeat interval
						0, -1);
				
			}

			return;
		} else if(surpriseChestItem != -1 && findLayerById(131).spriteFactory.getVisible("SURPRISECHEST_BACKGROUND"))
		{
			int width = (int)(170* m_ratio);
			int height = (int)(60* m_ratio);
			int posx = (int)(465* m_ratio);
			int posy = (int)(480* m_ratio);

			if(x >= posx && x <= posx + width && y >= posy && y <= posy + height)
			{
		    	findLayerById(130).spriteFactory.setVisible("DARKBG", false);
		    	findLayerById(131).spriteFactory.setVisible("SURPRISECHEST_BACKGROUND", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_STARS", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_BIG", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_TAPTOOPEN", false);
		    	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_MONEY1000", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_MONEY500", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_MONEY100", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_MONEY7", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_SANDCLOCKS5", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_SANDCLOCKS3", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_SANDCLOCKS1", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_BOMBS5", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_BOMBS3", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_BOMBS1", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_SHIELDS5", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_SHIELDS3", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_SHIELDS1", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_GOLD10", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_GOLD3", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_GOLD2", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_GOLD1", false);
		    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_ITEM_ROCKS", false);
		    	
		    	canUpdateSurpriseChest = true;
		    	surpriseChestItem = -1;
			}
			
			return;
		}
		
		if(findLayerById(131).spriteFactory.getVisible("NOTENOUGHMONEY_BG"))
		{
			int posx = (int)(565* m_ratio);
			int posy = (int)(420* m_ratio);
			int width = (int)(170* m_ratio);
			int height = (int)(60* m_ratio);
			
			if(x >= posx && x <= posx+ width && y >= posy && y <= posy+ height)
			{
				// close
		    	findLayerById(130).spriteFactory.setVisible("DARKBG", false);
				findLayerById(131).spriteFactory.setVisible("NOTENOUGHMONEY_BG", false);
				findLayerById(132).spriteFactory.setVisible("BTNCLOSE", false);
				findLayerById(132).spriteFactory.setVisible("BTNSTORE", false);
			}

			posx = (int)(365* m_ratio);
			if(x >= posx && x <= posx+ width && y >= posy && y <= posy+ height)
			{
				// store
		    	findLayerById(130).spriteFactory.setVisible("DARKBG", false);
				findLayerById(131).spriteFactory.setVisible("NOTENOUGHMONEY_BG", false);
				findLayerById(132).spriteFactory.setVisible("BTNCLOSE", false);
				findLayerById(132).spriteFactory.setVisible("BTNSTORE", false);
				
				boostOldY = (int)y;
				autoScrollMode = false;
				boostDiff = 0;

				currentTab = 2;
			}
		}
		
		if(findLayerById(131).spriteFactory.getVisible("SUCCESS_BG"))
		{
			int posx = (int)(465* m_ratio);
			int posy = (int)(420* m_ratio);
			int width = (int)(170* m_ratio);
			int height = (int)(60* m_ratio);
			
			if(x >= posx && x <= posx+ width && y >= posy && y <= posy+ height)
			{
				// close
		    	findLayerById(130).spriteFactory.setVisible("DARKBG", false);
				findLayerById(131).spriteFactory.setVisible("SUCCESS_BG", false);
				findLayerById(132).spriteFactory.setVisible("BTNCLOSE", false);
			}
		}

		if(boostDiff != 0)
		{
			autoScrollMode = true;
			boostScrollStart = getClockTick();
		}
		
		int coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SHIELD")+ (620* m_ratio)); 
		int coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SHIELD")+ (15* m_ratio));
		int width = (int)(180* m_ratio);
		int height = (int)(70* m_ratio);						
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_SHIELD_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleBuyShield();
		}
		
		coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SURPRISECHEST")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SURPRISECHEST")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_SURPRISECHEST_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleSurpriseChest();
		}
		
		coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_BOMB")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_BOMB")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_BOMB_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleBuyBomb();
		}
		
		coordX = (int)(findLayerById(91).spriteFactory.getX("BOOST_SANDCLOCK")+ (620* m_ratio)); 
		coordY = (int)(findLayerById(91).spriteFactory.getY("BOOST_SANDCLOCK")+ (15* m_ratio));
		if(findLayerById(92).spriteFactory.getVisible("BTN_BUY_SANDCLOCK_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleBuySandclock();
		}

		coordX = (int)(findLayerById(92).spriteFactory.getX("BTN_UPGRADE_SHIELD")); 
		coordY = (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SHIELD"));
		
		if(findLayerById(92).spriteFactory.getVisible("BTN_UPGRADE_SHIELD_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleUpgradeShield();
		}

		coordX = (int)(findLayerById(92).spriteFactory.getX("BTN_UPGRADE_BOMB")); 
		coordY = (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_BOMB"));
		
		if(findLayerById(92).spriteFactory.getVisible("BTN_UPGRADE_BOMB_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleUpgradeBomb();
		}

		coordX = (int)(findLayerById(92).spriteFactory.getX("BTN_UPGRADE_SANDCLOCK")); 
		coordY = (int)(findLayerById(92).spriteFactory.getY("BTN_UPGRADE_SANDCLOCK"));
		
		if(findLayerById(92).spriteFactory.getVisible("BTN_UPGRADE_SANDCLOCK_HOT") && x >= coordX && x <= coordX+ width && y >= coordY && y <= coordY+ height)
		{
			handleUpgradeSandclock();
		}
	}

	private void handleUpgradeSandclock()
	{
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SANDCLOCK", true);

    	sandclockDurationToSet = gameInfo.getSandclockDuration()+ 1;
    	int amount = 0;
		switch(sandclockDurationToSet- 1)
		{
		case 0: amount = 500; break;
		case 1: amount = 1000; break;
		case 2: amount = 2000; break;
		case 3: amount = 5000; break;
		case 4: amount = 10000; break;
		case 5: amount = 20000; break;
		case 6: amount = 50000; break;
		default:
			sandclockDurationToSet = -1;
			return;
		}

    	if(gameInfo.getMoney() >= amount)
		{
    		moneyToSet = gameInfo.getMoney()- amount;
		} else {
			// not enough money
			sandclockDurationToSet = -1;
			displayNotEnoughMoney();
		}
	}

	private void handleUpgradeBomb()
	{
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_BOMB", true);

    	bombPowerToSet = gameInfo.getBombPower()+ 1;
    	int amount = 0;
		switch(bombPowerToSet- 1)
		{
		case 0: amount = 500; break;
		case 1: amount = 1000; break;
		case 2: amount = 2000; break;
		case 3: amount = 5000; break;
		case 4: amount = 10000; break;
		case 5: amount = 20000; break;
		case 6: amount = 50000; break;
		default:
			bombPowerToSet = -1;
			return;
		}

    	if(gameInfo.getMoney() >= amount)
		{
    		moneyToSet = gameInfo.getMoney()- amount;
		} else {
			// not enough money
			bombPowerToSet = -1;
			displayNotEnoughMoney();
		}
	}

	private void handleUpgradeShield() {
    	findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD_HOT", false);
		findLayerById(92).spriteFactory.setVisible("BTN_UPGRADE_SHIELD", true);

    	shieldDurationToSet = gameInfo.getShieldDuration()+ 1;
    	int amount = 0;
		switch(shieldDurationToSet- 1)
		{
		case 0: amount = 500; break;
		case 1: amount = 1000; break;
		case 2: amount = 2000; break;
		case 3: amount = 5000; break;
		case 4: amount = 10000; break;
		case 5: amount = 20000; break;
		case 6: amount = 50000; break;
		default:
			shieldDurationToSet = -1;
			return;
		}

    	if(gameInfo.getMoney() >= amount)
		{
    		moneyToSet = gameInfo.getMoney()- amount;
		} else {
			// not enough money
			shieldDurationToSet = -1;
			displayNotEnoughMoney();
		}
	}

	private boolean canUpdateSurpriseChest;
	private Handler handler = new Handler();
	private Runnable scUpdateData = new Runnable(){
	    public void run(){
	    	findLayerById(132).tweenFactory.add(
					new MotionTween(MotionTween.ATTR_Y, findSpriteById(132, "SURPRISECHEST_BIG"), 
						// first
						(int)(162* m_ratio),
						// last	
						(int)(180* m_ratio),
						// duration
						1000),
						// delay, repeat interval
					0, -1);
	    	
			findLayerById(132).tweenFactory.add(
					new MotionTween(MotionTween.ATTR_Y, findSpriteById(132, "SURPRISECHEST_BIG"), 
						// first
						(int)(180* m_ratio),
						// last	
						(int)(162* m_ratio),
						// duration
						1000),
						// delay, repeat interval
					1000, -1);

			if(canUpdateSurpriseChest)
			{
				handler.postDelayed(scUpdateData,2000);
			}
	    }
	};
	
	private void handleSurpriseChest() {
    	findLayerById(130).spriteFactory.setVisible("DARKBG", true);

    	if(gameInfo.getMoney() >= 500)
		{
    		moneyToSet = gameInfo.getMoney()- 500;

	    	findLayerById(131).spriteFactory.setVisible("SURPRISECHEST_BACKGROUND", true);
	    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_STARS", true);
	    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_BIG", true);
	    	findLayerById(132).spriteFactory.setVisible("SURPRISECHEST_TAPTOOPEN", true);
	    	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", false);
	    	
	    	canUpdateSurpriseChest = true;
	    	scUpdateData.run();
		} else {
			// not enough money
			displayNotEnoughMoney();
		}
	}
	private void displayNotEnoughMoney() {
    	findLayerById(130).spriteFactory.setVisible("DARKBG", true);
    	findLayerById(131).spriteFactory.setVisible("NOTENOUGHMONEY_BG", true);
    	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", true);
    	findLayerById(132).spriteFactory.setVisible("BTNSTORE", true);
    	findSpriteById(132, "BTNCLOSE").setPosition((int)(565* m_ratio), (int)(420* m_ratio));
    	findSpriteById(132, "BTNSTORE").setPosition((int)(365* m_ratio), (int)(420* m_ratio));
	}

	private void handleBuySandclock() {
    	if(gameInfo.getMoney() >= 500)
		{
    		moneyToSet = gameInfo.getMoney()- 500;
    		sandclocksToSet = gameInfo.getSandclocks()+ 1;

    		findLayerById(130).spriteFactory.setVisible("DARKBG", true);
        	findLayerById(131).spriteFactory.setVisible("SUCCESS_BG", true);
        	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", true);
        	findSpriteById(132, "BTNCLOSE").setPosition((int)(465* m_ratio), (int)(420* m_ratio));
		} else {
			// not enough money
			displayNotEnoughMoney();
		}
	}
	private void handleBuyBomb() {
    	if(gameInfo.getMoney() >= 500)
		{
    		moneyToSet = gameInfo.getMoney()- 500;
    		bombsToSet = gameInfo.getBombs()+ 1;

    		findLayerById(130).spriteFactory.setVisible("DARKBG", true);
        	findLayerById(131).spriteFactory.setVisible("SUCCESS_BG", true);
        	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", true);
        	findSpriteById(132, "BTNCLOSE").setPosition((int)(465* m_ratio), (int)(420* m_ratio));
		} else {
			// not enough money
			displayNotEnoughMoney();
		}
	}
	private void handleBuyShield() {
		if(gameInfo.getMoney() >= 500)
		{
    		moneyToSet = gameInfo.getMoney()- 500;
    		shieldsToSet = gameInfo.getShields()+ 1;

    		findLayerById(130).spriteFactory.setVisible("DARKBG", true);
        	findLayerById(131).spriteFactory.setVisible("SUCCESS_BG", true);
        	findLayerById(132).spriteFactory.setVisible("BTNCLOSE", true);
        	findSpriteById(132, "BTNCLOSE").setPosition((int)(465* m_ratio), (int)(420* m_ratio));
		} else {
			// not enough money
			displayNotEnoughMoney();
		}
	}

	private void mapTravelToNext(int to) {
		findLayerById(91).tweenFactory.stopAll();
		findLayerById(91).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_X, findSpriteById(91, "MAP1"), 
					// first
					findLayerById(91).spriteFactory.getX("MAP1"),
					// last	
					(int)Math.floor((1100- 846)* m_ratio/ 2+ mapDiffLvl1X[to]* m_ratio),
					// duration
					700),
					// delay, repeat interval
				0, -1);

		findLayerById(91).tweenFactory.add(
				new MotionTween(MotionTween.ATTR_Y, findSpriteById(91, "MAP1"), 
					// first
					findLayerById(91).spriteFactory.getY("MAP1"),
					// last	
					(int)Math.floor((644- 458+ 70)* m_ratio/ 2+ mapDiffLvl1Y[to]* m_ratio),
					// duration
					700),
					// delay, repeat interval
				0, -1);
	}

	private void hideAllLevelThumbnails() {
		findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_0", false);
		findLayerById(101).spriteFactory.setVisible("THUMB_TOWN_1_1", false);
	}
	private Sprite findSpriteById(int layer, String id) {
		Layer l = findLayerById(layer);
		if(l != null)
		{
			for(int i = 0; i< l.spriteFactory.sprites.size(); i++)
			{
				if(l.spriteFactory.sprites.get(i).getId().equals(id))
				{
					return l.spriteFactory.sprites.get(i).getSprite();
				}
			}
		}
		
		return null;
	}

	private int getSelectedSublevel(int selectedLevel) {
		return 0;
	}

	private void completeStage()
	{
		hasEnded = true;
    	findLayerById(121).spriteFactory.setVisible("GAMEEND", true);
    	findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(121, "GAMEEND"), 0.0f, 1.0f, 2000.0), 0, -1);
		//return;
    	//findLayerById(122).spriteFactory.setVisible("BGWOODS", true);
    	//findLayerById(122).spriteFactory.setPosition("BGWOODS", 0.0f, -680.0f * m_ratio);
    	//findLayerById(122).tweenFactory.add(new MotionTween(MotionTween.ATTR_Y, findSpriteById(122, "BGWOODS"), -680.0f * m_ratio, 0.0f, 1000.0), 2000, -1);
    	findLayerById(122).spriteFactory.setVisible("GAMEWON", true);
    	findLayerById(122).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(122, "GAMELOST"), 0.0f, 1.0f, 2000.0), 0, -1);
	}
	
	private void handleShoot() {
		//displayMessage(timeLineLog);
		switch(currWeapon)
		{
		case WEAPON_REVOLVER:
			if(getClockTick()- lastShootAt <= WEAPON_REVOLVER_DELAY)
			{
				// not ready to shoot
				return;
			}
		}
		
		lastShootAt = getClockTick();
		int resid = R.raw.shot_r;
		if(remainingShots <= 0)
		{
			if(crossairX >= 5* m_ratio && crossairY >= 5* m_ratio && crossairX <= 160* m_ratio && crossairY <= 71* m_ratio)
			{
				// reload
				MediaPlayer mp = MediaPlayer.create(context, R.raw.pistol_c);
		        mp.setOnCompletionListener(new OnCompletionListener() {
		            @Override
		            public void onCompletion(MediaPlayer mp) {
		                mp.release();
		            }

		        });   
		        mp.start();	

		        remainingShots = 6;
				return;
			}

			resid = R.raw.shot_d;
		} else {
            findLayerById(101).spriteFactory.setVisible("CROSSAIR", false);
	    	findLayerById(101).movieClipFactory.setPosition("SHOOT", (int)Math.floor(crossairX- 41* m_ratio), (int)Math.floor(crossairY- 36* m_ratio));
	    	findLayerById(101).movieClipFactory.play("SHOOT");
	    	
	    	RunAfterFunction f = new RunAfterFunction() {
                @Override
	    		public void run()
	    		{
	    			findLayerById(101).spriteFactory.setVisible("CROSSAIR", true);
	    		}
	    	};
	    	
	    	String collidedObject = hitTest();
	    	shootMe(collidedObject, 1);
	    	
	    	findLayerById(101).movieClipFactory.runAfterPlay("SHOOT", f);
			remainingShots--;
			if(remainingShots< 0)
			{
				remainingShots = 0;
			}
		}

		MediaPlayer mp = MediaPlayer.create(context, resid);
        mp.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }

        });   
        mp.start();	
	}
	private void shootMe(String collidedObject, int damage) {
    	if(collidedObject != null)
    	{
    		// perform collision
    		Building curr = findBuildingById(collidedObject);
    		if(curr != null && curr.getKillHits() != -1)
    		{
    			// it is a building
    			for(int i = 0; i< layers.size(); i++)
    			{
    				for(int j = 0; j< layers.get(i).spriteFactory.sprites.size(); j++)
    				{
    					if(layers.get(i).spriteFactory.sprites.get(j).getId().equals(collidedObject) &&
    						layers.get(i).spriteFactory.sprites.get(j).getVisible())
    					{
    						if(curr.getKillHits() <= curr.getCurrHits())
    						{
    							// destruction animation
    							score += curr.getPoints();
    							scoreToSet = score;

    							currFoe += curr.getFoe();
    							if(currFoe >= targetFoe)
    							{
    								// stage completed
    								completeStage();
    							}
    							
    							try
    							{
	    							MovieClip clip = new MovieClip(curr.getDestructionDelay());
	    							for(int k = 0; k< curr.desctruction.size(); k++)
	    							{
		    					    	clip.getSpriteFactory().add(curr.getId()+ "_destr"+ k, curr.desctruction.get(k).getSprite());
	    							}

	    					    	//clip.setPosition(100,  100);
	    					    	clip.setRepetition(1);
	    					    	layers.get(i).movieClipFactory.add(curr.getId()+ "_DESTR", clip);
	    					    	layers.get(i).movieClipFactory.setPosition(curr.getId()+ "_DESTR", (int)Math.floor(curr.getPosX()* m_ratio), (int)Math.floor(curr.getPosY()* m_ratio));
	    					    	layers.get(i).movieClipFactory.play(curr.getId()+ "_DESTR");

	    					    	layers.get(i).spriteFactory.setVisible(curr.getId(), false);
	    					    	layers.get(i).spriteFactory.setPosition(curr.getId()+ "_DESTROYED", (int)Math.floor(curr.getPosX()* m_ratio), (int)Math.floor(curr.getPosY()* m_ratio));
	    					    	layers.get(i).spriteFactory.setVisible(curr.getId()+ "_DESTROYED", true);
	    					    	
	    					    	// destruction sound effect
	    					    	AssetFileDescriptor afd = context.getAssets().openFd("sounds/"+ curr.getDesturctionSound()+ ".mp3");
	    							MediaPlayer mp = new MediaPlayer();
	    							mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
	    					        mp.setOnCompletionListener(new OnCompletionListener() {
	    					            @Override
	    					            public void onCompletion(MediaPlayer mp) {
	    					                mp.release();
	    					            }

	    					        });
	    					        mp.prepare();
	    					        mp.start();	

    							} catch(Exception ex)
    							{
    								displayMessage(ex.getMessage());
    							}
    						} else {
	    						//layers.get(i).spriteFactory.sprites.get(j).
	    						curr.incrementReceivedHits(damage);
	    						int currVisual = 0;
								int _x = layers.get(i).spriteFactory.sprites.get(j).getSprite().getX();
								int _y = layers.get(i).spriteFactory.sprites.get(j).getSprite().getY();

								int _max = 0;
								for(int y = 0; y< curr.visuals.size(); y++)
	    						{
									if(curr.visuals.get(y).getAfterShot()> _max && curr.visuals.get(y).getAfterShot()< curr.getCurrHits())
									{
										currVisual = y;
									}
	    						}

								Sprite s = curr.visuals.get(currVisual).getSprite();
								s.setPosition(_x, _y);
	    						SpriteData data = new SpriteData(s, layers.get(i).spriteFactory.sprites.get(j).getId());
	    						
	    						layers.get(i).spriteFactory.sprites.set(j, data);

	    						final String id = layers.get(i).spriteFactory.sprites.get(j).getId();
	    						//layers.get(i).spriteFactory.setVisible(id, false);

	    	            		final MovieClip clip = new MovieClip(80);
    							clip.getSpriteFactory().add(id+ "_HIT_0", curr.visuals.get(currVisual).getHitSprite());
	    				    	clip.setRepetition(1);

	    				    	layers.get(i).movieClipFactory.add(id+ "_HIT", clip);

	    				    	layers.get(i).movieClipFactory.setPosition(id+ "_HIT", _x, _y);
	    				    	layers.get(i).movieClipFactory.play(id+ "_HIT");

	    				    	final int _i = i;
	    				    	RunAfterFunction func = new RunAfterFunction() {
	    			                @Override
	    				    		public void run()
	    				    		{
			    						layers.get(_i).spriteFactory.setVisible(id, true);
	    				    		}
	    				    	};

	    				    	layers.get(i).movieClipFactory.runAfterPlay(layers.get(i).spriteFactory.sprites.get(j).getId()+ "_HIT", func);
    						}
    					}
    				}
    			}
    		}
	    		
			// it is a character
			for(int i = 0; i< layers.size(); i++)
			{
				for(int j = 0; j< layers.get(i).movieClipFactory.movieClips.size(); j++)
				{
					String id = layers.get(i).movieClipFactory.movieClips.get(j).getId();
					com.tolka.shooterdudes.StageInfo.Character curc = findCharacterById(id);
					
					if(curc != null && collidedObject.equals(id))
					{
						MovieClip clip = layers.get(i).movieClipFactory.movieClips.get(j).getClip();
						for(int t = 0; t< clip.getSpriteFactory().sprites.size(); t++)
						{
							if(clip.getSpriteFactory().sprites.get(t).getVisible())
							{
	    						for(int g = 0; g< curc.getMovements().size(); g++)
	    						{
	    							// process corresponding action
	    							if(curc.getShootAction().equals("speedup2x"))
	    							{
	    								int appearX = clip.getSpriteFactory().sprites.get(t).getSprite().getX();
	    								int appearY = clip.getSpriteFactory().sprites.get(t).getSprite().getY();

	    								setCharacterFrameDelay(curc.getId(), curc.getMovements().get(g).getFrameDelay() / 2);
	    								scoreToSet = score + 10000;
	    								score += 10000;
    		                	    	
	    								findLayerById(121).spriteFactory.setPosition("SCORE1", appearX, appearY);
	    								findLayerById(121).spriteFactory.setVisible("SCORE1", true);
	    								findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_Y, findSpriteById(121, "SCORE1"), appearY, appearY - 200 * m_ratio, 400.0), 0, -1);
	    								findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_Y, findSpriteById(121, "SCORE1"), appearY - 200 * m_ratio, appearY + 120 * m_ratio, 750.0), 400, -1);
	    								findLayerById(121).tweenFactory.add(new MotionTween(MotionTween.ATTR_ALPHA, findSpriteById(121, "SCORE1"), 1.0, 0.0, 1000.0), 3000, -1);
	    							}
	    							else if(curc.getShootAction().equals("die") && curc.getMovements().get(g).getType().equals("die"))
	    							{
	    	    						//timeLineLog += "character "+ clip.getSpriteFactory().sprites.get(t).getId()+ " dies now\n";

	    								int appearX = clip.getSpriteFactory().sprites.get(t).getSprite().getX();
	    								int appearY = clip.getSpriteFactory().sprites.get(t).getSprite().getY();

										clip.getSpriteFactory().invisibleAll();
										layers.get(i).movieClipFactory.stop(id);
										
										// increment points
										score += curc.getPoints();
										scoreToSet = score;
		
										// increment foe
		    							currFoe += curc.getFoe();
		    							if(currFoe >= targetFoe)
		    							{
		    								// stage completed
		    							}

										// add die clip
		    							MovieClip _clip = new MovieClip(curc.getMovements().get(g).getFrameDelay());
	    						    	Layer layer = findLayerById(curc.getLayer(), true);

	    								for(int r = 0; r< curc.getMovements().get(g).getKeyframes().size(); r++)
	    								{
	    									Sprite sprite = curc.getMovements().get(g).getKeyframes().get(r).getSprite();
	    									_clip.getSpriteFactory().add(curc.getId()+ "_DIE_"+ r, sprite);
	    								}

	    						    	_clip.setRepetition(1);

	    						    	layer.movieClipFactory.add(curc.getId()+ "_DIE", _clip);
	    						    	layer.movieClipFactory.setPosition(curc.getId()+ "_DIE", (int)Math.floor(appearX), (int)Math.floor((appearY- YBIAS)* m_ratio));
	    						    	layer.movieClipFactory.play(curc.getId()+ "_DIE");

	    						    	final com.tolka.shooterdudes.StageInfo.Character curcF = curc;
	    	    				    	RunAfterFunction func = new RunAfterFunction() {
	    	    			                @Override
	    	    				    		public void run()
	    	    				    		{
	    	    			    				characterToInitialPosition(curcF.getId());
	    	    				    		}
	    	    				    	};

	    	    				    	layers.get(i).movieClipFactory.runAfterPlay(curc.getId()+ "_DIE", func);	    	    				    	
	    							}
	    						}
							}
						}
	    			}
	    		}
	    	}
    	}
    }
	private void characterToInitialPosition(String id)
	{
    	for(int i = 0; i< stageInfo.getFrames().size(); i++)
    	{
			for(int k = 0; k< stageInfo.getFrames().get(i).getCharacters().size(); k++)
			{
				FrameCharacter character = stageInfo.getFrames().get(i).getCharacters().get(k);
				if(character.getId().equals(id))
				{
					character.toInitialState();
					return;
				}
			}
    	}		
	}
	private void setCharacterFrameDelay(String id, int newFrameDelay)
	{
		for(int i = 0; i< stageInfo.getCharacters().size(); i++)
		{
			if(id.equals(stageInfo.getCharacters().get(i).getId()))
			{
				for(int j = 0; j< stageInfo.getCharacters().get(i).getMovements().size(); j++)
				{
					int newValue = stageInfo.getCharacters().get(i).getMovements().get(j).getFrameDelay() / 2;
					stageInfo.getCharacters().get(i).getMovements().get(j).setFrameDelay(newValue);
				}
			}
		}
	}
	private com.tolka.shooterdudes.StageInfo.Character findCharacterById(String id)
	{
		for(int i = 0; i< stageInfo.getCharacters().size(); i++)
		{
			if(id.indexOf(stageInfo.getCharacters().get(i).getId()+ "_") == 0)
			{
				return stageInfo.getCharacters().get(i);
			}
		}
		
		return null;
	}

	private Building findBuildingById(String id)
	{
		for(int i = 0; i< stageInfo.getBuildings().size(); i++)
		{
			if(stageInfo.getBuildings().get(i).getId().equals(id))
			{
				return stageInfo.getBuildings().get(i);
			}
		}
		
		return null;
	}
	
	private String hitTest() {
		for(int i = layers.size()- 1; i >= 0; i--)
		{
			if(layers.get(i).id >= 99 || layers.get(i).id == 0)
			{
				continue;
			}
			
			// 1.Buildings
			for(int j = 0; j< layers.get(i).spriteFactory.sprites.size(); j++)
			{
				String id = layers.get(i).spriteFactory.sprites.get(j).getId();
				if(findBuildingById(id) != null)
				{
					// it is a building
					Sprite sprite = layers.get(i).spriteFactory.sprites.get(j).getSprite();
					if(crossairX >= sprite.getX() && crossairX <= sprite.getX()+ sprite.getWidth() &&
						crossairY >= sprite.getY() && crossairY <= sprite.getY()+ sprite.getHeight())
					{
						//id += "["+ Math.floor(crossairX) +", "+ Math.floor(crossairY) +" :: "+ sprite.getX() +", "+ sprite.getY() +"] "+ Double.toString(((int)Math.floor(crossairX- sprite.getX())/m_ratio))+ " :: "+ ((int)Math.floor(crossairY- sprite.getY())/m_ratio);

						try
						{
							Bitmap bmp = Bitmap.createScaledBitmap(sprite.getBitmap(), (int)(sprite.getWidth()* sprite.getScaleX()), (int)(sprite.getHeight()* sprite.getScaleX()), false);  
							int pxl = bmp.getPixel((int)((crossairX- sprite.getX())/m_ratio), (int)((crossairY- sprite.getY())/m_ratio));
							int alpha = Color.alpha(pxl);
							if(alpha == 0)
							{
								//return id + " - blank";
								continue;
							}
						} catch(Exception ex)
						{
							timeLineLog += "f1 ("+ j +" - "+ ex.getMessage() +")\n";
						}
						
						return id;
					}
				}
			}

			// 2.Moving things
			for(int j = 0; j< layers.get(i).movieClipFactory.movieClips.size(); j++)
			{
				String id = layers.get(i).movieClipFactory.movieClips.get(j).getId();
				com.tolka.shooterdudes.StageInfo.Character character = findCharacterById(id);
				if(character != null)
				{
					MovieClip clip = layers.get(i).movieClipFactory.movieClips.get(j).getClip();
					for(int t = 0; t< clip.getSpriteFactory().sprites.size(); t++)
					{
						if(clip.getSpriteFactory().sprites.get(t).getVisible())
						{
							Sprite sprite = clip.getSpriteFactory().sprites.get(t).getSprite();
							if(crossairX >= sprite.getX() * sprite.getScaleX() && crossairX <= sprite.getX()+ sprite.getWidth() * sprite.getScaleX() &&
								crossairY >= sprite.getY() * sprite.getScaleY() && crossairY <= sprite.getY()+ sprite.getHeight() * sprite.getScaleY())
							{
								try
								{
									int pxl = sprite.getBitmap().getPixel((int)((crossairX- sprite.getX())/m_ratio* sprite.getScaleX()), (int)((crossairY- sprite.getY())/m_ratio* sprite.getScaleY()));
									int alpha = Color.alpha(pxl);
									if(alpha == 0)
									{
										continue;
									}
								} catch(Exception ex)
								{
								}

    							return id;
							}
							
							break;
						}
					}
				}
			}
		}
		
		return null;
	}
	
	// textures
	private void addSpriteToLayer(int layer, String id, String fileName, int width, int height, int left, int top, GL10 gl, boolean visible)
	{
		File file = new File(fileName);
		if(!file.exists())
		{
			fileName = "ui/" + fileName;
		}

    	Sprite sprite = new Sprite(fileName, width, height, gl, context, m_height);
    	sprite.setPosition(left, top);
    	findLayerById(layer).spriteFactory.add(id, sprite);
    	findLayerById(layer).spriteFactory.setVisible(id, visible);
	}
    private void createTextures(GL10 gl)
    {
    	addSpriteToLayer(100, "LOADINGFIRST", "loading_screen.png", (int)(1100* m_ratio), (int)(644* m_ratio), 0, 0, gl, true);
    	addSpriteToLayer(101, "PROGRESS", "progress.png", (int)(40* m_ratio), (int)(40* m_ratio), (int)Math.floor(176* m_ratio), (int)Math.floor(527* m_ratio), gl, true);
    	addSpriteToLayer(103, "PROGRESS_SHIELD", "progress.png", (int)(2.3* m_ratio), (int)(40* m_ratio), (int)Math.floor(176* m_ratio), (int)Math.floor(527* m_ratio), gl, false);
    	addSpriteToLayer(103, "PROGRESS_SANDCLOCK", "progress.png", (int)(2.3* m_ratio), (int)(40* m_ratio), (int)Math.floor(176* m_ratio), (int)Math.floor(527* m_ratio), gl, false);

    	findLayerById(99).textFactory.add("SCORE", new Text("000000", gl, context, m_ratio, m_height));
		findLayerById(131).textFactory.add("GOLD", new Text("0", gl, context, m_ratio, m_height));
		findLayerById(131).textFactory.add("MONEY", new Text("000000", gl, context, m_ratio, m_height));

		findLayerById(91).textFactory.add("SHIELD_DISP", new Text(""+ gameInfo.shields, gl, context, m_ratio, m_height));
		findLayerById(91).textFactory.add("BOMB_DISP", new Text(""+ gameInfo.bombs, gl, context, m_ratio, m_height));
		findLayerById(91).textFactory.add("SANDCLOCK_DISP", new Text(""+ gameInfo.sandclocks, gl, context, m_ratio, m_height));

		int price = 500;
		switch(gameInfo.getShieldDuration())
		{
		case 1: price = 1000; break;
		case 2: price = 2000; break;
		case 3: price = 5000; break;
		case 4: price = 10000; break;
		case 5: price = 20000; break;
		}

		findLayerById(93).textFactory.add("UPGRADE_SHIELD_PRICE", new Text(""+ price, gl, context, m_ratio, m_height));

		price = 500;
		switch(gameInfo.getBombPower())
		{
		case 1: price = 1000; break;
		case 2: price = 2000; break;
		case 3: price = 5000; break;
		case 4: price = 10000; break;
		case 5: price = 20000; break;
		}

		findLayerById(93).textFactory.add("UPGRADE_BOMB_PRICE", new Text(""+ price, gl, context, m_ratio, m_height));

		price = 500;
		switch(gameInfo.getSandclockDuration())
		{
		case 1: price = 1000; break;
		case 2: price = 2000; break;
		case 3: price = 5000; break;
		case 4: price = 10000; break;
		case 5: price = 20000; break;
		}

		findLayerById(93).textFactory.add("UPGRADE_SANDCLOCK_PRICE", new Text(""+ price, gl, context, m_ratio, m_height));
		
		findLayerById(131).textFactory.setScale("GOLD", 0.75f);
		findLayerById(131).textFactory.setScale("MONEY", 0.75f);

		findLayerById(91).textFactory.setScale("SHIELD_DISP", 0.7f);
		findLayerById(91).textFactory.setScale("BOMB_DISP", 0.7f);
		findLayerById(91).textFactory.setScale("SANDCLOCK_DISP", 0.7f);
		
		findLayerById(93).textFactory.setScale("UPGRADE_SHIELD_PRICE", 0.6f);
		findLayerById(93).textFactory.setScale("UPGRADE_BOMB_PRICE", 0.6f);
		findLayerById(93).textFactory.setScale("UPGRADE_SANDCLOCK_PRICE", 0.6f);

		findLayerById(99).textFactory.setVisible("SCORE", false);
		findLayerById(131).textFactory.setVisible("GOLD", false);
		findLayerById(131).textFactory.setVisible("MONEY", false);

		addTextureToLoad(100, "TOLKA", "tolka.png", (int)(1100* m_ratio), (int)(644* m_ratio), 0, 0, false);
		addTextureToLoad(900, "_DEBUG", "debug.png", (int)(50* m_ratio), (int)(63* m_ratio), (int)(80* m_ratio), (int)(m_height- 68* m_ratio), false);
		addTextureToLoad(130, "DARKBG", "darkbg.png", (int)(1100* m_ratio), (int)(644* m_ratio), 0, 0, false);
		addTextureToLoad(99, "WOODSHOLE", "ui_woods_hole.png", (int)(1100* m_ratio), (int)(680* m_ratio), 0, 0, false);
		addTextureToLoad(120, "FRAME1", "frame1.png", (int)(922* m_ratio), (int)(509* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2), (int)Math.floor((644- 509+ 50)* m_ratio/ 2), false);
		addTextureToLoad(101, "WAIT0", "wait0.png", (int)(358* m_ratio), (int)(112* m_ratio), (int)Math.floor((1100- 458)* m_ratio/ 2+ (int)(110* m_ratio)), (int)Math.floor((644- 112+ 10)* m_ratio/ 2), false);
    	addTextureToLoad(121, "ARROW_LEFT_LVL", "arrow_left_alpha.png", (int)(69* m_ratio), (int)(69* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(-5* m_ratio)), (int)(288* m_ratio), false);
    	addTextureToLoad(121, "ARROW_RIGHT_LVL", "arrow_right_alpha.png", (int)(69* m_ratio), (int)(69* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(858* m_ratio)), (int)(288* m_ratio), false);
		addTextureToLoad(101, "LEVEL1", "level1.png", (int)(158* m_ratio), (int)(331* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(100* m_ratio)), (int)Math.floor((644- 311+ 10)* m_ratio/ 2), false);
		addTextureToLoad(101, "LEVEL2", "level0.png", (int)(158* m_ratio), (int)(331* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(290* m_ratio)), (int)Math.floor((644- 311+ 10)* m_ratio/ 2), false);
		addTextureToLoad(101, "LEVEL3", "level0.png", (int)(158* m_ratio), (int)(331* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(480* m_ratio)), (int)Math.floor((644- 311+ 10)* m_ratio/ 2), false);
		addTextureToLoad(101, "LEVEL4", "level0.png", (int)(158* m_ratio), (int)(331* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(670* m_ratio)), (int)Math.floor((644- 311+ 10)* m_ratio/ 2), false);
		addTextureToLoad(90, "FRAMEBG", "frameinside1.png", (int)(864* m_ratio), (int)(458* m_ratio), (int)Math.floor((1100- 846)* m_ratio/ 2), (int)Math.floor((644- 458+ 50)* m_ratio/ 2), false);
		addTextureToLoad(103, "SMALLOGO", "logo_small.png", (int)(310* m_ratio), (int)(81* m_ratio), (int)Math.floor(782* m_ratio), (int)Math.floor(4* m_ratio), false);
		addTextureToLoad(101, "TAB1HOT", "tab1_hot.png", (int)(185* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(20* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2)- (int)(31* m_ratio), false);
    	addTextureToLoad(101, "TAB1", "tab1.png", (int)(185* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(20* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2)- (int)(31* m_ratio), false);
    	addTextureToLoad(101, "TAB2HOT", "tab2_hot.png", (int)(161* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(205* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "TAB2", "tab2.png", (int)(161* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(205* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "TAB3HOT", "tab3_hot.png", (int)(151* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(366* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "TAB3", "tab3.png", (int)(151* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(366* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "TAB4HOT", "tab4_hot.png", (int)(151* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(517* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "TAB4", "tab4.png", (int)(151* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(517* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(131, "NOTENOUGHMONEY_BG", "ui_notenoughmoney_bg.png", (int)(700* m_ratio), (int)(350* m_ratio), (int)Math.floor((1100- 700)* m_ratio/ 2), (int)Math.floor((644- 350)* m_ratio/ 2), false);
    	addTextureToLoad(131, "SUCCESS_BG", "ui_success.png", (int)(700* m_ratio), (int)(350* m_ratio), (int)Math.floor((1100- 700)* m_ratio/ 2), (int)Math.floor((644- 350)* m_ratio/ 2), false);
    	addTextureToLoad(100, "INTRO", "main0.png", (int)(1100* m_ratio), (int)(644* m_ratio), 0, 0, false);
    	addTextureToLoad(100, "INTRODARK", "main0dark.png", (int)(1100* m_ratio), (int)(644* m_ratio), 0, 0, false);
    	addTextureToLoad(100, "INTRO_TAP", "intro0_tap.png", (int)(342* m_ratio), (int)(30* m_ratio), (int)Math.floor(380* m_ratio), (int)Math.floor(558* m_ratio), false);
    	addTextureToLoad(100, "INTRO_TITLE", "intro0_title.png", (int)(796* m_ratio), (int)(207* m_ratio), (int)Math.floor(152* m_ratio), (int)Math.floor(125* m_ratio), false);
    	addTextureToLoad(122, "BGWOODS", "bgwoods.png", (int)(1100* m_ratio), (int)(680* m_ratio), 0, 0, false);
    	addTextureToLoad(101, "MENU1", "menu1.png", (int)(550* m_ratio), (int)(600* m_ratio), (int)Math.floor(((1100-550)/ 2)* m_ratio), (int)Math.floor(((644- 600)/ 2)* m_ratio), false);
    	addTextureToLoad(100, "TOLKATAP", "tolka_tapscreen.png", (int)(400* m_ratio), (int)(50* m_ratio), (int)Math.floor(((1100- 400)/ 2)* m_ratio), (int)Math.floor(550* m_ratio), false);
    	addTextureToLoad(100, "LOADING", "loading.png", (int)(1100* m_ratio), (int)(644* m_ratio), 0, 0, false);
    	addTextureToLoad(101, "CROSSAIR", "ch1.png", (int)(83* m_ratio), (int)(72* m_ratio), (int)(-41* m_ratio), (int)(-36* m_ratio), false);
    	addTextureToLoad(131, "UISTATS", "ui_stats.png", (int)(125* m_ratio), (int)(25* m_ratio), (int)(105* m_ratio), (int)(11* m_ratio), false);
    	addTextureToLoad(99, "UIBOMB", "ui_bomb.png", (int)(50* m_ratio), (int)(63* m_ratio), (int)(80* m_ratio), (int)(m_height- 68* m_ratio), false);
    	addTextureToLoad(99, "UISANDCLOCK", "ui_sandclock.png", (int)(50* m_ratio), (int)(63* m_ratio), (int)(150* m_ratio), (int)(m_height- 68* m_ratio), false);
    	addTextureToLoad(99, "UISHIELD", "ui_shield.png", (int)(50* m_ratio), (int)(63* m_ratio), (int)(10* m_ratio), (int)(m_height- 68* m_ratio), false);
    	addTextureToLoad(91, "MAP1", "map1.png", (int)(2493* m_ratio), (int)(916* m_ratio), 0, 0, false);
    	addTextureToLoad(100, "SELECTOR1_1", "level_selector0.png", (int)(349* m_ratio), (int)(446* m_ratio), (int)(634* m_ratio), (int)(123* m_ratio), false);
    	addTextureToLoad(121, "ARROW_LEFT", "arrow_left.png", (int)(69* m_ratio), (int)(69* m_ratio), (int)(604* m_ratio), (int)(248* m_ratio), false);
    	addTextureToLoad(121, "ARROW_LEFT_ALPHA", "arrow_left_alpha.png", (int)(69* m_ratio), (int)(69* m_ratio), (int)(604* m_ratio), (int)(248* m_ratio), false);
    	addTextureToLoad(121, "ARROW_RIGHT", "arrow_right.png", (int)(69* m_ratio), (int)(69* m_ratio), (int)(942* m_ratio), (int)(248* m_ratio), false);
    	addTextureToLoad(121, "ARROW_RIGHT_ALPHA", "arrow_right_alpha.png", (int)(69* m_ratio), (int)(69* m_ratio), (int)(942* m_ratio), (int)(248* m_ratio), false);
    	addTextureToLoad(101, "THUMB_TOWN_0", "thumb_town_0.png", (int)(323* m_ratio), (int)(189* m_ratio), (int)(646* m_ratio), (int)(188* m_ratio), false);
    	addTextureToLoad(101, "THUMB_TOWN_1_1", "thumb_town_1_1.png", (int)(323* m_ratio), (int)(189* m_ratio), (int)(646* m_ratio), (int)(188* m_ratio), false);
    	addTextureToLoad(101, "PLAYLEVEL", "btn_play_level.png", (int)(231* m_ratio), (int)(106* m_ratio), (int)(698* m_ratio), (int)(453* m_ratio), false);
    	addTextureToLoad(101, "PLAYALPHA", "btn_play_level_alpha.png", (int)(231* m_ratio), (int)(106* m_ratio), (int)(698* m_ratio), (int)(453* m_ratio), false);
    	addTextureToLoad(100, "TARGET0", "target0.png", (int)(213* m_ratio), (int)(213* m_ratio), 0, 0, false);
    	addTextureToLoad(100, "TARGET1", "target1.png", (int)(213* m_ratio), (int)(213* m_ratio), 0, 0, false);
		addTextureToLoad(101, "LTAB1HOT", "level_tab1_info_hot.png", (int)(185* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(20* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2)- (int)(31* m_ratio), false);
    	addTextureToLoad(101, "LTAB1", "level_tab1_info.png", (int)(185* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(20* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2)- (int)(31* m_ratio), false);
    	addTextureToLoad(101, "LTAB2HOT", "level_tab2_highscores_hot.png", (int)(161* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(205* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "LTAB2", "level_tab2_highscores.png", (int)(161* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(205* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "LTAB3HOT", "level_tab3_worldmap_hot.png", (int)(151* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(366* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
    	addTextureToLoad(101, "LTAB3", "level_tab3_worldmap.png", (int)(151* m_ratio), (int)(71* m_ratio), (int)Math.floor((1100- 922)* m_ratio/ 2+ (int)(366* m_ratio)), (int)Math.floor((644- 509)* m_ratio/ 2- (int)(31* m_ratio)), false);
		addTextureToLoad(91, "MAP1MINI", "map1_mini.png", (int)(856* m_ratio), (int)(315* m_ratio), (int)Math.floor((1100- 856)* m_ratio/ 2), (int)Math.floor((644- 315+ 50)* m_ratio/ 2), false);
    	addTextureToLoad(101, "IND_SANDCLOCK", "ui_ind_sandclock.png", (int)(310* m_ratio), (int)(70* m_ratio), (int)Math.floor(-320* m_ratio), (int)Math.floor(70* m_ratio), false);
    	addTextureToLoad(101, "IND_SHIELD", "ui_ind_shield.png", (int)(310* m_ratio), (int)(70* m_ratio), (int)Math.floor(-320* m_ratio), (int)Math.floor(70* m_ratio), false);
    	addTextureToLoad(121, "DAMAGE", "red.png", (int)(1100* m_ratio), (int)(680* m_ratio), (int)Math.floor(0* m_ratio), (int)Math.floor(0* m_ratio), false);
    	addTextureToLoad(121, "NODAMAGE", "green.png", (int)(1100* m_ratio), (int)(680* m_ratio), (int)Math.floor(0* m_ratio), (int)Math.floor(0* m_ratio), false);
    	addTextureToLoad(121, "SCORE1", "score1.png", (int)(232* m_ratio), (int)(63* m_ratio), (int)Math.floor(0* m_ratio), (int)Math.floor(0* m_ratio), false);
    	addTextureToLoad(121, "DECAL", "decal.png", (int)(128* m_ratio), (int)(128* m_ratio), (int)Math.floor(0* m_ratio), (int)Math.floor(0* m_ratio), false);
    	addTextureToLoad(121, "GAMEEND", "white.png", (int)(1100* m_ratio), (int)(680* m_ratio), (int)Math.floor(0* m_ratio), (int)Math.floor(0* m_ratio), false);
    	addTextureToLoad(122, "GAMELOST", "outcome_defeat.png", (int)(512* m_ratio), (int)(512* m_ratio), (int)Math.floor(294* m_ratio), (int)Math.floor(84* m_ratio), false);
    	addTextureToLoad(122, "GAMEWON", "outcome_victory.png", (int)(512* m_ratio), (int)(512* m_ratio), (int)Math.floor(294* m_ratio), (int)Math.floor(84* m_ratio), false);
    	
    	// user interface
		Layer uiLayer = findLayerById(99);
    	handleUINumbers();

    	// town names
    	for(int i = 1; i <= 35; i++)
    	{
    		addTextureToLoad(121, "TOWNNAME_1_"+ i, "townname_1_"+ i +".png", (int)(350* m_ratio), (int)(38* m_ratio), (int)(634* m_ratio), (int)(147* m_ratio), false);
    	}
    	
    	// foe
    	addTextureToLoad(99, "FOETITLE", "ui_foe_title.png", (int)(62* m_ratio), (int)(32* m_ratio), (int)((m_width/ 2)- 277* m_ratio), (int)(m_height- 40* m_ratio), false);
    	
		for(int g = 0; g< 20; g++)
    	{
			addTextureToLoad(99, "FOEEMPTY"+ (g+ 1), "ui_foe_empty.png", (int)(22* m_ratio), (int)(32* m_ratio), (int)((m_width/ 2)- (213- 24* g)* m_ratio), (int)(m_height- 40* m_ratio), false);
			addTextureToLoad(99, "FOEFULL"+ (g+ 1), "ui_foe_full.png", (int)(22* m_ratio), (int)(32* m_ratio), (int)((m_width/ 2)- (213- 24* g)* m_ratio), (int)(m_height- 40* m_ratio), false);
    	}

		for(int g = 0; g< 6; g++)
		{
			addTextureToLoad(99, "BULLET"+ (g+ 1), "bullet.png", (int)(20* m_ratio), (int)(55* m_ratio), (int)((5+ g* 25)* m_ratio), (int)(5* m_ratio), false);
		}

		uiLayer.spriteFactory.add("RELOAD", new Sprite("ui/btn_reload.png", (int)(155* m_ratio), (int)(66* m_ratio), gl, context, m_height));
		uiLayer.spriteFactory.setVisible("RELOAD", false);
		uiLayer.spriteFactory.setPosition("RELOAD", (int)(5* m_ratio), (int)(5* m_ratio));

		// surprise chest
		addTextureToLoad(131, "SURPRISECHEST_BACKGROUND", "schestbg0.png", (int)(700* m_ratio), (int)(460* m_ratio), (int)(200* m_ratio), (int)(92* m_ratio), false);
		addTextureToLoad(132, "BTNCLOSE", "btn_close.png", (int)(170* m_ratio), (int)(60* m_ratio), (int)(465* m_ratio), (int)(480* m_ratio), false);
		addTextureToLoad(132, "BTNSTORE", "btn_store.png", (int)(170* m_ratio), (int)(60* m_ratio), (int)(465* m_ratio), (int)(480* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_TAPTOOPEN", "txt_taptoopen.png", (int)(235* m_ratio), (int)(30* m_ratio), (int)(433* m_ratio), (int)(407* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_STARS", "schestbg1.png", (int)(700* m_ratio), (int)(460* m_ratio), (int)(200* m_ratio), (int)(92* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_BIG", "schest.png", (int)(271* m_ratio), (int)(211* m_ratio), (int)(415* m_ratio), (int)(160* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_ROCKS", "chestitem_rocks.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_MONEY1000", "chestitem_money_1000.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_MONEY500", "chestitem_money_500.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_MONEY100", "chestitem_money_100.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_MONEY7", "chestitem_money_7.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_SANDCLOCKS5", "chestitem_sandclock5.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_SANDCLOCKS3", "chestitem_sandclock3.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_SANDCLOCKS1", "chestitem_sandclock1.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_BOMBS5", "chestitem_bombs5.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_BOMBS3", "chestitem_bombs3.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_BOMBS1", "chestitem_bombs1.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_SHIELDS5", "chestitem_shields5.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_SHIELDS3", "chestitem_shields3.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_SHIELDS1", "chestitem_shields1.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_GOLD10", "chestitem_gold10.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_GOLD3", "chestitem_gold3.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_GOLD2", "chestitem_gold2.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(132, "SURPRISECHEST_ITEM_GOLD1", "chestitem_gold1.png", (int)(589* m_ratio), (int)(456* m_ratio), (int)(255* m_ratio), (int)(94* m_ratio), false);
		addTextureToLoad(91, "STORE_MONEY_10000", "store_band_money10000.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(130* m_ratio), false);
		addTextureToLoad(91, "STORE_MONEY_20000", "store_band_money20000.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(245* m_ratio), false);
		addTextureToLoad(91, "STORE_MONEY_50000", "store_band_money50000.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(360* m_ratio), false);
		addTextureToLoad(91, "STORE_MONEY_100000", "store_band_money100000.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(475* m_ratio), false);
		addTextureToLoad(91, "STORE_GOLD_10", "store_bar_of_gold_10.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(620* m_ratio), false);
		addTextureToLoad(91, "STORE_GOLD_20", "store_bar_of_gold_20.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(735* m_ratio), false);
		addTextureToLoad(91, "STORE_GOLD_50", "store_bar_of_gold_50.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(850* m_ratio), false);
		addTextureToLoad(91, "STORE_GOLD_100", "store_bar_of_gold_100.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(965* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_10000", "btn_buy_usd049.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(145* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_10000_HOT", "btn_buy_usd049_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(145* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_20000", "btn_buy_usd099.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(260* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_20000_HOT", "btn_buy_usd099_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(260* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_50000", "btn_buy_usd149.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(375* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_50000_HOT", "btn_buy_usd149_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(375* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_100000", "btn_buy_usd199.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(490* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_MONEY_100000_HOT", "btn_buy_usd199_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(490* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_10", "btn_buy_usd049.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(635* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_10_HOT", "btn_buy_usd049_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(635* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_20", "btn_buy_usd099.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(750* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_20_HOT", "btn_buy_usd099_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(750* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_50", "btn_buy_usd149.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(865* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_50_HOT", "btn_buy_usd149_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(865* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_100", "btn_buy_usd199.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(980* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_GOLD_100_HOT", "btn_buy_usd199_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(980* m_ratio), false);
		addTextureToLoad(91, "BOOST_SHIELD", "boost_band_shield.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(130* m_ratio), false);
		addTextureToLoad(91, "BOOST_BOMB", "boost_band_bomb.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(245* m_ratio), false);
		addTextureToLoad(91, "BOOST_SANDCLOCK", "boost_band_sandclock.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(360* m_ratio), false);
		addTextureToLoad(91, "BOOST_SURPRISECHEST", "boost_band_surprisechest.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(475* m_ratio), false);
		addTextureToLoad(91, "BOOST_SHIELD_UPGRADE", "boost_band_shied_upgrade.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(620* m_ratio), false);
		addTextureToLoad(91, "BOOST_BOMB_UPGRADE", "boost_band_bomb_upgrade.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(735* m_ratio), false);
		addTextureToLoad(91, "BOOST_SANDCLOCK_UPGRADE", "boost_band_sandclock_upgrade.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(850* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_SHIELD", "btn_buy.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(145* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_SHIELD_HOT", "btn_buy_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(145* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_BOMB", "btn_buy.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(260* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_BOMB_HOT", "btn_buy_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(260* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_SANDCLOCK", "btn_buy.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(375* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_SANDCLOCK_HOT", "btn_buy_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(375* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_SURPRISECHEST", "btn_buy.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(490* m_ratio), false);
		addTextureToLoad(92, "BTN_BUY_SURPRISECHEST_HOT", "btn_buy_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(490* m_ratio), false);
		addTextureToLoad(92, "BTN_UPGRADE_SHIELD", "btn_upgrade.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(635* m_ratio), false);
		addTextureToLoad(92, "BTN_UPGRADE_SHIELD_HOT", "btn_upgrade_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(635* m_ratio), false);
		addTextureToLoad(92, "BTN_UPGRADE_BOMB", "btn_upgrade.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(750* m_ratio), false);
		addTextureToLoad(92, "BTN_UPGRADE_BOMB_HOT", "btn_upgrade_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(750* m_ratio), false);
		addTextureToLoad(92, "BTN_UPGRADE_SANDCLOCK", "btn_upgrade.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(865* m_ratio), false);
		addTextureToLoad(92, "BTN_UPGRADE_SANDCLOCK_HOT", "btn_upgrade_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(865* m_ratio), false);
		addTextureToLoad(91, "BOOST_EARN_1000", "boost_band_earn1000.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(130* m_ratio), false);
		addTextureToLoad(91, "BOOST_EARN_2GOLD", "boost_band_earn2gold.png", (int)(822* m_ratio), (int)(109* m_ratio), (int)(145* m_ratio), (int)(245* m_ratio), false);
		addTextureToLoad(92, "BTN_CLAIM_1000", "btn_claim.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(145* m_ratio), false);
		addTextureToLoad(92, "BTN_CLAIM_1000_HOT", "btn_claim_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(145* m_ratio), false);
		addTextureToLoad(92, "BTN_CLAIM_2GOLD", "btn_claim.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(260* m_ratio), false);
		addTextureToLoad(92, "BTN_CLAIM_2GOLD_HOT", "btn_claim_hot.png", (int)(180* m_ratio), (int)(71* m_ratio), (int)(765* m_ratio), (int)(260* m_ratio), false);

		addTextureToLoad(92, "PRGRSS_SHIELD_LVL1", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(283* m_ratio), (int)(686* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SHIELD_LVL2", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(348* m_ratio), (int)(686* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SHIELD_LVL3", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(413* m_ratio), (int)(686* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SHIELD_LVL4", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(478* m_ratio), (int)(686* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SHIELD_LVL5", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(543* m_ratio), (int)(686* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SHIELD_LVL6", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(608* m_ratio), (int)(686* m_ratio), false);

		addTextureToLoad(92, "PRGRSS_BOMB_LVL1", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(283* m_ratio), (int)(801* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_BOMB_LVL2", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(348* m_ratio), (int)(801* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_BOMB_LVL3", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(413* m_ratio), (int)(801* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_BOMB_LVL4", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(478* m_ratio), (int)(801* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_BOMB_LVL5", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(543* m_ratio), (int)(801* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_BOMB_LVL6", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(608* m_ratio), (int)(801* m_ratio), false);

		addTextureToLoad(92, "PRGRSS_SANDCLOCK_LVL1", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(283* m_ratio), (int)(916* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SANDCLOCK_LVL2", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(348* m_ratio), (int)(916* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SANDCLOCK_LVL3", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(413* m_ratio), (int)(916* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SANDCLOCK_LVL4", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(478* m_ratio), (int)(916* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SANDCLOCK_LVL5", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(543* m_ratio), (int)(916* m_ratio), false);
		addTextureToLoad(92, "PRGRSS_SANDCLOCK_LVL6", "ui_bar_full.png", (int)(56* m_ratio), (int)(27* m_ratio), (int)(608* m_ratio), (int)(916* m_ratio), false);

    	//findLayerById(92).textFactory.setPosition("UPGRADE_SHIELD", (int)(405* m_ratio), (int)(180* m_ratio));

		findLayerById(93).textFactory.setPosition("UPGRADE_SHIELD_PRICE", (int)(843* m_ratio), (int)(667* m_ratio));
		findLayerById(93).textFactory.setPosition("UPGRADE_BOMB_PRICE", (int)(843* m_ratio), (int)(782* m_ratio));
		findLayerById(93).textFactory.setPosition("UPGRADE_SANDCLOCK_PRICE", (int)(843* m_ratio), (int)(897* m_ratio));

		findLayerById(93).textFactory.setVisible("UPGRADE_SHIELD_PRICE", false);
		findLayerById(93).textFactory.setVisible("UPGRADE_BOMB_PRICE", false);
		findLayerById(93).textFactory.setVisible("UPGRADE_SANDCLOCK_PRICE", false);

		findLayerById(91).textFactory.setPosition("SHIELD_DISP", (int)(405* m_ratio), (int)(180* m_ratio));
    	findLayerById(91).textFactory.setPosition("BOMB_DISP", (int)(405* m_ratio), (int)(295* m_ratio));
    	findLayerById(91).textFactory.setPosition("SANDCLOCK_DISP", (int)(405* m_ratio), (int)(410* m_ratio));
		
    	findLayerById(91).textFactory.setVisible("SHIELD_DISP", false);
    	findLayerById(91).textFactory.setVisible("BOMB_DISP", false);
    	findLayerById(91).textFactory.setVisible("SANDCLOCK_DISP", false);
    	
		MovieClip clip = new MovieClip(25);
    	clip.getSpriteFactory().add("ch2", new Sprite("ui/ch2.png", (int)(82* m_ratio), (int)(71* m_ratio), gl, this.context, m_height));
    	clip.getSpriteFactory().add("ch3", new Sprite("ui/ch3.png", (int)(82* m_ratio), (int)(71* m_ratio), gl, this.context, m_height));
    	clip.getSpriteFactory().add("ch4", new Sprite("ui/ch4.png", (int)(82* m_ratio), (int)(71* m_ratio), gl, this.context, m_height));
    	clip.getSpriteFactory().add("ch5", new Sprite("ui/ch5.png", (int)(82* m_ratio), (int)(71* m_ratio), gl, this.context, m_height));
    	clip.getSpriteFactory().add("ch1", new Sprite("ui/ch1.png", (int)(82* m_ratio), (int)(71* m_ratio), gl, this.context, m_height));
    	//clip.setPosition(100,  100);
    	clip.setRepetition(1);
    	findLayerById(101).movieClipFactory.add("SHOOT", clip);

    	setMoney(gameInfo.money, false);
    	setGold(gameInfo.gold, false);

		findLayerById(131).textFactory.setVisible("GOLD", false);
		findLayerById(131).textFactory.setVisible("MONEY", false);

		textureCountToLoad = texturesToLoad.size();
    }
	private void addTextureToLoad(int layer, String id, String fileName, int width,
			int height, int posx, int posy, boolean visible) {
		texturesToLoad.add(new TextureToLoad(layer, id, fileName, width, height, posx, posy, visible));		
	}
	private long getClockTick()
	{
		return System.currentTimeMillis();
	}
}
