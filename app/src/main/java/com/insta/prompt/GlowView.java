package com.insta.prompt;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

/** Living background: drifting crimson glows + rising ember particles. */
public class GlowView extends View {
    private final Paint pa = new Paint(), pb = new Paint(), pc = new Paint();
    private final Paint pd = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix ma = new Matrix(), mb = new Matrix(), mc = new Matrix();
    private RadialGradient ga, gb, gc;
    private float[] ex, ey, es, ep, er;
    private int[] ec;
    private int w, h;
    private long t0;
    private boolean attached = false, visible = true;

    public GlowView(Context c, AttributeSet a) { super(c, a); }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        this.w = w; this.h = h;
        float r = Math.max(w, h) * 0.85f;
        ga = new RadialGradient(0, 0, r, new int[]{0x59FF2740, 0x208F0E22, 0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        gb = new RadialGradient(0, 0, r, new int[]{0x4DC8102E, 0x1A5E0A1A, 0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        gc = new RadialGradient(0, 0, r * 0.6f, new int[]{0x26FF7A3C, 0x0F8F2E1A, 0x00000000},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        pa.setShader(ga); pb.setShader(gb); pc.setShader(gc);
        int n = 34;
        ex = new float[n]; ey = new float[n]; es = new float[n];
        ep = new float[n]; er = new float[n]; ec = new int[n];
        Random rnd = new Random(7);
        int[] cols = {0xFFFF6B7A, 0xFFFFB03A, 0xFFFFFFFF, 0xFFFF2D40};
        for (int i = 0; i < n; i++) {
            ex[i] = rnd.nextFloat(); ey[i] = rnd.nextFloat();
            es[i] = 8 + rnd.nextFloat() * 22; ep[i] = rnd.nextFloat() * 6.283f;
            er[i] = 1 + rnd.nextFloat() * 2.2f; ec[i] = cols[i % 4];
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true; t0 = System.nanoTime();
        postInvalidateOnAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        attached = false;
        super.onDetachedFromWindow();
    }

    @Override protected void onWindowVisibilityChanged(int v) {
        super.onWindowVisibilityChanged(v);
        visible = (v == VISIBLE);
        if (visible && attached) postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        if (w == 0 || h == 0) return;
        float t = (System.nanoTime() - t0) / 1000000000f;
        c.drawColor(0xFF0A0406);
        ma.setTranslate(w * (0.16f + 0.10f * (float) Math.sin(t * 0.31f)),
                        h * (0.14f + 0.05f * (float) Math.cos(t * 0.23f)));
        ga.setLocalMatrix(ma); c.drawRect(0, 0, w, h, pa);
        mb.setTranslate(w * (0.88f + 0.08f * (float) Math.cos(t * 0.27f)),
                        h * (0.90f + 0.05f * (float) Math.sin(t * 0.19f)));
        gb.setLocalMatrix(mb); c.drawRect(0, 0, w, h, pb);
        mc.setTranslate(w * (0.5f + 0.2f * (float) Math.sin(t * 0.16f + 2f)), h * 1.02f);
        gc.setLocalMatrix(mc); c.drawRect(0, 0, w, h, pc);
        float span = h + 60;
        for (int i = 0; i < ex.length; i++) {
            float y = h + 30 - ((ey[i] * span + t * es[i]) % span);
            float x = ex[i] * w + 6f * (float) Math.sin(t * 0.5f + ep[i]);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 1.7f + ep[i]);
            pd.setColor(ec[i]);
            pd.setAlpha((int) (30 + 70 * tw));
            c.drawCircle(x, y, er[i], pd);
        }
        if (attached && visible) postInvalidateOnAnimation();
    }
}
