package com.tolka;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.opengl.GLUtils;

public class Text {
	protected String message;
	protected int posx;
	protected int posy;
	protected final String alphabet = "0123456789";
	protected Vector<Sprite> alphabetSprites;
	protected int[] charWidths = {22, 15, 21, 20, 21, 21, 21, 18, 20, 22};
	protected float m_scale;
	protected Bitmap bitmapFinal; 
	
	private FloatBuffer vertexBuffer;	// buffer holding the vertices
	private FloatBuffer textureBuffer;	// buffer holding the texture coordinates
	private FloatBuffer colorBuffer;	// buffer holding the colors
	
	private float vertices[] = {
			1.0f, 1.0f,  0.0f,
			0.0f, 1.0f,  0.0f,
			1.0f, 0.0f,  0.0f,
			0.0f, 0.0f,  0.0f
		};
		
	private float colors[] = {
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 0.7f,
			1.0f, 1.0f, 1.0f, 0.4f,
			1.0f, 1.0f, 1.0f, 0.1f
		};
		
	private float texture[] = {    		
		1.0f, 0.0f,
		0.0f, 0.0f,
		1.0f, 1.0f,
		0.0f, 1.0f
	};
	
	protected int[] textures = new int[1];
	protected GL10 gl;
	protected Context context;
	protected int textWidth(String text)
	{
		int result = 0;
		
		for(int i = 0; i< text.length(); i++)
		{
			int pos = alphabet.indexOf(text.substring(i, i+ 1));
			if(pos == -1)
			{
				result += charWidths[0]* m_scale;
				continue;
			}

			result += charWidths[pos]* m_scale;
		}
		
		return result;
	}
    protected int totalWidth = 0;
    protected int globalHeight;
    protected float m_ratio;
    protected String errorMessage = "No error";

    public void setPosition(int posx, int posy)
    {
    	this.posx = posx;
    	this.posy = posy;
    }
	public void setPosX(int posx)
	{
		this.posx = posx;
	}
	public void setPosY(int posy)
	{
		this.posy = posy;
	}
	public int getPosX()
	{
		return posx;
	}
	public int getPosY()
	{
		return posy;
	}
	public float getScale()
	{
		return m_scale;
	}
	public void setScale(float scale)
	{
		this.m_scale = scale;
	}
	
	public Text(String message, GL10 gl, Context context, float ratio, int globalHeight)
	{
		m_scale = 1;

		this.gl = gl;
		this.context = context;
		this.message = message;
		this.globalHeight = globalHeight;
		this.m_ratio = ratio;

		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		vertexBuffer = byteBuffer.asFloatBuffer();
		vertexBuffer.put(vertices);
		vertexBuffer.position(0);

		byteBuffer = ByteBuffer.allocateDirect(colors.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		colorBuffer = byteBuffer.asFloatBuffer();
		colorBuffer.put(colors);
		colorBuffer.position(0);

		byteBuffer = ByteBuffer.allocateDirect(texture.length * 4);
		byteBuffer.order(ByteOrder.nativeOrder());
		textureBuffer = byteBuffer.asFloatBuffer();
		textureBuffer.put(texture);
		textureBuffer.position(0);
		errorMessage = "No error";

		alphabetSprites = new Vector<Sprite>();

		try {
			loadGLTexture();
		} catch (IOException e) {
			errorMessage = e.getMessage();
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
	
	public void draw()
	{
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_BLEND);
        gl.glTranslatef (posx, -posy+ (globalHeight - 26* m_ratio), 0f);
        gl.glScalef (totalWidth* m_ratio, 26* m_ratio, 0f);
    	
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
		gl.glFrontFace(GL10.GL_CW);
		
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glScalef(m_scale, m_scale, m_scale);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
		
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		//
		gl.glPopMatrix();
	}

	public String getError()
	{
		return errorMessage;
	}
	
	public void setMessage(String message)
	{
		this.message = message;
		errorMessage = "No error";

		try {
			loadGLTexture();
		} catch (IOException e) {
			errorMessage = e.getMessage();
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
	
	public int getTotalWidth()
	{
		return totalWidth;
	}
	
	private void loadGLTexture() throws IOException {
		if(alphabetSprites.size() == 0)
		{
			for(int i = 0; i< 10; i++)
			{
				alphabetSprites.add(new Sprite("ui/ui_char_"+ alphabet.substring(i, i+ 1)+ ".png", 18, 26, gl, context, globalHeight));
			}
		}

		for(int i = 0; i< 10; i++)
		{
			alphabetSprites.get(i).setScale(m_scale);
		}

		BitmapFactory.Options options = new BitmapFactory.Options ();
        options.inScaled = false;
        options.inPreferredConfig = alphabetSprites.get(0).getBitmap().getConfig();

        totalWidth = 0;
        for(int i = 0; i< message.length(); i++)
        {
			int pos = alphabet.indexOf(message.substring(i, i+ 1));
			if(pos == -1)
			{
				totalWidth += charWidths[0];
				continue;
			}

			totalWidth += charWidths[pos];
        }
        
        bitmapFinal = Bitmap.createBitmap(totalWidth, 26, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapFinal);

        int currWidth = 0;
        for(int i = 0; i< message.length(); i++)
        {
			int pos = alphabet.indexOf(message.substring(i, i+ 1));
			if(pos == -1)
			{
				currWidth += charWidths[0];
				continue;
			}

        	Bitmap bitmap = alphabetSprites.get(pos).getBitmap();
        	canvas.drawBitmap(bitmap, currWidth, 0, null);
        	
	    	currWidth += charWidths[pos];
        }

		gl.glGenTextures(1, textures, 0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		//gl.glTexEnvf (GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmapFinal, 0);

		//GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, alphabetSprites.get(0).getBitmap(), 0);
	}
}
