package com.tolka;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;

public class Sprite {
	private float m_alpha;
	private float m_darkness;
	private int m_x = 0;
	private int m_y = 0;
	private int m_width;
	private int m_height;
	private int m_globalHeight;
	private boolean m_flip;
	private float m_scale_x;
	private float m_scale_y;
	private float m_scale_z;

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
	
	private int[] textures = new int[1];
	private String drawable;
	private GL10 gl;
	private Context context;
	
	public void setBrightness(float brightness)
	{
		m_darkness = 1.0f - brightness;
	}
	public float getBrightness()
	{
		return 1.0f- m_darkness;
	}
	public int getY()
	{
		return m_y;
	}
	public int getX()
	{
		return m_x;
	}
	public float getScaleX()
	{
		return m_scale_x;
	}
	public float getScaleY()
	{
		return m_scale_y;
	}
	public float getScaleZ()
	{
		return m_scale_z;
	}
	public int getWidth()
	{
		return m_width;
	}
	public int getHeight()
	{
		return m_height;
	}
	public String getDrawable()
	{
		return this.drawable;
	}
	public void setDrawable(String drawable)
	{
		this.drawable = drawable;
	}
	public Context getContext()
	{
		return context;
	}
	public int[] getTextures()
	{
		return textures;
	}
	public GL10 getGl()
	{
		return gl;
	}
	public void setFlip(boolean flip)
	{
		this.m_flip = flip;
	}
	public boolean getFlip()
	{
		return m_flip;
	}
	
	public Sprite(String drawable, int width, int height, GL10 gl, Context context, int m_globalHeight)
	{
		this.drawable = drawable;
		this.gl = gl;
		this.context = context;
		this.m_globalHeight = m_globalHeight;
		this.m_flip = false;
		
		m_width = width;
		m_height = height;
		m_alpha = 1.0f;
		m_darkness = 0.0f;
		
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
		
		m_scale_x = 1.0f;
		m_scale_y = 1.0f;
		m_scale_z = 1.0f;
		
		try {
			loadGLTexture();
		} catch (IOException e) {
			e.printStackTrace();
		}

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		gl.glClearDepthf(1.0f);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		
		gl.glEnable(GL10.GL_ALPHA_TEST);
		gl.glAlphaFunc(GL10.GL_GREATER, 0);
	    gl.glEnable(GL10.GL_BLEND);
	    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST); 
	}
	
	private Bitmap bitmap;
	
	public void reloadFromBitmap(Bitmap bitmap)
	{
		gl.glGenTextures(1, textures, 0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
		//gl.glTexEnvf (GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
		
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, (int)GL10.GL_RGBA, bitmap, 0);
	}
	
	public Bitmap getBitmap() throws IOException
	{
        BitmapFactory.Options options = new BitmapFactory.Options ();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawable);
        InputStream is = context.getAssets().open("textures/"+ drawable);        
        bitmap = BitmapFactory.decodeStream(is);

        return bitmap;
	}
	
	private void loadGLTexture() throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options ();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        //Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), drawable);
        InputStream is = context.getAssets().open("textures/"+ drawable);        
        bitmap = BitmapFactory.decodeStream(is);

		gl.glGenTextures(1, textures, 0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
		
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);
		
		//gl.glTexEnvf (GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE);
		
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, (int)GL10.GL_RGBA, bitmap, 0);
	}
	
	public void draw()
	{
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);

        gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glTranslatef ((m_flip?m_width:0) + m_x, -m_y+ (m_globalHeight - m_height), 0f);
        gl.glScalef (m_width, m_height, 0f);
    	
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		
        gl.glColor4f(1.0f- m_darkness, 1.0f- m_darkness, 1.0f- m_darkness, m_alpha);
        gl.glScalef(m_flip?-m_scale_x:m_scale_x, m_scale_y, m_scale_z);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
		
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, vertices.length / 3);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		//
		gl.glPopMatrix();
	}
	
	public void setPosition(int x, int y)
	{
		m_x = x;
		m_y = y;
	}
	
	public void setScale(float scale)
	{
		m_scale_x = scale;
		m_scale_y = scale;
		m_scale_z = scale;
	}
	
	public void setAlpha(float alpha)
	{
		m_alpha = alpha;
	}
	public void setWidth(int width)
	{
		m_width = width;
	}
	public void setHeight(int height)
	{
		m_width = height;
	}
	public void flip() {
		m_flip = m_flip?false:true;
	}
}
