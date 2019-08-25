package com.catmanjan.bsp;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;

public class EntityModel {

    public ModelInstance modelInstance;
    public AnimationController animationController;

    public EntityModel(Model model) {
        modelInstance = new ModelInstance(model);
        animationController = new AnimationController(modelInstance);
    }

}