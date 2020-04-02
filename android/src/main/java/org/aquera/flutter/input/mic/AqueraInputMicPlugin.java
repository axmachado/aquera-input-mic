/*
 * AqueraInputMicPlugin - Flutter microphone input plugin from Aquera Foundation
 * Copyright © 2020 - All rights reserved.
 */
package org.aquera.flutter.input.mic;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * AqueraInputMicPlugin
 * <p>
 * Flutter plugin for microphone input
 *
 * @author Alexandre Machado (axmachado@gmail.com)
 * @copyright Copyright © 2020 - Aquera Foundation
 *
 */

public class AqueraInputMicPlugin implements FlutterPlugin {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel methodChannel;
    // EventChannel - communicate microphone events
    private EventChannel eventChannel;
    // controlador do microfone
    private MicrophoneController microphoneController;

    public AqueraInputMicPlugin() {
        super();
        microphoneController = MicrophoneController.getInstance();
    }

    /**
     * New Flutter interface - post flutter 1.12
     * Attach plugin to flutter engine.
     *
     * @param flutterPluginBinding
     */
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "org.aquera:input_mic:methods");
        methodChannel.setMethodCallHandler(new AqueraInputMicMethodHandler(microphoneController));
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "org.aquera:input_mic:events");
        eventChannel.setStreamHandler(new AqueraInputMicStreamHandler(microphoneController));
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.

    /**
     * Old version interface. Still supported because of applications being created using old
     * versions of flutter.
     *
     * @param registrar
     */
    public static void registerWith(Registrar registrar) {
        final AqueraInputMicPlugin plugin = new AqueraInputMicPlugin();
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "org.aquera:input_mic:method");
        channel.setMethodCallHandler(new AqueraInputMicMethodHandler(plugin.microphoneController));
        final EventChannel evtChannel = new EventChannel(registrar.messenger(), "org.aquera:input_mic:events");
        evtChannel.setStreamHandler(new AqueraInputMicStreamHandler(plugin.microphoneController));
    }

    /**
     * Release resources when disconnected from flutter engine.
     * @param binding
     */
    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        microphoneController.stop();
        methodChannel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }
}
