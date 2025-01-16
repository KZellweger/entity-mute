package com.boomer.runelite.entitymute;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.input.MouseAdapter;

import javax.swing.*;
import java.awt.event.MouseEvent;

@Slf4j
public class EntityMuteMouseListener extends MouseAdapter {

    private final EntityMutePlugin plugin;

    public EntityMuteMouseListener(EntityMutePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent mouseEvent) {

        log.info("Mouse clicked");

        if(SwingUtilities.isRightMouseButton(mouseEvent)){
            log.debug( "Right click detected");
        }
        mouseEvent.consume();
        return mouseEvent;
    }
}
