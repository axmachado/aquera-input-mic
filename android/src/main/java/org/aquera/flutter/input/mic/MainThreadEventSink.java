package org.aquera.flutter.input.mic;

import android.os.Handler;
import android.os.Looper;

import io.flutter.plugin.common.EventChannel;

public class MainThreadEventSink implements EventChannel.EventSink {

    private EventChannel.EventSink eventSink;
    private Handler handler;

    public MainThreadEventSink(EventChannel.EventSink eventSink) {
        this.eventSink = eventSink;
        handler = new Handler(Looper.getMainLooper());

    }

    @Override
    public void success(final Object event) {
        handler.post(new Runnable() {
            public void run() {
                eventSink.success(event);
            }
        });
    }

    @Override
    public void error(final String errorCode, final String errorMessage, final Object errorDetails) {
        handler.post(new Runnable() {
            public void run() {
                eventSink.error(errorCode, errorMessage, errorDetails);
            }
        });
    }

    @Override
    public void endOfStream() {
        handler.post(new Runnable() {
            public void run() {
                eventSink.endOfStream();
            }
        });
    }
}
