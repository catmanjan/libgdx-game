package com.catmanjan.bsp;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.catmanjan.bsp.shared.Constants;
import com.catmanjan.bsp.shared.Snapshot;
import com.catmanjan.bsp.shared.Entity;
import com.catmanjan.bsp.shared.UserCommand;
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketHandler;
import com.github.czyzby.websocket.WebSocketHandler.Handler;
import com.github.czyzby.websocket.WebSockets;
import com.github.czyzby.websocket.net.ExtendedNet;
import com.github.czyzby.websocket.serialization.impl.ManualSerializer;

public class MyGdxGame implements ApplicationListener, InputProcessor {
    private AssetManager assetManager;
    private boolean loading;

    private PerspectiveCamera perspectiveCamera;
    private ObjectMap<String, EntityModel> entityModels = new ObjectMap<>();
    private ModelBatch modelBatch;
    private Model soldierBlue;
    private Model soldierBlueLocal;
    private Model soldierRed;
    private Model soldierRedLocal;
    private ModelInstance level;
    private Environment environment;
    private BitmapFont fontRed;
    private BitmapFont fontBlue;
    private SpriteBatch spriteBatch;

    private WebSocket socket;

    private EntityModel localPlayerEntityModel;
    private Vector3 localPlayerVelocity = new Vector3();
    private Vector3 localPlayerDirection = new Vector3();

    private long lastServerUpdate = 0;

    private Vector3 tmp = new Vector3();

    private boolean chatting;
    private String chatText;

    @Override
    public void create() {
        perspectiveCamera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        perspectiveCamera.near = 0.1f;
        perspectiveCamera.far = 100;

        assetManager = new AssetManager();
        assetManager.load("soldierBlue.g3dj", Model.class);
        assetManager.load("soldierBlueLocal.g3dj", Model.class);
        assetManager.load("soldierRed.g3dj", Model.class);
        assetManager.load("soldierRedLocal.g3dj", Model.class);
        assetManager.load("soldierIdle.g3dj", Model.class);
        assetManager.load("soldierRun.g3dj", Model.class);
        assetManager.load("level.g3dj", Model.class);

        fontBlue = new BitmapFont(Gdx.files.internal("fontBlue.fnt"));
        fontRed = new BitmapFont(Gdx.files.internal("fontRed.fnt"));
        spriteBatch = new SpriteBatch();

        loading = true;

        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.75f, 0.75f, 0.75f, 1f));

        ManualSerializer serializer = new ManualSerializer();
        serializer.register(new Entity());
        serializer.register(new UserCommand());
        serializer.register(new Snapshot());

        WebSocketHandler handler = new WebSocketHandler();
        handler.registerHandler(Snapshot.class, new Handler<Snapshot>() {

            @Override
            public boolean handle(WebSocket webSocket, Snapshot snapshot) {
                lastServerUpdate = TimeUtils.millis();

                for (Entity entity : snapshot.entities) {
                    EntityModel entityModel;

                    if (entityModels.containsKey(entity.id)) {
                        entityModel = entityModels.get(entity.id);
                    } else {
                        Model playerModel = null;

                        if (entity.team == 0) {
                            if (entity.id.equals(snapshot.receiverId)) {
                                playerModel = soldierBlueLocal;
                            } else {
                                playerModel = soldierBlue;
                            }
                        } else {
                            if (entity.id.equals(snapshot.receiverId)) {
                                playerModel = soldierRedLocal;
                            } else {
                                playerModel = soldierRed;
                            }
                        }

                        entityModel = new EntityModel(playerModel, entity.position, entity.direction);
                        entityModels.put(entity.id, entityModel);
                    }

                    entityModel.position = entity.position;
                    entityModel.direction = entity.direction;

                    if (entity.velocity.isZero()) {
                        entityModel.animationController.animate("idle", -1, 1f, null, 0.2f);
                    } else {
                        entityModel.animationController.animate("shoot", -1, 1f, null, 0.2f);
                    }
                }

                localPlayerEntityModel = entityModels.get(snapshot.receiverId);

                return true;
            }
        });

        socket = ExtendedNet.getNet().newWebSocket("catmanjan.australiaeast.cloudapp.azure.com", 8000);
        socket.setSerializer(serializer);
        socket.addListener(handler);
        socket.connect();

        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);
    }

    private void doneLoading() {
        soldierBlue = assetManager.get("soldierBlue.g3dj", Model.class);
        soldierBlueLocal = assetManager.get("soldierBlueLocal.g3dj", Model.class);
        soldierRed = assetManager.get("soldierRed.g3dj", Model.class);
        soldierRedLocal = assetManager.get("soldierRedLocal.g3dj", Model.class);
        level = new ModelInstance(assetManager.get("level.g3dj", Model.class));

        Model idle = assetManager.get("soldierIdle.g3dj", Model.class);
        Model run = assetManager.get("soldierRun.g3dj", Model.class);

        ModelHelpers.copyAnimationsFromModel(soldierBlue, idle, "idle");
        ModelHelpers.copyAnimationsFromModel(soldierBlue, run, "shoot");
        ModelHelpers.copyAnimationsFromModel(soldierBlueLocal, idle, "idle");
        ModelHelpers.copyAnimationsFromModel(soldierBlueLocal, run, "shoot");
        ModelHelpers.copyAnimationsFromModel(soldierRed, idle, "idle");
        ModelHelpers.copyAnimationsFromModel(soldierRed, run, "shoot");
        ModelHelpers.copyAnimationsFromModel(soldierRedLocal, idle, "idle");
        ModelHelpers.copyAnimationsFromModel(soldierRedLocal, run, "shoot");

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

        float alpha = (float)TimeUtils.timeSinceMillis(lastServerUpdate) / (float)Constants.TICK_INTERVAL;

        for (EntityModel entityModel : entityModels.values()) {
            entityModel.update(alpha);
        }

        // input
        Vector3 forward = new Vector3();
        forward.set(perspectiveCamera.direction);
        forward.y = 0;
        Vector3 right = new Vector3();
        right.set(perspectiveCamera.direction);
        right.crs(perspectiveCamera.up);
        right.y = 0;

        localPlayerVelocity.setZero();

        if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayerVelocity.set(forward).add(right.scl(-1)).nor();
        } else if (Gdx.input.isKeyPressed(Input.Keys.W) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayerVelocity.set(forward).add(right).nor();
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayerVelocity.set(forward).add(right.scl(-1)).nor().scl(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.S) && Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayerVelocity.set(forward).add(right).nor().scl(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            localPlayerVelocity.set(forward).nor();
        } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            localPlayerVelocity.set(forward).nor().scl(-1);
        } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            localPlayerVelocity.set(right).nor().scl(-1);
        }  else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            localPlayerVelocity.set(right).nor();
        }

        localPlayerDirection.set(perspectiveCamera.direction);
        localPlayerDirection.rotate(Vector3.Y, 90);
        localPlayerDirection.crs(perspectiveCamera.up);
        localPlayerDirection.nor();
        localPlayerDirection.y = 0;

        // networking
        UserCommand command = new UserCommand();
        command.velocity = localPlayerVelocity;
        command.direction = localPlayerDirection;
        command.shoot = Gdx.input.isKeyPressed(Input.Buttons.LEFT) || Gdx.input.isKeyPressed(Input.Buttons.RIGHT);
        command.jump = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (!loading && socket.isOpen()) {
            socket.send(command);
        }

        if (localPlayerEntityModel != null) {
            perspectiveCamera.position.set(localPlayerEntityModel.realPosition);
            perspectiveCamera.position.add(0, 8.2f, 0);
            perspectiveCamera.update();
        }

        for (EntityModel entityModel : entityModels.values()) {
            entityModel.animationController.update(Gdx.graphics.getDeltaTime());
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        modelBatch.begin(perspectiveCamera);
        for (EntityModel entityModel : entityModels.values()) {
            modelBatch.render(entityModel.modelInstance, environment);
        }
        modelBatch.render(level, environment);
        modelBatch.end();

        spriteBatch.begin();
        fontBlue.draw(spriteBatch, "hello", 0, fontBlue.getLineHeight());
        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            Gdx.input.setCursorCatched(false);
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Gdx.input.setCursorCatched(true);

        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        mouseLook();

        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        mouseLook();

        return true;
    }

    private void mouseLook() {
        if (Gdx.input.isCursorCatched()) {
            float deltaX = -Gdx.input.getDeltaX() * 0.5f;
            float deltaY = -Gdx.input.getDeltaY() * 0.5f;
            perspectiveCamera.direction.rotate(perspectiveCamera.up, deltaX);
            tmp.set(perspectiveCamera.direction).crs(perspectiveCamera.up).nor();
            perspectiveCamera.direction.rotate(tmp, deltaY);
        }
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    @Override
    public void dispose() {
        WebSockets.closeGracefully(socket);
    }

    class EntityModel {
        public ModelInstance modelInstance;
        public AnimationController animationController;
        // set every tick
        public Vector3 position;
        public Vector3 direction;
        // lerping
        public Vector3 realPosition;
        public Vector3 realDirection;

        public EntityModel(Model model, Vector3 position, Vector3 direction) {
            this.position = this.realPosition = position;
            this.direction = this.realDirection = direction;

            modelInstance = new ModelInstance(model);
            animationController = new AnimationController(modelInstance);
        }

        public void update(float alpha) {
            realPosition.lerp(position, alpha);
            realDirection.lerp(direction, alpha);

            modelInstance.transform.setToRotation(Vector3.Z, realDirection).trn(realPosition);
        }
    }
}
