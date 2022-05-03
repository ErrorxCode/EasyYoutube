package com.xcoder.easyyt;

public interface ProgressListener {
    void onProgress(int progress);
    void onError(Exception e);
}
