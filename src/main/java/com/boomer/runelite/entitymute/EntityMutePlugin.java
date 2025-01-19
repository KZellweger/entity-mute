package com.boomer.runelite.entitymute;

import com.google.common.base.MoreObjects;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.AreaSoundEffectPlayed;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Entity-Mute"
)
public class EntityMutePlugin extends Plugin {
    private final Map<NPC, HighlightedNpc> highlightedNpcs = new HashMap<>();
    private final Function<NPC, HighlightedNpc> isHighlighted = highlightedNpcs::get;
    @Inject
    private Client client;
    @Inject
    private EntityMuteConfig config;
    @Inject
    private ItemManager itemManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private NpcOverlayService npcOverlayService;
    @Inject
    private NpcUtil npcUtil;

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
        }
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed soundEffectPlayed) {
        final Actor actor = soundEffectPlayed.getSource();

        Player localPlayer = client.getLocalPlayer();


        if (actor == null) {
            log.warn("Sound effect source not an Actor: {}", soundEffectPlayed.getSoundId());
            log.warn("Sound effect : {}", MoreObjects.toStringHelper(soundEffectPlayed).add("id", soundEffectPlayed.getSoundId()));
            return;
        }
        if (actor instanceof NPC) {
            final NPC npc = (NPC) actor;
            highlightedNpcs.put(npc, highlightedNpc(npc));
            log.debug("Highlighted npc: {}", MoreObjects.toStringHelper(npc).add("id", npc.getId()));
        }
        if (actor instanceof Player) {
            final Player player = (Player) actor;
            log.debug("Highlighted player: {}", MoreObjects.toStringHelper(player).add("id", player.getName()));
        }

        npcOverlayService.rebuild();
    }

    @Subscribe
    public void onAreaSoundEffectPlayed(AreaSoundEffectPlayed soundEffectPlayed) {

        final Actor actor = soundEffectPlayed.getSource();
        String actorString = "None";

        Player localPlayer = client.getLocalPlayer();
        WorldView playersWorldView = localPlayer.getWorldView();
        int areaSoundEffectOriginSceneX = soundEffectPlayed.getSceneX();
        int areaSoundEffectOriginSceneY = soundEffectPlayed.getSceneY();

        Scene scene = playersWorldView.getScene();
        int z = playersWorldView.getPlane();

        Tile[][][] tiles = scene.getTiles();

        ArrayList<Tile> tilesToCheck = new ArrayList<>();
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX][areaSoundEffectOriginSceneY]);
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX - 1][areaSoundEffectOriginSceneY]);
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX + 1][areaSoundEffectOriginSceneY]);
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX][areaSoundEffectOriginSceneY - 1]);
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX][areaSoundEffectOriginSceneY + 1]);
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX +1 ][areaSoundEffectOriginSceneY + 1]);
        tilesToCheck.add(tiles[z][areaSoundEffectOriginSceneX - 1 ][areaSoundEffectOriginSceneY - 1]);

        List<GameObject> possibleOriginObjects = tilesToCheck.stream().flatMap(tile -> Arrays.stream(tile.getGameObjects()).filter(Objects::nonNull)).collect(Collectors.toList());

        for(GameObject gameObject : possibleOriginObjects) {
            log.warn("Found possible Sound origin game object: {}", gameObject.getId());
        }

        if (actor != null) {
            String type = "";
            if (actor instanceof Player) {
                type = "Player";
            } else {
                type = "NPC";
            }
            actorString = "Actor: " + MoreObjects.toStringHelper(actor)
                    .add("type: ", type)
                    .add("name", actor.getName())
                    .add("actor sceneX", actor.getLocalLocation().getSceneX())
                    .add("actor sceneY", actor.getLocalLocation().getSceneY());
        }

        log.debug("Area Sound effect played: {} actor: {}", MoreObjects
                        .toStringHelper(soundEffectPlayed)
                        .add("id", soundEffectPlayed.getSoundId())
                        .add("sceneX", soundEffectPlayed.getSceneX())
                        .add("sceneY", soundEffectPlayed.getSceneY())
                        .add("range", soundEffectPlayed.getRange())
                        .add("delay", soundEffectPlayed.getDelay()),
                actorString
        );

        if (actor instanceof NPC) {
            final NPC npc = (NPC) actor;
            highlightedNpcs.put(npc, highlightedNpc(npc));
            log.debug("Highlighted npc: {}", MoreObjects.toStringHelper(npc).add("id", npc.getId()));
        }
        npcOverlayService.rebuild();
    }

    @Override
    protected void startUp() throws Exception {
        log.info("Example started!");
        npcOverlayService.registerHighlighter(isHighlighted);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Example stopped!");
        npcOverlayService.unregisterHighlighter(isHighlighted);
    }

    @Provides
    EntityMuteConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(EntityMuteConfig.class);
    }

    private HighlightedNpc highlightedNpc(NPC npc) {
        Color color = Color.PINK;
        final int npcId = npc.getId();
        return HighlightedNpc.builder()
                .npc(npc)
                .highlightColor(color)
                .name(true)
                .render(it -> true)
                .build();
    }

}
