/*
 * The MIT License (MIT)
 *
 * FXGL - JavaFX Game Library
 *
 * Copyright (c) 2015-2017 AlmasB (almaslvl@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.almasb.fxglgames.pong;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;

public class TankComponent extends Component {

    private static final double TANK_SPEED = 300;
    private static final double Tank_Rotation_Speed = 8000;
    private int playerNumber;

    protected PhysicsComponent physics;

    public void Forward() {

        Point2D forwardVector = getForwardVector();
        physics.setLinearVelocity(forwardVector.multiply(TANK_SPEED));
    }

    public void Backward() {

        Point2D backwardVector = getForwardVector().multiply(-1);
        physics.setLinearVelocity(backwardVector.multiply(TANK_SPEED));
    }

    public void Right() {
        // Apply a rotational velocity to the entity's physics component
        physics.setAngularVelocity(Tank_Rotation_Speed / 60);
    }

    public void Left() {
        // Apply negative rotational velocity to rotate in the opposite direction
        physics.setAngularVelocity(-Tank_Rotation_Speed / 60);
    }

    public void Fire() {
        // Get the tank's rotation in degrees
        double rotationDegrees = entity.getRotation();

        // Convert to radians
        double rotationRadians = Math.toRadians(rotationDegrees);

        // Handle special cases for up (90°) and down (270°) angles
        double cosRotation = Math.cos(rotationRadians);
        double sinRotation = Math.sin(rotationRadians);

        // Check if the rotation is nearly up or down to avoid small floating point errors
        if (Math.abs(cosRotation) < 0.0001) {
            // If facing exactly up or down, force the direction vector to be vertical
            cosRotation = 0;  // No horizontal component
            if (sinRotation > 0) {
                sinRotation = 1;  // Upward direction
            } else {
                sinRotation = -1;  // Downward direction
            }
        }

        // Calculate the direction vector based on the rotation (forward direction)
        Point2D direction = new Point2D(cosRotation, sinRotation);

        // Define the offset for the bullet spawn position, ensuring it's in front of the tank
        double offsetX = direction.getX() * 40;  // Adjust this value to control how far in front the bullet spawns
        double offsetY = direction.getY() * 40;

        // Spawn the bullet entity, using the tank's position plus the offset
        Entity bullet = FXGL.spawn("bullet", entity.getPosition().add(offsetX, offsetY));

        // Set the bullet's velocity in the direction the tank is facing
        PhysicsComponent bulletPhysics = bullet.getComponent(PhysicsComponent.class);
        bulletPhysics.setLinearVelocity(direction.multiply(300)); // BULLET_SPEED is a constant for bullet speed

        // Optionally: Adjust bullet rotation to match the tank's rotation
        bullet.setRotation(rotationDegrees);  // Match bullet's rotation to the tank's facing direction
    }

    public void stop() {
        physics.setLinearVelocity(0, 0);
        physics.setAngularVelocity(0);
    }

    private Point2D getForwardVector() {
        double angle = Math.toRadians(entity.getRotation()); // Convert rotation to radians
        return new Point2D(Math.cos(angle), Math.sin(angle));
    }
    public int GetPlayerNumber(){
        return playerNumber;
    }
    public void SetPlayerNumber(int number){
        playerNumber = number;
    }


}
