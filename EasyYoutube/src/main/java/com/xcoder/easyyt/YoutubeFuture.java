package com.xcoder.easyyt;

import java.util.concurrent.Phaser;

public class YoutubeFuture<T> {
    protected T result;
    protected Phaser phaser = new Phaser(1);
    protected YoutubeException exception;
    protected OnSuccessListener<T> successListener = result -> {};
    protected OnCompleteListener<T> completeListener = YoutubeFuture -> {};
    protected OnFailureListener failureListener = exception1 -> {};


    public YoutubeFuture<T> addOnSuccessListener(OnSuccessListener<T> listener){
        successListener = listener;
        return this;
    }

    public YoutubeFuture<T> addOnFailureListener(OnFailureListener listener){
        failureListener = listener;
        return this;
    }

    public void addOnCompleteListener(OnCompleteListener<T> listener){
        completeListener = listener;
    }

    public T getResult(){
        return result;
    }

    public YoutubeException getException(){
        return exception;
    }

    public boolean isSuccessful(){
        return exception == null;
    }

    private T get() throws YoutubeException {
        phaser.arriveAndAwaitAdvance();
        if (exception == null)
            return result;
        else
            throw exception;
    }


    public interface OnSuccessListener<T> {
        void onSuccess(T result);
    }


    public interface OnFailureListener {
        void onFailed(YoutubeException exception);
    }


    public interface OnCompleteListener<T> {
        void onComplete(YoutubeFuture<T> YoutubeFuture);
    }
}


