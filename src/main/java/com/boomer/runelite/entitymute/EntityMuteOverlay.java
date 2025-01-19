package com.boomer.runelite.entitymute;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

@Singleton
@Slf4j
public class EntityMuteOverlay extends Overlay {

    public static final int IMAGE_Z_OFFSET = 80;
    private static final int MAX_DISTANCE = 2400;
    private final Client client;
    private final EntityMutePlugin plugin;
    private final BufferedImage volumeOffImage = resizeImage(ImageUtil.loadImageResource(EntityMutePlugin.class, "/assets/volume_off_16dp.png"), 16, 16);
    private final BufferedImage volumeOnImage = resizeImage(ImageUtil.loadImageResource(EntityMutePlugin.class, "/assets/volume_up_16dp.png"), 16, 16);
    private final Set<Integer> gameObjectsToOverlay = new HashSet<>();

    @Inject
    private EntityMuteOverlay(Client client, EntityMutePlugin plugin) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.plugin = plugin;
    }

    public void addGameObjectToOverlay(int gameObjectId) {
        gameObjectsToOverlay.add(gameObjectId);
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        renderNpcs(graphics);
        renderTileObjects(graphics);

        return null;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        Image scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }

    private void renderNpcs(Graphics2D graphics) {
        Player player = client.getLocalPlayer();
        IndexedObjectSet<? extends NPC> npcs = player.getWorldView().npcs();

        for (NPC npc : npcs) {
            BufferedImage image = plugin.npcIsMuted(npc.getId()) ? volumeOffImage : volumeOnImage;
            OverlayUtil.renderActorOverlayImage(graphics, npc, image, Color.LIGHT_GRAY, IMAGE_Z_OFFSET);
        }
    }

    private void renderTileObjects(Graphics2D graphics) {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();

        int z = client.getPlane();

        for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
                Tile tile = tiles[z][x][y];

                if (tile == null) {
                    continue;
                }
                Player player = client.getLocalPlayer();
                if (player == null) {
                    continue;
                }

                GameObject[] gameObjects = tile.getGameObjects();
                if (gameObjects != null) {
                    for (GameObject gameObject : gameObjects) {
                        if (gameObject != null &&
                                gameObjectsToOverlay.contains(gameObject.getId()) &&
                                gameObject.getSceneMinLocation().equals(tile.getSceneLocation()) &&
                                player.getLocalLocation().distanceTo(gameObject.getLocalLocation()) <= MAX_DISTANCE) {
                            BufferedImage image = plugin.objectIsMuted(gameObject.getId()) ? volumeOffImage : volumeOnImage;
                            OverlayUtil.renderImageLocation(client, graphics, gameObject.getLocalLocation(), image, IMAGE_Z_OFFSET);
                        }
                    }
                }
            }
        }
    }
}
