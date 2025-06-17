package com.example.myapplication

import android.content.Context
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.model.Model
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import io.github.sceneview.node.ViewNode
import android.widget.TextView
import android.view.LayoutInflater
import io.github.sceneview.node.LightNode

class ModelLoaderWrapper(
    private val sceneView: SceneView,

    private val context: Context
) {
    private val modelLoader = ModelLoader(sceneView.engine, context, MainScope())
    private val coroutineScope = MainScope()
    fun interface OnModelLoadSuccess {
        fun onSuccess(modelNode: ModelNode)
    }

    fun interface OnModelLoadError {
        fun onError(error: Throwable)
    }
    fun loadModel(
        modelUrl: String,
        onSuccess: OnModelLoadSuccess,
        onError: OnModelLoadError
    )
            {
            coroutineScope.launch {
                try {
                    val model = modelLoader.loadModel(modelUrl)
                    if (model != null) {
                        val modelInstance = model.instance
                        val modelNode = ModelNode(modelInstance)

                        onSuccess.onSuccess(modelNode)
                    } else {
                        onError.onError(NullPointerException("ModelLoader.loadModel returned null"))
                    }
                } catch (e: Throwable) {
                    onError.onError(e)
                }
            }
        }

}
