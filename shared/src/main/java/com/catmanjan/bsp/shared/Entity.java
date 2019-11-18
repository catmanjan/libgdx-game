package com.catmanjan.bsp.shared;

import com.badlogic.gdx.math.Vector3;
import com.github.czyzby.websocket.serialization.SerializationException;
import com.github.czyzby.websocket.serialization.Transferable;
import com.github.czyzby.websocket.serialization.impl.Deserializer;
import com.github.czyzby.websocket.serialization.impl.Serializer;

public class Entity implements Transferable<Entity> {

    public String id;
    public String name;
    public Vector3 position;
    public Vector3 direction;
    public Vector3 velocity;
    public boolean jump;
    public boolean shoot;
    public int team;

    @Override
    public void serialize(Serializer serializer) throws SerializationException {
        serializer.serializeString(id)
                .serializeString(name)
                .serializeFloat(position.x)
                .serializeFloat(position.y)
                .serializeFloat(position.z)
                .serializeFloat(direction.x)
                .serializeFloat(direction.y)
                .serializeFloat(direction.z)
                .serializeFloat(velocity.x)
                .serializeFloat(velocity.y)
                .serializeFloat(velocity.z)
                .serializeBoolean(jump)
                .serializeBoolean(shoot)
                .serializeInt(team);
    }

    @Override
    public Entity deserialize(Deserializer deserializer) throws SerializationException {
        Entity entity = new Entity();
        entity.id = deserializer.deserializeString();
        entity.name = deserializer.deserializeString();
        entity.position = new Vector3(deserializer.deserializeFloat(), deserializer.deserializeFloat(), deserializer.deserializeFloat());
        entity.direction = new Vector3(deserializer.deserializeFloat(), deserializer.deserializeFloat(), deserializer.deserializeFloat());
        entity.velocity = new Vector3(deserializer.deserializeFloat(), deserializer.deserializeFloat(), deserializer.deserializeFloat());
        entity.jump = deserializer.deserializeBoolean();
        entity.shoot = deserializer.deserializeBoolean();
        entity.team = deserializer.deserializeInt();

        return entity;
    }
}
