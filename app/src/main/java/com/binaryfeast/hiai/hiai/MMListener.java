package com.binaryfeast.hiai.hiai;

import com.huawei.hiai.vision.visionkit.face.Face;

import java.util.List;

/**
 * Created by huarong on 2018/2/26.
 */
public interface MMListener {
    void onTaskCompleted(List<Face> faces);
}