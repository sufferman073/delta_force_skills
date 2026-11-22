package com.rzy.dealt_force_skills.client.screen;

import mccfk.DeltaForce.java.client.DFClientConfig;
import mccfk.DeltaForce.java.client.screen.UiCanvasScaleMath;
import net.minecraft.client.Minecraft;

record DepartmentCanvas(int width, int height, float scale, int originX, int originY) {
    static DepartmentCanvas fit(int actualWidth, int actualHeight) {
        double factor = DFClientConfig.getInstance().resolveUiScale(Minecraft.getInstance().getWindow().getGuiScale());
        int width = UiCanvasScaleMath.canvasSize(UiCanvasScaleMath.REFERENCE_WIDTH, factor);
        int height = UiCanvasScaleMath.canvasSize(UiCanvasScaleMath.REFERENCE_HEIGHT, factor);
        float scale = UiCanvasScaleMath.fitScale(actualWidth, actualHeight, width, height);
        return new DepartmentCanvas(width, height, scale, UiCanvasScaleMath.canvasOffset(actualWidth, width, scale),
                UiCanvasScaleMath.canvasOffset(actualHeight, height, scale));
    }
    double localX(double screenX) { return (screenX - originX) / scale; }
    double localY(double screenY) { return (screenY - originY) / scale; }
}
