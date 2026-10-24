package com.example.voicechanger.client;

import com.example.voicechanger.VoiceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/**
 * Settings screen of the voice changer.
 * <p>
 * Every effect has its own row: the name is shown on the left, the value is
 * shown inside the slider. The values are written directly into the shared
 * {@link VoiceConfig} instance, which is read by the audio thread
 * (see {@link com.example.voicechanger.audio.VoiceEffects}).
 */
public class VoiceChangerScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int LABEL_WIDTH = 104;
    private static final int ROW_HEIGHT = 22;
    private static final int GAP = 4;
    private static final int SECTION_HEIGHT = 15;

    private static final int COLOR_TITLE = 0xFFFFFFFF;
    private static final int COLOR_LABEL = 0xFFFFFFFF;
    private static final int COLOR_SECTION = 0xFFFFD166;
    private static final int COLOR_VALUE = 0xFFFFFFFF;
    private static final int COLOR_VALUE_DISABLED = 0xFF9E9E9E;
    private static final int COLOR_HINT = 0xFFB0B0B0;
    private static final int COLOR_PANEL = 0x88000000;
    private static final int COLOR_LINE = 0x60FFFFFF;
    private static final int COLOR_KNOB = 0xFF3F8CFF;
    private static final int COLOR_KNOB_DISABLED = 0xFF808080;

    private final Screen parent;
    private final VoiceConfig config = VoiceConfig.get();

    private final List<TextLine> texts = new ArrayList<>();
    private final List<int[]> dividers = new ArrayList<>();
    private final List<Slider> sliders = new ArrayList<>();

    private Button toggleButton;
    private int panelLeft;
    private int panelRight;
    private int panelTop;
    private int panelBottom;

    public VoiceChangerScreen(Screen parent) {
        super(Component.literal("ボイスチェンジャー"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        texts.clear();
        dividers.clear();
        sliders.clear();

        int contentHeight = 16 + (ROW_HEIGHT + GAP)
                + (SECTION_HEIGHT + ROW_HEIGHT + GAP) * 2 + SECTION_HEIGHT
                + (ROW_HEIGHT + GAP) * 4 + GAP + ROW_HEIGHT + 16;

        int x = this.width / 2 - PANEL_WIDTH / 2;
        int y = Math.max(8, (this.height - contentHeight) / 2);

        panelLeft = x - 8;
        panelRight = x + PANEL_WIDTH + 8;
        panelTop = y - 8;

        // Title
        texts.add(new TextLine(this.width / 2, y, Component.literal("ボイスチェンジャー設定"), COLOR_TITLE, true));
        y += 16;

        // Enable / disable
        toggleButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            config.enabled = !config.enabled;
            updateToggleMessage();
        }).bounds(x, y, PANEL_WIDTH, ROW_HEIGHT).build());
        updateToggleMessage();
        y += ROW_HEIGHT + GAP;

        // Voice
        y = addSection(x, y, "声質");
        y = addSlider(x, y, "ピッチ", "声の高さ（半音）", -24.0D, 24.0D, config.pitchSemitones,
                value -> String.format("%+.1f 半音 (x%.2f)", value, Math.pow(2.0D, value / 12.0D)),
                value -> config.pitchSemitones = value);
        y = addSlider(x, y, "音質", "クリアさ（低いとこもる）", 0.0D, 100.0D, config.tone,
                value -> String.format("%.0f%%", value),
                value -> config.tone = (int) Math.round(value));

        // Echo
        y = addSection(x, y, "エコー");
        y = addSlider(x, y, "エコー量", "響きの強さ", 0.0D, 100.0D, config.echoWet,
                value -> String.format("%.0f%%", value),
                value -> config.echoWet = (int) Math.round(value));
        y = addSlider(x, y, "エコー遅延", "やまびこの間隔", 0.0D, 1000.0D, config.echoDelayMs,
                value -> String.format("%.0f ms", value),
                value -> config.echoDelayMs = (int) Math.round(value));
        y = addSlider(x, y, "エコー反復", "繰り返しの回数", 0.0D, 90.0D, config.echoFeedback,
                value -> String.format("%.0f%%", value),
                value -> config.echoFeedback = (int) Math.round(value));

        // Output
        y = addSection(x, y, "出力");
        y = addSlider(x, y, "音量", "送信する音量", 50.0D, 200.0D, config.gain,
                value -> String.format("%.0f%%", value),
                value -> config.gain = (int) Math.round(value));

        // Buttons
        y += GAP;
        int buttonWidth = (PANEL_WIDTH - GAP) / 2;
        this.addRenderableWidget(Button.builder(Component.literal("リセット"), button -> {
            config.reset();
            this.rebuildWidgets();
        }).bounds(x, y, buttonWidth, ROW_HEIGHT).build());
        this.addRenderableWidget(Button.builder(Component.literal("完了"), button -> this.onClose())
                .bounds(x + buttonWidth + GAP, y, buttonWidth, ROW_HEIGHT).build());
        y += ROW_HEIGHT + 4;

        texts.add(new TextLine(this.width / 2, y, Component.literal("完了を押すと設定が保存されます"), COLOR_HINT, false));
        panelBottom = y + 12;
    }

    private int addSection(int x, int y, String name) {
        texts.add(new TextLine(x, y + 3, Component.literal(name), COLOR_SECTION, true));
        dividers.add(new int[]{x, y + 13, x + PANEL_WIDTH});
        return y + SECTION_HEIGHT;
    }

    private int addSlider(int x, int y, String label, String description, double min, double max, double value,
                          DoubleFunction<String> formatter, DoubleConsumer onChange) {
        texts.add(new TextLine(x, y + (ROW_HEIGHT - 8) / 2, Component.literal(label), COLOR_LABEL, false));
        Slider slider = new Slider(x + LABEL_WIDTH, y, PANEL_WIDTH - LABEL_WIDTH, ROW_HEIGHT, min, max, value, formatter, onChange);
        slider.setTooltip(Tooltip.create(Component.literal(description)));
        this.addRenderableWidget(slider);
        sliders.add(slider);
        return y + ROW_HEIGHT + GAP;
    }

    private void updateToggleMessage() {
        if (toggleButton != null) {
            toggleButton.setMessage(Component.literal(config.enabled
                    ? "ボイスチェンジャー: ON"
                    : "ボイスチェンジャー: OFF（声はそのまま）"));
        }
        for (Slider slider : sliders) {
            slider.refresh();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, COLOR_PANEL);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, COLOR_LINE);
        graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, COLOR_LINE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        for (int[] divider : dividers) {
            graphics.fill(divider[0], divider[1], divider[2], divider[1] + 1, COLOR_LINE);
        }
        for (TextLine line : texts) {
            Component text = line.text();
            int textX = line.centered() ? line.x() - this.font.width(text) / 2 : line.x();
            graphics.text(this.font, text, textX, line.y(), line.color(), true);
        }
    }

    @Override
    public void onClose() {
        VoiceConfig.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    private record TextLine(int x, int y, Component text, int color, boolean centered) {
    }

    /**
     * A simple slider widget that shows its current value.
     */
    private static class Slider extends AbstractWidget {

        private final double min;
        private final double max;
        private final DoubleFunction<String> formatter;
        private final DoubleConsumer onChange;

        private double value;

        Slider(int x, int y, int width, int height, double min, double max, double value,
               DoubleFunction<String> formatter, DoubleConsumer onChange) {
            super(x, y, width, height, Component.empty());
            this.min = min;
            this.max = max;
            this.value = clamp(value);
            this.formatter = formatter;
            this.onChange = onChange;
            updateMessage();
        }

        void refresh() {
            updateMessage();
        }

        private double clamp(double v) {
            return Math.max(min, Math.min(max, v));
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            setValueFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            setValueFromMouse(event.x());
        }

        private void setValueFromMouse(double mouseX) {
            double ratio = (mouseX - (this.getX() + 4.0D)) / (this.getWidth() - 8.0D);
            ratio = Math.max(0.0D, Math.min(1.0D, ratio));
            double newValue = clamp(min + ratio * (max - min));
            if (newValue != value) {
                value = newValue;
                onChange.accept(value);
                updateMessage();
            }
        }

        private void updateMessage() {
            this.setMessage(Component.literal(formatter.apply(value)));
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();

            boolean enabled = VoiceConfig.get().enabled;

            int background = this.isHovered() ? 0xFF454545 : 0xFF242424;
            graphics.fill(x, y, x + width, y + height, background);
            graphics.fill(x, y, x + width, y + 1, 0xFF000000);
            graphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000);

            double ratio = (value - min) / (max - min);
            int knobX = x + 4 + (int) Math.round(ratio * (width - 8));
            graphics.fill(knobX - 2, y + 2, knobX + 3, y + height - 2, enabled ? COLOR_KNOB : COLOR_KNOB_DISABLED);

            Component message = this.getMessage();
            Minecraft minecraft = Minecraft.getInstance();
            graphics.text(minecraft.font, message, x + width / 2 - minecraft.font.width(message) / 2,
                    y + (height - 8) / 2, enabled ? COLOR_VALUE : COLOR_VALUE_DISABLED, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            this.defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
