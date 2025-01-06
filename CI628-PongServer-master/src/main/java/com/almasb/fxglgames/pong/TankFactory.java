package com.almasb.fxglgames.pong;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.BoxShapeData;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.ArrayList;


import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * Tank Game Factory for spawning tank and bullet entities.
 */

public class TankFactory implements EntityFactory {

    public int playerNumber;


    @Spawns("tank")
    public Entity newTank(SpawnData data) {
        // Retrieve the player number or some distinguishing factor from the SpawnData
        playerNumber = data.get("playerNumber");

        // Load the tank sprite based on player number
        Image tankImage = loadTankImage(playerNumber);
        ImageView tankView = new ImageView(tankImage);

        // Set sprite size
        tankView.setFitWidth(32);
        tankView.setFitHeight(32);

        // Set the initial rotation of the tank
        tankView.setRotate(90);  // Or set this to whatever default rotation you need

        // Create the bounding box for the tank based on the sprite size
        double spriteWidth = tankView.getFitWidth();
        double spriteHeight = tankView.getFitHeight();
        HitBox box = new HitBox(BoundingShape.box(spriteWidth, spriteHeight));

        // Create the Physics component for the tank
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);

        // Create the tank entity with the centered sprite, physics, and center marker
        Entity tank = entityBuilder(data)
                .type(EntityType.TANK)
                .view(tankView)  // Add the ImageView as the view
                .with(physics)
                .with(new CollidableComponent(true))  // Make it collidable
                .with(new TankComponent())  // Custom component to control tank behavior
                .bbox(box)
                .build();
        return tank;
    }


    private Image loadTankImage(int playerNumber) {
        String tankImagePath = "sprites/Tank" + playerNumber + ".png";
        return getAssetLoader().loadImage(tankImagePath);  // Dynamically load based on player number
    }

    @Spawns("bullet")
    public Entity newBullet(SpawnData data) {
        // Load the bullet sprite (image) from assets
        Image bulletImage = getAssetLoader().loadImage("sprites/bullet.png");  // Replace with the actual image path
        ImageView bulletView = new ImageView(bulletImage);

        // Create Physics component for the bullet
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        bulletView.setRotate(90);

        // Create the bullet entity
        Entity bullet = entityBuilder(data)
                .type(EntityType.BULLETS)
                .view(bulletView)
                .bbox(new HitBox(BoxShapeData.circle(7)))
                .with(physics)
                .with(new CollidableComponent(true))  // Make the bullet collidable
                .with(new BulletsComponent())  // Custom component to handle bullet behavior
                .build();
        TankApp.BULLETLIST.add(bullet);
        return bullet;
    }


    @Spawns("wall")
    public Entity newWall(SpawnData data) {

        Image wallImage = getAssetLoader().loadImage("sprites/wall.png");
        ImageView wallView = new ImageView(wallImage);

        wallView.setFitWidth(32);
        wallView.setFitHeight(32);

        HitBox box = new HitBox(BoundingShape.box(32, 32));

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);
        physics.setFixtureDef(new FixtureDef().density(1f).restitution(1.0f));

        Entity wall = entityBuilder(data)
                .type(EntityType.WALL)
                .view(wallView)
                .with(physics)
                .bbox(box)
                .with(new CollidableComponent(true))
                .build();

        System.out.println("Wall entity created: " + wall);
        TankApp.WALLLIST.add(wall);
        return wall;
    }
}
