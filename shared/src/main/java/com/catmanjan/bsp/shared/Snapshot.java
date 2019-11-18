package com.catmanjan.bsp.shared;

import com.badlogic.gdx.utils.Array;
import com.github.czyzby.websocket.serialization.SerializationException;
import com.github.czyzby.websocket.serialization.Transferable;
import com.github.czyzby.websocket.serialization.impl.Deserializer;
import com.github.czyzby.websocket.serialization.impl.Serializer;

public class Snapshot implements Transferable<Snapshot> {

    public String receiverId;
    public String chat;
    public Array<Entity> entities;

    @Override
    public void serialize(Serializer serializer) throws SerializationException {
        serializer.serializeString(receiverId);
        serializer.serializeString(chat);
        serializer.serializeInt(entities.size);

        for (Entity entity : entities) {
            serializer.serializeTransferable(entity);
        }
    }

    @Override
    public Snapshot deserialize(Deserializer deserializer) throws SerializationException {
        Snapshot snapshot = new Snapshot();
        snapshot.receiverId = deserializer.deserializeString();
        snapshot.chat = deserializer.deserializeString();

        Entity[] entities = new Entity[deserializer.deserializeInt()];

        for (int i = 0; i < entities.length; i++) {
            entities[i] = deserializer.deserializeTransferable(new Entity());
        }

        snapshot.entities = new Array<>(entities);

        return snapshot;
    }
}
