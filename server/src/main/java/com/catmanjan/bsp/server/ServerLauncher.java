package com.catmanjan.bsp.server;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
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
    private final ObjectMap<ServerWebSocket, Entity> entities = new ObjectMap<>();

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

        for (ObjectMap.Entry<ServerWebSocket, Entity> entry : entities) {
            ServerWebSocket webSocket = entry.key;
            Entity entity = entry.value;

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

            Snapshot snapshot = new Snapshot();
            snapshot.receiverId = entity.id;
            snapshot.entities = entities.values().toArray();

            webSocket.writeFinalBinaryFrame(Buffer.buffer(serializer.serialize(snapshot)));
        }

    }

    private void handleFrame(final ServerWebSocket webSocket, final WebSocketFrame frame) {
        Entity entity;

        synchronized (entities) {
            if (entities.containsKey(webSocket)) {
                entity = entities.get(webSocket);
            } else {
                entity = new Entity();
                entity.id = UUID.randomUUID().toString();
                entity.position = new Vector3(0, 0, 0);
                entity.direction = new Vector3();
                entity.velocity = new Vector3();

                int blueTeamCount = 0;
                int redTeamCount = 0;

                for (Entity e : entities.values()) {
                    if (e.team == 0) {
                        blueTeamCount++;
                    } else {
                        redTeamCount++;
                    }
                }

                if (blueTeamCount > redTeamCount) {
                    entity.team = 1;
                }

                entities.put(webSocket, entity);
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
        }
    }

    private void handleClose(final ServerWebSocket webSocket) {
        synchronized (entities) {
            if (entities.containsKey(webSocket)) {
                entities.remove(webSocket);
            }
        }
    }
}