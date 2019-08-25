package com.catmanjan.bsp;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.catmanjan.bsp.shared.Snapshot;
import com.catmanjan.bsp.shared.Entity;
import com.catmanjan.bsp.shared.UserCommand;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketHandler;
import com.github.czyzby.websocket.WebSocketHandler.Handler;
import com.github.czyzby.websocket.WebSockets;
import com.github.czyzby.websocket.net.ExtendedNet;
import com.github.czyzby.websocket.serialization.impl.ManualSerializer;

import java.util.HashMap;

public class MyGdxGame implements ApplicationListener {

    private AssetManager assetManager;
    private boolean loading;

    private ChaseCamera camera;
    private HashMap<String, EntityModel> entityModels = new HashMap<>();
    private ModelBatch modelBatch;
    private ModelBatch shadowBatch;
    private Model model;
    private ModelInstance level;
    private Environment environment;
    private DirectionalShadowLight shadowLight;

    private WebSocket socket;

    private Vector3 localPlayerDirection = new Vector3();
    private Vector3 localPlayerPosition = new Vector3();

    float yOffset = 6;//8
    float zOffset = -9;//-12
    float blockRadiusForFrustrumCulling = 11;//(float) Math.sqrt(10 * 10 + 10 * 10);

    @Override
    public void create() {
        camera = new ChaseCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.transform.setToTranslation(Vector3.Zero);
        camera.position.set(yOffset, yOffset, zOffset);
        camera.desiredLocation.set(0, yOffset, zOffset);
        camera.targetOffset.set(0, 1, 0);
        camera.near = 1;
        camera.far = 100;
        camera.acceleration = 100;
        camera.rotationSpeed = 0;

        assetManager = new AssetManager();
        assetManager.load("robot.g3dj", Model.class);
        assetManager.load("level.g3dj", Model.class);
        loading = true;

        modelBatch = new ModelBatch();
        shadowBatch = new ModelBatch(new DepthShaderProvider());
        shadowLight = new DirectionalShadowLight(1024, 1024, 100f, 100f, 1f, 100f);
        shadowLight.set(0.3f, 0.3f, 0.3f, 0.5f, -0.5f, 0.5f);
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.75f, 0.75f, 0.75f, 1f));
        environment.set(new ColorAttribute(ColorAttribute.Fog, 1.0f, 1.0f, 1.0f, 1f));
        environment.add(shadowLight);
        environment.shadowMap = shadowLight;

        ManualSerializer serializer = new ManualSerializer();
        serializer.register(new Entity());
        serializer.register(new UserCommand());
        serializer.register(new Snapshot());

        WebSocketHandler handler = new WebSocketHandler();
        handler.registerHandler(Snapshot.class, new Handler<Snapshot>() {

            @Override
            public boolean handle(WebSocket webSocket, Snapshot snapshot) {
                for (Entity entity : snapshot.entities) {
                    if (entity.id.equals(snapshot.receiverId)) {
                        localPlayerPosition = entity.position;
                    }

                    EntityModel entityModel;

                    if (entityModels.containsKey(entity.id)) {
                        entityModel = entityModels.get(entity.id);
                    } else {
                        entityModel = new EntityModel(model);

                        entityModels.put(entity.id, entityModel);
                    }

                    if (entity.velocity.isZero()) {
                        if (entity.crouch) {
                            entityModel.animationController.animate("crouch", -1, 1f, null, 0.2f);
                        } else {
                            entityModel.animationController.animate("idle", -1, 1f, null, 0.2f);
                        }
                    } else {
                        if (entity.crouch) {
                            entityModel.animationController.animate("crawl", -1, 1f, null, 0.2f);
                        } else if (entity.run) {
                            entityModel.animationController.animate("run", -1, 1f, null, 0.2f);
                        } else {
                            entityModel.animationController.animate("walk", -1, 1f, null, 0.2f);
                        }
                    }

                    entityModel.modelInstance.transform.setToRotation(Vector3.Z, entity.direction).trn(entity.position);
                }

                return true;
            }
        });

        socket = ExtendedNet.getNet().newWebSocket("icogspjan", 8000);
        socket.setSerializer(serializer);
        socket.addListener(handler);
        socket.connect();
    }

    private void doneLoading() {
        model = assetManager.get("robot.g3dj", Model.class);
        Model levelModel = assetManager.get("level.g3dj", Model.class);
        level = new ModelInstance(levelModel);

        loading = false;
    }

    @Override
    public void render() {
        // load assets
        if (loading && assetManager.update()) {
            doneLoading();
        }

        if (loading) {
            return;
        }

        // input
        Vector3 localPlayerVelocity = new Vector3();

        Vector3 forward = new Vector3();
        forward.set(camera.direction);
        forward.y = 0;
        Vector3 right = new Vector3();
        right.set(camera.direction.crs(camera.up));
        right.y = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayerVelocity.set(forward).add(right.scl(-1)).nor();
            localPlayerDirection.set(forward).add(right).nor();
        } else if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayerVelocity.set(forward).add(right).nor();
            localPlayerDirection.set(forward).add(right).nor();
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayerVelocity.set(forward).add(right.scl(-1)).nor().scl(-1);
            localPlayerDirection.set(forward).add(right).nor().scl(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayerVelocity.set(forward).add(right).nor().scl(-1);
            localPlayerDirection.set(forward).add(right).nor().scl(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            localPlayerVelocity.set(forward).nor();
            localPlayerDirection.set(forward).nor();
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            localPlayerVelocity.set(forward).nor().scl(-1);
            localPlayerDirection.set(forward).nor().scl(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayerVelocity.set(right).nor().scl(-1);
            localPlayerDirection.set(right).nor().scl(-1);
        }  else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayerVelocity.set(right).nor();
            localPlayerDirection.set(right).nor();
        }

        // networking
        UserCommand command = new UserCommand();
        command.velocity = localPlayerVelocity;
        command.direction = localPlayerDirection;
        command.run = !Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
        command.crouch = Gdx.input.isKeyPressed(Input.Keys.C);

        if (!loading && socket.isOpen()) {
            socket.send(command);
        }

        camera.transform.setToTranslation(localPlayerPosition);
        camera.position.y = localPlayerPosition.y + yOffset;
        camera.update();

        Node workbenchNode = level.nodes.get(0);

        for (Node childNode : workbenchNode.getChildren()) {
            boolean enable = camera.frustum.sphereInFrustum(childNode.translation, blockRadiusForFrustrumCulling);

            for (NodePart childNodePart : childNode.parts) {
                childNodePart.enabled = enable;
            }
        }

        for (EntityModel entityModel : entityModels.values()) {
            entityModel.animationController.update(Gdx.graphics.getDeltaTime());
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        shadowLight.begin(localPlayerPosition, camera.direction);
        shadowBatch.begin(shadowLight.getCamera());
        for (EntityModel entityModel : entityModels.values()) {
            shadowBatch.render(entityModel.modelInstance, environment);
        }
        shadowBatch.render(level, environment);
        shadowBatch.end();
        shadowLight.end();

        if (Gdx.input.isKeyPressed(Input.Keys.L)) {
            modelBatch.begin(shadowLight.getCamera());
        } else {
            modelBatch.begin(camera);
        }
        for (EntityModel entityModel : entityModels.values()) {
            modelBatch.render(entityModel.modelInstance, environment);
        }
        modelBatch.render(level, environment);
        modelBatch.end();
    }

    @Override
    public void dispose() {
        WebSockets.closeGracefully(socket);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
}
