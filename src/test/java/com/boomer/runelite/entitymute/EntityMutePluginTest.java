package com.boomer.runelite.entitymute;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class EntityMutePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(EntityMutePlugin.class);
		RuneLite.main(args);
	}
}