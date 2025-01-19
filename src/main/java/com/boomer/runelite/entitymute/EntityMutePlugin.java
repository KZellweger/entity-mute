package com.boomer.runelite.entitymute;

import com.google.common.base.MoreObjects;
import com.google.inject.Provides;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
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
import java.lang.reflect.Field;
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
    private final Map<Integer, MutedGameObject> mutedGameObjects = new HashMap<>();
    private final Map<Integer, MutedNpc> mutedNpcs = new HashMap<>();

    // Option added to menu
    private static final String MUTE = "Mute";
    private static final String UN_MUTE = "Un-mute";

    @Inject
    private Client client;
    @Inject
    private EntityMuteConfig config;
    @Inject
    private ItemManager itemManager;
    @Inject
    private EntityMuteOverlay entityMuteOverlay;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private NpcOverlayService npcOverlayService;
    @Inject
    private NpcUtil npcUtil;



    Queue<String> playerActionQueue = new ArrayDeque<>();

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        int targetIdentifier  = menuOptionClicked.getMenuEntry().getIdentifier();
        menuOptionClicked.getMenuTarget();
        log.debug("Menu option clicked: {}, id {}", menuOptionClicked.getMenuTarget(), targetIdentifier);
    }

    @Subscribe
    public void onSoundEffectPlayed(SoundEffectPlayed soundEffectPlayed) {
        final Actor actor = soundEffectPlayed.getSource();

        Player localPlayer = client.getLocalPlayer();
        var animationID = localPlayer.getAnimation();
        Actor interacting = localPlayer.getInteracting();

        String animationKey = getVarNameFromValue(AnimationID.class, animationID);

        if(animationKey != null) {
            log.debug("Animation played: {}", animationKey);
        }

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

        if(soundEffectPlayed.isConsumed()){
            return;
        }
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

        if (actor instanceof NPC) {
            final NPC npc = (NPC) actor;
            highlightedNpcs.put(npc, highlightedNpc(npc));
            log.debug("Highlighted npc: {}", MoreObjects.toStringHelper(npc).add("id", npc.getId()));
        }
        npcOverlayService.rebuild();
    }


    public void toggleMuteNpc(NPC npc) {
        MutedNpc mutedNpc = new MutedNpc(npc.getId(), npc.getName());
        if(isMuted(npc)){
            mutedNpcs.remove(npc.getId());
        } else {
            mutedNpcs.put(npc.getId(), mutedNpc);
        }
    }

    public void toggleMuteGameObject(GameObject gameObject) {
        MutedGameObject mutedGameObject = new MutedGameObject(gameObject.getId());
        if(isMuted(gameObject)){
            mutedGameObjects.remove(gameObject.getId());
        } else {
            mutedGameObjects.put(gameObject.getId(), mutedGameObject);
        }
    }

    public boolean isMuted(NPC npc) {
        return mutedNpcs.containsKey(npc.getId());
    }
    public boolean isMuted(GameObject gameObject) {
        return mutedGameObjects.containsKey(gameObject.getId());
    }

    @Override
    protected void startUp() throws Exception {
        log.info("Example started!");
        overlayManager.add(entityMuteOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Example stopped!");
        overlayManager.remove(entityMuteOverlay);
    }

    @Provides
    EntityMuteConfig getConfig(ConfigManager configManager) {
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

    private static String getVarNameFromValue(Class<?> clazz, int value) {
        // Iterate over all declared fields in the class
        for (Field field : clazz.getDeclaredFields()) {
            // Ensure the field is static and of the correct type
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                try {
                    // Check if the value of the field matches the given value
                    if (field.getInt(null) == value) {
                        return field.getName(); // Return the field name
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null; // Return null if no matching value is found
    }

    @Data
    private static class MutedGameObject {
        private final int id;
    }

    @Data
    private static class MutedNpc {
        private final int id;
        private final String name;
    }

}
