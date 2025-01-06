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


import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;

import com.almasb.fxgl.entity.Entity;

import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.*;
import com.almasb.fxgl.physics.CollisionHandler;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.ui.UI;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.almasb.fxgl.dsl.FXGL.*;

public class TankApp extends GameApplication implements MessageHandler<String> {



    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Tank");
        settings.setVersion("1.0");
        settings.setFontUI("Tank.ttf");
        settings.setApplicationMode(ApplicationMode.DEBUG);
    }

    private Entity player1;
    private Entity player2;
    private TankComponent TankPlayer1;
    private TankComponent TankPlayer2;
    private MapHandler mapHandler;

    private static Server<String> server;

    private int player1Score = 0;
    private int player2Score = 0;

    public static ArrayList<Entity> BULLETLIST = new ArrayList<Entity>();
    public static ArrayList<Entity> WALLLIST = new ArrayList<Entity>();

    @Override
    protected void initInput() {
        // Forward movement for Player 1
        getInput().addAction(new UserAction("Forward1") {
            @Override
            protected void onAction() { TankPlayer1.Forward();}

            @Override
            protected void onActionEnd() {TankPlayer1.stop();}
        }, KeyCode.W);

        // Backward movement for Player 1
        getInput().addAction(new UserAction("Backward1") {
            @Override
            protected void onAction() { TankPlayer1.Backward();}

            @Override
            protected void onActionEnd() {TankPlayer1.stop(); }
        }, KeyCode.S);

        // Rotate left for Player 1
        getInput().addAction(new UserAction("RotateLeft1") {
            @Override
            protected void onAction() { TankPlayer1.Left();}

            @Override
            protected void onActionEnd() {TankPlayer1.stop();}
        }, KeyCode.A);

        // Rotate right for Player 1
        getInput().addAction(new UserAction("RotateRight1") {
            @Override
            protected void onAction() {TankPlayer1.Right();}

            @Override
            protected void onActionEnd() {TankPlayer1.stop();}
        }, KeyCode.D);

        // Firing action for Player 1
        getInput().addAction(new UserAction("Fire1") {
            @Override
            protected void onActionBegin() {TankPlayer1.Fire();}
        }, KeyCode.SPACE);

        //========================== Player 2 =================================

        // Forward movement for Player 2
        getInput().addAction(new UserAction("Forward2") {
            @Override
            protected void onAction() {TankPlayer2.Forward();}

            @Override
            protected void onActionEnd() {TankPlayer2.stop();}
        }, KeyCode.I);

        // Backward movement for Player 2
        getInput().addAction(new UserAction("Backward2") {
            @Override
            protected void onAction() {TankPlayer2.Backward();}

            @Override
            protected void onActionEnd() {TankPlayer2.stop();}
        }, KeyCode.K);

        // Rotate left for Player 2
        getInput().addAction(new UserAction("RotateLeft2") {
            @Override
            protected void onAction() {TankPlayer2.Left();}

            @Override
            protected void onActionEnd() {TankPlayer2.stop();}
        }, KeyCode.J);

        // Rotate right for Player 2
        getInput().addAction(new UserAction("RotateRight2") {
            @Override
            protected void onAction() {TankPlayer2.Right();}

            @Override
            protected void onActionEnd() {TankPlayer2.stop();}
        }, KeyCode.L);

        // Firing action for Player 2
        getInput().addAction(new UserAction("Fire2") {
            @Override
            protected void onActionBegin() {TankPlayer2.Fire();}
        }, KeyCode.ENTER);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("player1score", 0);
        vars.put("player2score", 0);
    }

    @Override
    protected void initGame() {
        Writers.INSTANCE.addTCPWriter(String.class, outputStream -> new MessageWriterS(outputStream));
        Readers.INSTANCE.addTCPReader(String.class, in -> new MessageReaderS(in));

        server = getNetService().newTCPServer(55555, new ServerConfig<>(String.class));

        server.setOnConnected(connection -> {
            connection.addMessageHandlerFX(this);
        });



        getGameWorld().addEntityFactory(new TankFactory());
        getGameScene().setBackgroundColor(Color.rgb(0, 0, 5));

        mapHandler = new MapHandler();
        renderMap();

        initScreenBounds();
        initGameObjects();

        var t = new Thread(server.startTask()::run);
        t.setDaemon(true);
        t.start();
    }

    protected void initPhysics() {
        // Set gravity to zero (no gravity in the game world)
        getPhysicsWorld().setGravity(0, 0);
        System.out.println("Physics Successfully Loaded");
        // Bullet vs. Wall
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BULLETS, EntityType.WALL) {
            @Override
            protected void onHitBoxTrigger(Entity bullet, Entity wall, HitBox bulletBox, HitBox wallBox) {
                // Get the bullet's physics component
                var bulletPhysics = bullet.getComponent(PhysicsComponent.class);
                Point2D velocity = bulletPhysics.getLinearVelocity();

                // Calculate the magnitude of the bullet's velocity
                double speed = velocity.distance(0, 0);  // Magnitude of the velocity vector

                if (Math.abs(velocity.getX()) > Math.abs(velocity.getY())) {
                    velocity = new Point2D(velocity.getX(), -velocity.getY()); // Reflect on X-axis
                } else {
                    velocity = new Point2D(-velocity.getX(), velocity.getY()); // Reflect on Y-axis
                }

                bulletPhysics.setLinearVelocity(velocity);
                // Access the BulletComponent to track bounces
                BulletsComponent bulletComponent = bullet.getComponent(BulletsComponent.class);

                // Increment the bounce counter
                bulletComponent.bounces++;

                // If the bullet has bounced 5 times, remove it
                if (bulletComponent.bounces >= 10) {
                    BULLETLIST.remove(bullet);
                    bullet.removeFromWorld();
                }
            }
        });
        // Bullet vs. Player Tank
        getPhysicsWorld().addCollisionHandler(new CollisionHandler(EntityType.BULLETS, EntityType.TANK) {
            @Override
            protected void onHitBoxTrigger(Entity bullet, Entity tank, HitBox bulletBox, HitBox tankBox) {
                // Remove bullet from the world
                BULLETLIST.remove(bullet);
                bullet.removeFromWorld();

                // Access the TankComponent of the tank
                TankComponent tankComponent = tank.getComponent(TankComponent.class);

                if (tankComponent != null) {
                    int playerNumber = tankComponent.GetPlayerNumber();
                    ScoreHandler(playerNumber);
                }

                loadNextMap();
            }

        });

    }

    protected void sendGameScoresToClient() {
        String message = "SCORE_DATA," + player1Score + "," + player2Score;

        server.broadcast(message);
    }
    @Override
    protected void initUI() {
        MainUIController controller = new MainUIController();
        UI ui = getAssetLoader().loadUI("main.fxml", controller);

        controller.getLabelScorePlayer().textProperty().bind(getip("player1score").asString());
        controller.getLabelScoreEnemy().textProperty().bind(getip("player2score").asString());

        getGameScene().addUI(ui);
    }

    @Override
    protected void onUpdate(double tpf) {

        if (!server.getConnections().isEmpty()) {


            StringBuilder message = new StringBuilder("GAME_DATA,");



            // Convert rotations to integers (rounding to nearest integer)
            int player1Rotation = (int) Math.round(player1.getRotation());
            int player2Rotation = (int) Math.round(player2.getRotation());

            // Player positions and rotations
            message.append(player1.getX()).append(",")
                    .append(player1.getY()).append(",")
                    .append(player1Rotation).append(",");
            message.append(player2.getX()).append(",")
                    .append(player2.getY()).append(",")
                    .append(player2Rotation).append(",");


            if (BULLETLIST != null) {
                // Bullet count and positions (only if there are bullets)
                if (!BULLETLIST.isEmpty()) {
                    message.append(BULLETLIST.size()).append(",");

                    for (Entity bullet : BULLETLIST) {
                        message.append(bullet.getX()).append(",")
                                .append(bullet.getY()).append(",");
                    }

                    // Remove the trailing comma
                    message.setLength(message.length() - 1);
                }
            }

            // Broadcast the constructed message to all connected clients
            server.broadcast(message.toString());
        }
    }

    private void initScreenBounds() {
        Entity walls = entityBuilder()
                .type(EntityType.WALL)
                .collidable()
                .buildScreenBounds(150);

        getGameWorld().addEntity(walls);
    }

    private void initGameObjects() {
        player1 = spawn("tank", new SpawnData(getAppWidth() / 4, getAppHeight() / 2 - 30).put("isPlayer", true).put("playerNumber", 1));
        player2 = spawn("tank", new SpawnData(3 * getAppWidth() / 4 - 20, getAppHeight() / 2 - 30).put("isPlayer", false).put("playerNumber", 2));

        TankPlayer1 = player1.getComponent(TankComponent.class);
        TankPlayer2 = player2.getComponent(TankComponent.class);

        TankPlayer1.SetPlayerNumber(1);
        TankPlayer2.SetPlayerNumber(2);
    }


    private void ScoreHandler(int playerNumber) {
        if (playerNumber == 1) {
            inc("player2score", +1);
            player2Score++;
            System.out.println("Player 1 hit Player 2!");

            // Check if either player's score reaches the threshold
            if (player1Score >= 10 || player2Score >= 10) {
                System.out.println("Match over! Resetting scores and map.");

                // Subtract the current scores from the global scoreboard
                inc("player1score", -player1Score);
                inc("player2score", -player2Score);

                // Reset local scores and map
                player1Score = 0;
                player2Score = 0;
                mapHandler.setMap(1);
            }
        } else {
            inc("player1score", +1);
            player1Score++;
            System.out.println("Player 2 hit Player 1!");

            // Check if either player's score reaches the threshold
            if (player1Score >= 10 || player2Score >= 10) {
                System.out.println("Match over! Resetting scores and map.");

                // Subtract the current scores from the global scoreboard
                inc("player1score", -player1Score);
                inc("player2score", -player2Score);

                // Reset local scores and map
                player1Score = 0;
                player2Score = 0;
                mapHandler.setMap(1);
            }
        }
        sendGameScoresToClient();
    }


    @Override
    public void onReceive(Connection<String> connection, String message) {
        var tokens = message.split(",");

        for (String keyState : tokens) {
            if (keyState.endsWith("_DOWN")) {
                KeyCode key = KeyCode.valueOf(keyState.substring(0, keyState.length() - 5));
                // Handle key press (e.g., moving forward, backward, left, right)
                if (key == KeyCode.W) {
                    // Move forward
                    TankPlayer1.Forward();
                } else if (key == KeyCode.S) {
                    // Move backward
                    TankPlayer1.Backward();
                } else if (key == KeyCode.A) {
                    // Move left
                    TankPlayer1.Left();
                } else if (key == KeyCode.D) {
                    // Move right
                    TankPlayer1.Right();
                } else if (key == KeyCode.SPACE) {
                    // Move right
                    TankPlayer1.Fire();
                }
                } else if (keyState.endsWith("_UP")) {
                    KeyCode key = KeyCode.valueOf(keyState.substring(0, keyState.length() - 3));
                    TankPlayer1.stop();
                    // Handle key release
                }
        }
    }



    static class MessageWriterS implements TCPMessageWriter<String> {

        private OutputStream os;
        private PrintWriter out;

        MessageWriterS(OutputStream os) {
            this.os = os;
            out = new PrintWriter(os, true);
        }

        @Override
        public void write(String s) throws Exception {
            out.print(s.toCharArray());
            out.flush();
        }
    }

    static class MessageReaderS implements TCPMessageReader<String> {

        private BlockingQueue<String> messages = new ArrayBlockingQueue<>(50);

        private InputStreamReader in;

        MessageReaderS(InputStream is) {
            in =  new InputStreamReader(is);

            var t = new Thread(() -> {
                try {

                    char[] buf = new char[36];

                    int len;

                    while ((len = in.read(buf)) > 0) {
                        var message = new String(Arrays.copyOf(buf, len));

                        System.out.println("Recv message: " + message);

                        messages.put(message);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            t.setDaemon(true);
            t.start();
        }

        @Override
        public String read() throws Exception {
            return messages.take();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void renderMap() {
        BULLETLIST.clear();
        WALLLIST.clear();

        // Clear the current game world
        getGameWorld().getEntitiesCopy().forEach(Entity::removeFromWorld);

        int tileSize = 32; // Each tile is 32x32 pixels

        // Check if there are any connections to send the wall data
        if (!server.getConnections().isEmpty()) {
            StringBuilder wallMessage = new StringBuilder("WALLS_DATA,");
            // Render the map tiles and collect wall data
            for (int row = 0; row < mapHandler.getCurrentMap().length; row++) {
                for (int col = 0; col < mapHandler.getCurrentMap()[row].length; col++) {
                    int tileType = mapHandler.getCurrentMap()[row][col];

                    // If the tile type is 1, it's a wall (you can adjust the condition as needed)
                    if (tileType == 1) {
                        // Spawn the wall in the game world
                        spawn("wall", col * tileSize, row * tileSize);

                        // Add the wall's coordinates to the wallMessage
                        wallMessage.append(col).append(",")
                                .append(row).append(",");
                    }
                }
            }
            if (wallMessage.length() > "WALLS_DATA,".length()) {

                wallMessage.setLength(wallMessage.length() - 1);
                server.broadcast(wallMessage.toString());
            }
        }
    }

    public void loadNextMap() {
       mapHandler.currentMapIndex = (mapHandler.currentMapIndex + 1) % mapHandler.allMaps.length;
        mapHandler.setMap(mapHandler.currentMapIndex);
        renderMap();
        initGameObjects();
        // Ensure map is rendered after loading
    }

}
