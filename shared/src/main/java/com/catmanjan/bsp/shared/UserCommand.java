package com.catmanjan.bsp.shared;

import com.badlogic.gdx.math.Vector3;
import com.github.czyzby.websocket.serialization.SerializationException;
import com.github.czyzby.websocket.serialization.Transferable;
import com.github.czyzby.websocket.serialization.impl.Deserializer;
import com.github.czyzby.websocket.serialization.impl.Serializer;

public class UserCommand implements Transferable<UserCommand> {

    public Vector3 velocity;
    public Vector3 direction;
    public boolean run;
    public boolean crouch;

    @Override
    public void serialize(Serializer serializer) throws SerializationException {
        serializer.serializeFloat(velocity.x)
                .serializeFloat(velocity.y)
                .serializeFloat(velocity.z)
                .serializeFloat(direction.x)
                .serializeFloat(direction.y)
                .serializeFloat(direction.z)
                .serializeBoolean(run)
                .serializeBoolean(crouch);
    }

    @Override
    public UserCommand deserialize(Deserializer deserializer) throws SerializationException {
        UserCommand userCommand = new UserCommand();
        userCommand.velocity = new Vector3(deserializer.deserializeFloat(), deserializer.deserializeFloat(), deserializer.deserializeFloat());
        userCommand.direction = new Vector3(deserializer.deserializeFloat(), deserializer.deserializeFloat(), deserializer.deserializeFloat());
        userCommand.run = deserializer.deserializeBoolean();
        userCommand.crouch = deserializer.deserializeBoolean();

        return userCommand;
    }
}
