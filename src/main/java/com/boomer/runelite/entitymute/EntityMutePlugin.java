package com.boomer.runelite.entitymute;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
	name = "Entity-Mute"
)
public class EntityMutePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private EntityMuteConfig config;
	@Inject
	private MouseManager mouseManager;
	@Inject
	private ItemManager itemManager;
	private EntityMuteMouseListener mouseListener;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Example started!");
		mouseListener = new EntityMuteMouseListener(this);
		mouseManager.registerMouseListener(mouseListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Example stopped!");
		mouseManager.unregisterMouseListener(mouseListener);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event) {

		var sortedEntries = existingEntriesByTarget(event.getMenuEntries());

		sortedEntries.entrySet().forEach(entry -> {
			MenuEntry templateEntry = entry.getValue().get(0);
			client.getMenu()
					.createMenuEntry(-1)
					.setOption("Toggle Mute")
					.setTarget(entry.getKey())
					.setType(MenuAction.RUNELITE)
					.onClick(it -> {
						determineEntity(it);
					});
		});
		log.info("Menu opened");
	}

	private Map<String, List<MenuEntry>> existingEntriesByTarget(MenuEntry[] entries){
		return Arrays.stream(entries)
				.filter(menuEntry -> menuEntry.getTarget() != null && !menuEntry.getTarget().isEmpty())
				.collect(Collectors.groupingBy(MenuEntry::getTarget));
	}

	private void determineEntity(MenuEntry menuEntry) {
		int itemId = menuEntry.getItemId();
		ItemComposition itemComposition = itemManager.getItemComposition(itemId);
		String name = itemComposition.getName();
		log.info("Item name: {}", name);

		Optional<NPC> npc = Optional.ofNullable(menuEntry.getNpc());
		if(npc.isPresent()){
			log.info("We have an NPC");
		}

		Optional<Actor> actor = Optional.ofNullable(menuEntry.getActor());
		if(actor.isPresent()){
			log.info("We have an Actor");
		}

		Optional<Player> player = Optional.ofNullable(menuEntry.getPlayer());
		if(player.isPresent()){
			log.info("We have an Actor");
		}


	}

	@Provides
	EntityMuteConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(EntityMuteConfig.class);
	}
}
