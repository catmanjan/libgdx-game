package com.catmanjan.bsp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.model.Animation;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodeAnimation;
import com.badlogic.gdx.graphics.g3d.model.NodeKeyframe;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;

import java.util.HashMap;

public class ModelHelpers {

    public static void copyAnimationsFromModel(Model targetModel, Model animationModel, String name){
        for (Animation sourceAnimation : animationModel.animations) {
            Animation targetAnimation = new Animation();
            targetAnimation.id = name;
            targetAnimation.duration = sourceAnimation.duration;

            for (final Node node : animationModel.nodes) {
                copyAnimationsFromNodes(targetAnimation, sourceAnimation, node, targetModel);
            }

            if (targetAnimation.nodeAnimations.size > 0) {
                targetModel.animations.add(targetAnimation);
            }
        }
    }


    private static void copyAnimationsFromNodes(Animation targetAnimation, Animation sourceAnimation, Node node, Model model) {
        if (node.hasChildren()) {
            for (final Node child : node.getChildren()) {
                copyAnimationsFromNodes(targetAnimation, sourceAnimation, child, model);
            }
        }

        final Node modelNode = model.getNode(node.id);

        if (modelNode == null) {
            return;
        }

        NodeAnimation sourceNodeAnimation = null;

        // look for existing node animation
        for (final NodeAnimation tmp : sourceAnimation.nodeAnimations) {
            if (tmp.node.id.equals(node.id)) {
                sourceNodeAnimation = tmp;
                break;
            }
        }

        NodeAnimation newNodeAnimation = new NodeAnimation();
        newNodeAnimation.node = modelNode;
        newNodeAnimation.translation = new Array<>();
        newNodeAnimation.rotation = new Array<>();
        newNodeAnimation.scaling = new Array<>();

        if (sourceNodeAnimation != null && sourceNodeAnimation.translation != null) {
            for (final NodeKeyframe<Vector3> kf : sourceNodeAnimation.translation) {
                newNodeAnimation.translation.add(new NodeKeyframe<>(kf.keytime, kf.value));
            }
        } else {
            newNodeAnimation.translation.add(new NodeKeyframe<>(0, node.translation));
        }

        if (sourceNodeAnimation != null && sourceNodeAnimation.rotation != null) {
            for (final NodeKeyframe<Quaternion> kf : sourceNodeAnimation.rotation) {
                newNodeAnimation.rotation.add(new NodeKeyframe<>(kf.keytime, kf.value));
            }
        } else {
            newNodeAnimation.rotation.add(new NodeKeyframe<>(0, node.rotation));
        }

        if (sourceNodeAnimation != null && sourceNodeAnimation.scaling != null) {
            for (final NodeKeyframe<Vector3> kf : sourceNodeAnimation.scaling) {
                newNodeAnimation.scaling.add(new NodeKeyframe<>(kf.keytime, kf.value));
            }
        } else {
            newNodeAnimation.scaling.add(new NodeKeyframe<>(0, node.scale));
        }

        targetAnimation.nodeAnimations.add(newNodeAnimation);
    }
}
