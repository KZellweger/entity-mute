package com.boomer.runelite.entitymute;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(EntityMuteConfig.GROUP)
public interface EntityMuteConfig extends Config
{
	String GROUP = "entityMute";

	@ConfigItem(
		keyName = "muteIndicator",
		name = "Show Mute Indicator",
		description = "Render Icon to display MuteState of Objects and NPCs"
	)
	default boolean greeting()
	{
		return true;
	}
}
