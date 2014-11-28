package com.shutterbug.gameboy;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.shutterbug.gameboy.Z80.*;

public class MyGdxGame implements ApplicationListener
{
	Texture texture;
	Z80 z80 = new Z80();
	SpriteBatch batch;

	@Override
	public void create()
	{
		texture = new Texture(Gdx.files.internal("android.jpg"));
		batch = new SpriteBatch();
		z80.reset();
		z80.loadCartridge("/storage/emulated/0/Download/Tetris (World)/Tetris (World).gb");
	}

	@Override
	public void render()
	{        
	    Gdx.gl.glClearColor(1, 1, 1, 1);
	    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		batch.draw(texture, Gdx.graphics.getWidth() / 4, 0, 
				   Gdx.graphics.getWidth() / 2, Gdx.graphics.getWidth() / 2);
		batch.end();
		z80.opcode();
		long startTime = System.nanoTime();
		int cyclesUsed = z80.lastCycles;
		long usedTime = System.nanoTime() - startTime;
		try {
			double nanosSleep = (cyclesUsed * 23.84D) - usedTime;
			if(nanosSleep > 0D) {
				Thread.sleep(0, (int)nanosSleep);
			}
		} catch (InterruptedException e) {
			//Unthrown exception
		}
	}

	@Override
	public void dispose()
	{
	}

	@Override
	public void resize(int width, int height)
	{
	}

	@Override
	public void pause()
	{
	}

	@Override
	public void resume()
	{
	}
}
