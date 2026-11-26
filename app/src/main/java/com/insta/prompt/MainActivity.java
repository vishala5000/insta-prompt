package com.insta.prompt;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    /** Your ten layer files, in the exact order they are joined into the prompt. */
    static final String[] FILES = {
        "BACKGROUND.txt", "CAMERA ANGLE.txt", "FACE EXPRESSION.txt", "HAIRSTYLE.txt",
        "HALF BODY POSE.txt", "LIGHTING.txt", "MOOD.txt", "OUTFIT.txt",
        "PARIS LOCATION.txt", "SETTING.txt"
    };

    /** The fixed ending. Nothing is appended after this. */
    static final String ENDING = "An extremely unremarkable, casual iPhone snapshot of [SUBJECT/SCENE]. "
        + "Careless framing, accidental composition, mild overexposure, and natural uneven indoor lighting. "
        + "Looks like an unedited camera roll photo.";

    private final LinkedHashMap<String, ArrayList<String>> layers = new LinkedHashMap<>();
    private final HashMap<String, String> last = new HashMap<>();
    private final Random rng = new Random();

    private SharedPreferences prefs;
    private int copies, totalLines;
    private boolean soundOn = true;
    private SoundPool pool;
    private int popId;
    private boolean popReady = false;
    private ValueAnimator breath;
    private Runnable hideBadge, hidePill;

    private View header, statsRow, hero, card, btnWrap, btnCopy, btnSound, orbit, pulse1, pulse2;
    private ImageView imgSound;
    private TextView chipCombos, chipCount, tvStatus, tvCopied, tvPrompt, pill, footer;
    private LinearLayout chipsRow;
    private BurstView burst;

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("insta", MODE_PRIVATE);
        copies = prefs.getInt("copies", 0);
        soundOn = prefs.getBoolean("sound", true);

        header = findViewById(R.id.header);
        statsRow = findViewById(R.id.statsRow);
        hero = findViewById(R.id.hero);
        card = findViewById(R.id.card);
        footer = findViewById(R.id.footer);
        btnWrap = findViewById(R.id.btnWrap);
        btnCopy = findViewById(R.id.btnCopy);
        btnSound = findViewById(R.id.btnSound);
        imgSound = findViewById(R.id.imgSound);
        orbit = findViewById(R.id.orbit);
        pulse1 = findViewById(R.id.pulse1);
        pulse2 = findViewById(R.id.pulse2);
        chipCombos = findViewById(R.id.chipCombos);
        chipCount = findViewById(R.id.chipCount);
        tvStatus = findViewById(R.id.tvStatus);
        tvCopied = findViewById(R.id.tvCopied);
        tvPrompt = findViewById(R.id.tvPrompt);
        chipsRow = findViewById(R.id.chipsRow);
        pill = findViewById(R.id.pill);
        burst = findViewById(R.id.burst);

        loadLayers();
        chipCombos.setText(combosLabel() + " COMBOS");
        chipCount.setText(copies + " COPIED");
        imgSound.setImageResource(soundOn ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        btnSound.setAlpha(soundOn ? 1f : 0.55f);
        pill.setTranslationY(dp(90));

        hideBadge = () -> tvCopied.animate().alpha(0f).setDuration(240)
                .withEndAction(() -> tvCopied.setVisibility(View.INVISIBLE)).start();
        hidePill = () -> pill.animate().translationY(dp(90)).alpha(0f).setDuration(280).start();

        setupSound();
        btnCopy.setOnClickListener(v -> copyPrompt());
        btnSound.setOnClickListener(v -> toggleSound());

        ArrayList<String> seed = roll();
        renderPrompt(seed);
        renderChips(seed, false);
        tvStatus.setText(totalLines + " lines ready \u00B7 tap COPY PROMPT to shuffle");

        entrance();
        startIdleMotion();
    }

    /** Preload every line once → each tap is pure memory lookup, zero I/O. */
    private void loadLayers() {
        totalLines = 0;
        for (String f : FILES) {
            ArrayList<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(getAssets().open(f), StandardCharsets.UTF_8))) {
                String l;
                while ((l = br.readLine()) != null) {
                    l = l.replace("\uFEFF", "").trim();
                    if (!l.isEmpty()) lines.add(l);
                }
            } catch (Exception e) {
                lines.add(f.replace(".txt", "").toLowerCase(Locale.ROOT));
            }
            if (lines.isEmpty()) lines.add(f.replace(".txt", "").toLowerCase(Locale.ROOT));
            totalLines += lines.size();
            layers.put(f, lines);
        }
    }

    private String combosLabel() {
        double c = 1;
        for (ArrayList<String> l : layers.values()) c *= l.size();
        String[] suf = {"", "K", "M", "B", "T", "Qa"};
        int i = 0;
        while (c >= 1000 && i < suf.length - 1) { c /= 1000; i++; }
        String num = c >= 100 ? String.format(Locale.US, "%.0f", c)
                              : String.format(Locale.US, "%.1f", c);
        return num + suf[i];
    }

    /** One random line per file, never repeating the previous pick. Instant. */
    private ArrayList<String> roll() {
        ArrayList<String> picks = new ArrayList<>();
        for (String f : FILES) {
            ArrayList<String> lines = layers.get(f);
            String prev = last.get(f);
            String v = lines.get(rng.nextInt(lines.size()));
            int guard = 0;
            while (lines.size() > 1 && v.equals(prev) && guard++ < 6) {
                v = lines.get(rng.nextInt(lines.size()));
            }
            last.put(f, v);
            picks.add(v);
        }
        return picks;
    }

    private void copyPrompt() {
        ArrayList<String> picks = roll();
        String joined = TextUtils.join(", ", picks);
        String full = joined + ", " + ENDING;

        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("INSTA PROMPT", full));

        copies++;
        prefs.edit().putInt("copies", copies).apply();

        renderPrompt(picks);
        renderChips(picks, true);
        chipCount.setText(copies + " COPIED");
        pop(chipCount);
        tvStatus.setText("Prompt #" + copies + " \u00B7 " + full.length()
                + " chars \u00B7 on your clipboard");
        showBadge();
        showPill("\u2713  Copied — paste it anywhere");
        pulseRings();
        burst.burst(burst.getWidth() / 2f, burst.getHeight() / 2f);
        bounceButton();
        haptic();
        playPop();
    }

    private void renderPrompt(ArrayList<String> picks) {
        String joined = TextUtils.join(", ", picks);
        String full = joined + ", " + ENDING;
        SpannableStringBuilder ssb = new SpannableStringBuilder(full);
        int a = joined.length() + 2;
        ssb.setSpan(new ForegroundColorSpan(0xFFD9A8AE), a, full.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvPrompt.setText(ssb);
        tvPrompt.setAlpha(0.25f);
        tvPrompt.animate().alpha(1f).setDuration(240).start();
    }

    private void renderChips(ArrayList<String> picks, boolean animate) {
        chipsRow.removeAllViews();
        int[] labelColors = {0xFFFF8A94, 0xFFFFB03A, 0xFFFF6B7A, 0xFFF4A0B5};
        for (int i = 0; i < FILES.length; i++) {
            LinearLayout chip = new LinearLayout(this);
            chip.setOrientation(LinearLayout.VERTICAL);
            chip.setBackgroundResource(R.drawable.chip_layer);
            chip.setPadding(dp(10), dp(7), dp(10), dp(7));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(8);
            chip.setLayoutParams(lp);

            TextView cat = new TextView(this);
            cat.setText(FILES[i].replace(".txt", ""));
            cat.setTextSize(9);
            cat.setLetterSpacing(0.18f);
            cat.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            cat.setTextColor(labelColors[i % labelColors.length]);

            TextView val = new TextView(this);
            val.setText(picks.get(i));
            val.setTextSize(12);
            val.setTextColor(0xFFF4E9EA);
            val.setMaxWidth(dp(170));
            val.setSingleLine(true);
            val.setEllipsize(TextUtils.TruncateAt.END);

            chip.addView(cat);
            chip.addView(val);
            if (animate) {
                chip.setAlpha(0f);
                chip.setTranslationX(dp(18));
                chip.animate().alpha(1f).translationX(0f)
                        .setStartDelay(i * 40L).setDuration(240).start();
            }
            chipsRow.addView(chip);
        }
    }

    private void pulseRings() {
        View[] rings = {pulse1, pulse2};
        for (int i = 0; i < rings.length; i++) {
            View r = rings[i];
            r.setScaleX(1f); r.setScaleY(1f); r.setAlpha(0.85f);
            r.animate().scaleX(1.36f).scaleY(1.36f).alpha(0f)
                    .setStartDelay(i * 130L).setDuration(620)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void bounceButton() {
        btnCopy.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70)
                .withEndAction(() -> btnCopy.animate().scaleX(1f).scaleY(1f)
                        .setDuration(340).setInterpolator(new OvershootInterpolator(3.2f)).start())
                .start();
    }

    private void pop(View v) {
        v.animate().scaleX(1.16f).scaleY(1.16f).setDuration(90)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                .start();
    }

    private void showBadge() {
        tvCopied.setVisibility(View.VISIBLE);
        tvCopied.setAlpha(0f);
        tvCopied.animate().alpha(1f).setDuration(180).start();
        tvCopied.removeCallbacks(hideBadge);
        tvCopied.postDelayed(hideBadge, 2200);
    }

    private void showPill(String msg) {
        pill.setText(msg);
        pill.removeCallbacks(hidePill);
        pill.animate().translationY(0f).alpha(1f).setDuration(260)
                .setInterpolator(new DecelerateInterpolator()).start();
        pill.postDelayed(hidePill, 1800);
    }

    private void haptic() {
        btnCopy.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(22, 120));
                } else {
                    v.vibrate(22);
                }
            }
        } catch (Exception ignored) { }
    }

    private void setupSound() {
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(aa).build();
        popId = pool.load(this, R.raw.pop, 1);
        pool.setOnLoadCompleteListener((p, id, status) -> {
            if (id == popId && status == 0) popReady = true;
        });
    }

    private void playPop() {
        if (soundOn && popReady && pool != null) {
            pool.play(popId, 0.55f, 0.55f, 1, 0, 1f);
        }
    }

    private void toggleSound() {
        soundOn = !soundOn;
        prefs.edit().putBoolean("sound", soundOn).apply();
        imgSound.setImageResource(soundOn ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        btnSound.setAlpha(soundOn ? 1f : 0.55f);
        btnSound.setContentDescription(soundOn ? "Sound on — tap to mute" : "Sound muted — tap to unmute");
        pop(btnSound);
        if (soundOn) playPop();
        showPill(soundOn ? "Sound on" : "Sound muted");
    }

    private void entrance() {
        View[] staged = {header, statsRow, hero, tvStatus, card, footer};
        float[] ty = {-20, -14, 0, 10, 26, 10};
        long[] delay = {60, 140, 200, 330, 380, 470};
        for (int i = 0; i < staged.length; i++) {
            View v = staged[i];
            v.setAlpha(0f);
            v.setTranslationY(dp(ty[i]));
            if (v == hero) { v.setScaleX(0.86f); v.setScaleY(0.86f); }
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(delay[i]).setDuration(420)
                    .setInterpolator(v == hero ? new OvershootInterpolator(1.4f)
                                               : new DecelerateInterpolator())
                    .start();
        }
    }

    private void startIdleMotion() {
        breath = ValueAnimator.ofFloat(1f, 1.035f);
        breath.setDuration(1700);
        breath.setRepeatCount(ValueAnimator.INFINITE);
        breath.setRepeatMode(ValueAnimator.REVERSE);
        breath.setInterpolator(new AccelerateDecelerateInterpolator());
        breath.addUpdateListener(a -> {
            float s = (float) a.getAnimatedValue();
            btnWrap.setScaleX(s);
            btnWrap.setScaleY(s);
        });
        breath.setStartDelay(900);
        breath.start();

        RotateAnimation rot = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rot.setDuration(26000);
        rot.setRepeatCount(Animation.INFINITE);
        rot.setInterpolator(new LinearInterpolator());
        orbit.startAnimation(rot);
    }

    @Override protected void onPause() {
        super.onPause();
        if (breath != null) breath.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (breath != null && breath.isPaused()) breath.resume();
    }

    @Override protected void onDestroy() {
        if (breath != null) { breath.cancel(); breath = null; }
        if (pool != null) { pool.release(); pool = null; }
        if (orbit != null) orbit.clearAnimation();
        if (tvCopied != null) tvCopied.removeCallbacks(hideBadge);
        if (pill != null) pill.removeCallbacks(hidePill);
        super.onDestroy();
    }
}
