package com.almasb.fxglgames.pong;

import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;

import static com.almasb.fxgl.dsl.FXGL.*;
import static java.lang.Math.abs;
import static java.lang.Math.signum;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class BulletsComponent extends Component {
    private PhysicsComponent physics;

    private static final double BULLET_SPEED = 300; // Bullet speed constant
    public int bounces = 0;

    @Override
    public void onUpdate(double tpf) {

        checkOffscreen();

    }


    private void checkOffscreen() {
        // If the bullet is outside the viewport, remove the entity
        if (getEntity().getBoundingBoxComponent().isOutside(getGameScene().getViewport().getVisibleArea())) {
            getEntity().removeFromWorld();
        }
    }
}
