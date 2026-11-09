package com.etherealnymph.dim.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/** 静态的极光背景：两层径向光晕 + 顶部压暗，不做逐帧动画以省电。 */
public class AuroraBackdropView extends View {

    private final Paint background = new Paint();
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shade = new Paint();
    private final RectF rect = new RectF();

    private Shader backgroundShader;
    private Shader mintShader;
    private Shader indigoShader;
    private Shader topShade;
    private Shader centerShader;

    public AuroraBackdropView(Context context) {
        this(context, null);
    }

    public AuroraBackdropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AuroraBackdropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        backgroundShader = new LinearGradient(
                0f, 0f, 0f, h,
                0xFF06070B, 0xFF0C121C,
                Shader.TileMode.CLAMP);
        background.setShader(backgroundShader);

        mintShader = new RadialGradient(
                w * 0.86f, h * 0.10f, Math.max(w, h) * 0.72f,
                new int[]{0x2E74E8C8, 0x1274E8C8, 0x0074E8C8},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP);

        indigoShader = new RadialGradient(
                w * 0.10f, h * 0.92f, Math.max(w, h) * 0.62f,
                new int[]{0x2A3557C8, 0x0E3557C8, 0x003557C8},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);

        topShade = new LinearGradient(
                0f, 0f, 0f, Ui.dp(getContext(), 180),
                0x40000000, 0x00000000,
                Shader.TileMode.CLAMP);
        shade.setShader(topShade);
        centerShader = new RadialGradient(
                w / 2f, h * 0.36f, w * 0.62f,
                new int[]{0x0AFFFFFF, 0x00FFFFFF},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        rect.set(0f, 0f, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (backgroundShader == null) {
            return;
        }
        canvas.drawRect(rect, background);

        glow.setShader(mintShader);
        canvas.drawRect(rect, glow);
        glow.setShader(indigoShader);
        canvas.drawRect(rect, glow);

        canvas.drawRect(rect, shade);

        // 极淡的中心提亮，让锁屏圆盘从背景里浮起来。
        glow.setShader(centerShader);
        canvas.drawRect(rect, glow);
    }
}
