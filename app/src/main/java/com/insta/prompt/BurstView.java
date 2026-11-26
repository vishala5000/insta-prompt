package com.insta.prompt;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

/** Spark burst fired from the COPY PROMPT button on every copy. */
public class BurstView extends View {
    private static final int[] COLORS = {0xFFFF3B4E, 0xFFFFFFFF, 0xFFFFB03A, 0xFFFF7A8A};

    private static class P {
        float x, y, vx, vy, life, max, size;
        int color;
    }

    private final ArrayList<P> ps = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private ValueAnimator anim;
    private long last;

    public BurstView(Context c, AttributeSet a) { super(c, a); }

    public void burst(float cx, float cy) {
        ps.clear();
        for (int i = 0; i < 26; i++) {
            P p = new P();
            double ang = rnd.nextDouble() * Math.PI * 2;
            float sp = 140 + rnd.nextFloat() * 330;
            p.x = cx; p.y = cy;
            p.vx = (float) Math.cos(ang) * sp;
            p.vy = (float) Math.sin(ang) * sp - 130;
            p.max = 0.5f + rnd.nextFloat() * 0.5f;
            p.life = p.max;
            p.size = 2.5f + rnd.nextFloat() * 4.5f;
            p.color = COLORS[i % COLORS.length];
            ps.add(p);
        }
        if (anim != null) anim.cancel();
        last = System.nanoTime();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1100);
        anim.addUpdateListener(a -> { step(); invalidate(); });
        anim.start();
    }

    private void step() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1000000000f);
        last = now;
        for (int i = ps.size() - 1; i >= 0; i--) {
            P p = ps.get(i);
            p.vy += 980 * dt;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.life -= dt;
            if (p.life <= 0) ps.remove(i);
        }
    }

    @Override protected void onDraw(Canvas c) {
        for (P p : ps) {
            float k = Math.max(0f, p.life / p.max);
            paint.setColor(p.color);
            paint.setAlpha((int) (255 * k));
            c.drawCircle(p.x, p.y, p.size * (0.5f + 0.5f * k), paint);
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (anim != null) { anim.cancel(); anim = null; }
        ps.clear();
        super.onDetachedFromWindow();
    }
}
