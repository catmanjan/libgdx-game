package com.catmanjan.bsp.server;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
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
import io.vertx.core.net.SocketAddress;

public class ServerLauncher {

    private final Vertx vertx = Vertx.vertx();
    private final ManualSerializer serializer;

    ObjectMap<SocketAddress, Entity> entities = new ObjectMap<>();

    public ServerLauncher() {
        serializer = new ManualSerializer();
        serializer.register(new Entity());
        serializer.register(new UserCommand());
        serializer.register(new Snapshot());
    }

    public static void main(final String... args) throws Exception {
        new ServerLauncher().launch();
    }

    private void launch() {
        System.out.println("Launching web socket server...");
        final HttpServer server = vertx.createHttpServer();
        server.websocketHandler(webSocket -> {
            // Printing received packets to console, sending response:
            webSocket.frameHandler(frame -> handleFrame(webSocket, frame));
            webSocket.closeHandler(frame -> handleClose(webSocket));
        }).listen(8000);
    }

    private void handleFrame(final ServerWebSocket webSocket, final WebSocketFrame frame) {
        SocketAddress remoteAddress = webSocket.remoteAddress();
        Entity entity;

        synchronized (entities) {
            if (entities.containsKey(remoteAddress)) {
                entity = entities.get(remoteAddress);
            } else {
                entity = new Entity();
                entity.id = UUID.randomUUID().toString();
                entity.position = new Vector3(0, 10, 0);
                entity.direction = new Vector3();
                entity.velocity = new Vector3();

                entities.put(remoteAddress, entity);
            }
        }

        final Object request = serializer.deserialize(frame.binaryData().getBytes());

        if (request instanceof UserCommand) {
            UserCommand userCommand = (UserCommand) request;

            entity.velocity = userCommand.velocity;
            entity.direction = userCommand.direction;
            entity.crouch = userCommand.crouch;
            entity.run = userCommand.run;
            
            entity.position.add(entity.velocity);

            Snapshot snapshot = new Snapshot();
            snapshot.receiverId = entity.id;
            snapshot.entities = entities.values().toArray();

            webSocket.writeFinalBinaryFrame(Buffer.buffer(serializer.serialize(snapshot)));
        }
    }

    private void handleClose(final ServerWebSocket webSocket) {
        SocketAddress remoteAddress = webSocket.remoteAddress();

        synchronized (entities) {
            if (entities.containsKey(remoteAddress)) {
                entities.remove(remoteAddress);
            }
        }
    }
}