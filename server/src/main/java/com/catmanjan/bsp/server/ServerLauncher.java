package com.catmanjan.bsp.server;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Queue;
import com.catmanjan.bsp.shared.Constants;
import com.catmanjan.bsp.shared.Entity;
import com.catmanjan.bsp.shared.Snapshot;
import com.catmanjan.bsp.shared.UserCommand;
import com.github.czyzby.websocket.serialization.impl.ManualSerializer;

import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;

public class ServerLauncher {

    private final Vertx vertx = Vertx.vertx();
    private final ManualSerializer serializer;
    private final ObjectMap<ServerWebSocket, ServerEntity> serverEntities = new ObjectMap<>();
    private final Queue<String> chatQueue = new Queue<>(6);

    private float playerSpeed = 2.0f;
    private Vector3 gravity = new Vector3(0, -0.3f, 0);
    private Vector3 tmp = new Vector3();

    private ServerLauncher() {
        serializer = new ManualSerializer();
        serializer.register(new Entity());
        serializer.register(new UserCommand());
        serializer.register(new Snapshot());
    }

    public static void main(final String... args) {
        new ServerLauncher().launch();
    }

    private void launch() {
        System.out.println("Launching web socket server...");

        vertx.setPeriodic(Constants.TICK_INTERVAL, handler -> update());

        final HttpServer server = vertx.createHttpServer();

        server.websocketHandler(webSocket -> {
            webSocket.frameHandler(frame -> handleFrame(webSocket, frame));
            webSocket.closeHandler(frame -> handleClose(webSocket));
        }).listen(8000);
    }

    private void update() {
        float delta = Constants.TICK_INTERVAL / 100;

        // update all serverEntities
        for (ObjectMap.Entry<ServerWebSocket, ServerEntity> entry : serverEntities) {
            Entity entity = entry.value.entity;

            entity.velocity.add(gravity);
            tmp.set(entity.velocity);
            tmp.scl(playerSpeed * delta);
            entity.position.add(tmp);

            if (entity.position.y <= 0) {
                entity.position.y = 0;
                entity.velocity.y = 0;

                if (entity.jump) {
                    entity.velocity.y = 2f;
                }
            }
        }

        StringBuilder snapshotChat = new StringBuilder();

        for (String chat : chatQueue) {
            snapshotChat.append(chat + "\n");
        }

        Array<Entity> snapshotEntities = new Array<>();

        for (ServerEntity serverEntity : serverEntities.values()) {
            snapshotEntities.add(serverEntity.entity);
        }

        // send to all serverEntities
        for (ObjectMap.Entry<ServerWebSocket, ServerEntity> entry : serverEntities) {
            Snapshot snapshot = new Snapshot();
            snapshot.receiverId = entry.value.entity.id;
            snapshot.chat = snapshotChat.toString();
            snapshot.entities = snapshotEntities;

            entry.key.writeFinalBinaryFrame(Buffer.buffer(serializer.serialize(snapshot)));
        }

    }

    private void handleFrame(final ServerWebSocket webSocket, final WebSocketFrame frame) {
        Entity entity;

        synchronized (serverEntities) {
            if (serverEntities.containsKey(webSocket)) {
                entity = serverEntities.get(webSocket).entity;
            } else {
                entity = new Entity();
                entity.id = UUID.randomUUID().toString();
                entity.name = "anonymous";
                entity.position = new Vector3(0, 0, 0);
                entity.direction = new Vector3();
                entity.velocity = new Vector3();

                int blueTeamCount = 0;
                int redTeamCount = 0;

                for (ServerEntity serverEntity : serverEntities.values()) {
                    if (serverEntity.entity.team == 0) {
                        blueTeamCount++;
                    } else {
                        redTeamCount++;
                    }
                }

                if (blueTeamCount > redTeamCount) {
                    entity.team = 1;
                }

                ServerEntity serverEntity = new ServerEntity();
                serverEntity.entity = entity;

                serverEntities.put(webSocket, serverEntity);

                chatQueue.addFirst(entity.name + " joined the game");
            }
        }

        final Object request = serializer.deserialize(frame.binaryData().getBytes());

        if (request instanceof UserCommand) {
            UserCommand userCommand = (UserCommand) request;
            entity.velocity.x = userCommand.velocity.x;
            entity.velocity.z = userCommand.velocity.z;
            entity.direction = userCommand.direction;
            entity.jump = userCommand.jump;
            entity.shoot = userCommand.shoot;

            String chat = userCommand.chat.trim();

            if (chat.length() > 0) {
                if (chat.startsWith("/name ")) {
                    String name = chat.substring("/name ".length());

                    if (name.length() > 0) {
                        chatQueue.addFirst(entity.name + " renamed to " + name);
                        entity.name = name;
                    }
                } else {
                    chatQueue.addFirst(entity.name + ": " + chat);
                }
            }
        }
    }

    private void handleClose(final ServerWebSocket webSocket) {
        synchronized (serverEntities) {
            if (serverEntities.containsKey(webSocket)) {
                serverEntities.remove(webSocket);
            }
        }
    }

    class ServerEntity {

        Entity entity;

    }
}