package com.catmanjan.bsp.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.catmanjan.bsp.MyGdxGame;
import com.github.czyzby.websocket.CommonWebSockets;

public class DesktopLauncher {
	public static void main (String[] arg) {
		CommonWebSockets.initiate();

		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 1200;
		config.height = 720;
		new LwjglApplication(new MyGdxGame(), config);
	}
}
