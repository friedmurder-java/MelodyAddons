package com.skyblockmod.client.gui;

import com.skyblockmod.client.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModMenuScreen extends Screen {

    private static final int BG_COLOR      = 0xCC0D0D0D;
    private static final int PANEL_COLOR   = 0xFF1A1A2E;
    private static final int ACCENT        = 0xFF4A90D9;
    private static final int TAB_COLOR     = 0xFF12122A;
    private static final int TAB_ACTIVE    = 0xFF1E1E3A;
    private static final int TEXT_COLOR    = 0xFFEEEEEE;
    private static final int SUBTEXT_COLOR = 0xFF888888;
    private static final int FIELD_BG      = 0xFF0A0A1A;
    private static final int FIELD_BORDER  = 0xFF334466;
    private static final int FIELD_ACTIVE  = 0xFF4A90D9;

    private static final int TAB_W     = 90;
    private static final int TAB_H     = 36;
    private static final int CONTENT_W = 380;
    private static final int TOGGLE_H  = 44;
    private static final int TEXT_H    = 58;
    private static final int POOL_H    = 30;
    private static final int SECTION_H = 32;
    private static final int TOGGLE_SW = 40;
    private static final int TOGGLE_SH = 20;
    private static final int PADDING   = 14;
    private static final int HEADER_H  = 46;
    private static final int FOOTER_H  = 20;
    private static final int HUD_BTN_W = 20;

    private static final String[] TABS = {"Misc", "Melody"};

    private sealed interface Row permits ToggleRow, HudToggleRow, TextRow, PoolHeaderRow, PoolItemRow {}
    private record ToggleRow(String label, String description, Supplier<Boolean> getter, Consumer<Boolean> setter) implements Row {}
    private record HudToggleRow(String label, String description, String hudId, Supplier<Boolean> getter, Consumer<Boolean> setter) implements Row {}
    private record TextRow(String label, String description, String placeholder, Supplier<String> getter, Consumer<String> setter) implements Row {}
    private record PoolHeaderRow() implements Row {}
    private record PoolItemRow(int index) implements Row {}

    private final List<Row> rows = new ArrayList<>();
    private final List<TextFieldWidget> fields = new ArrayList<>();
    private final Map<String, Float> animStates = new HashMap<>();

    private int panelX, panelY, panelH, panelW;
    private int contentX;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int activeTab = 0;
    private boolean poolExpanded = false;

    public ModMenuScreen() {
        super(Text.literal("MelodyAddons"));
    }

    @Override
    protected void init() {
        rows.clear();
        fields.forEach(this::remove);
        fields.clear();
        buildRows();

        panelW = TAB_W + CONTENT_W;
        int contentH = HEADER_H + FOOTER_H;
        for (Row r : rows) contentH += rowHeight(r);

        panelH = Math.min(contentH, height - 20);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentX = panelX + TAB_W;
        maxScroll = Math.max(0, contentH - panelH);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        rebuildTextFields();
    }

    private int rowHeight(Row r) {
        if (r instanceof TextRow)       return TEXT_H;
        if (r instanceof PoolItemRow)   return POOL_H;
        if (r instanceof PoolHeaderRow) return SECTION_H;
        return TOGGLE_H;
    }

    private void buildRows() {
        ModConfig cfg = ModConfig.get();

        if (activeTab == 0) {
            addHudToggle("Height HUD", "Shows your current Y position on screen", "height",
                    () -> cfg.heightHud, v -> { cfg.heightHud = v; cfg.save(); });
            addToggle("Rarity Glow", "Outline dropped items with rarity colour",
                    () -> cfg.rarityGlow, v -> { cfg.rarityGlow = v; cfg.save(); });
            addToggle("Slayer RNG Tracker", "Show XP reset and drop % on slayer drops",
                    () -> cfg.slayerRngTracker, v -> { cfg.slayerRngTracker = v; cfg.save(); });
            addToggle("Dungeon RNG Tracker", "Show drop % on dungeon drops",
                    () -> cfg.dungeonRngTracker, v -> { cfg.dungeonRngTracker = v; cfg.save(); });
            addToggle("Blessed (+10%)", "Include Blessed attribute bonus in RNG XP calculation",
                    () -> cfg.dungeonRngBlessed, v -> { cfg.dungeonRngBlessed = v; cfg.save(); });
            addToggle("Cocoon Alert", "Big COCOONED! text when you cocoon a slayer boss",
                    () -> cfg.cocoonAlert, v -> { cfg.cocoonAlert = v; cfg.save(); });
            addToggle("Fullbright", "Maximum brightness",
                    () -> cfg.fullbright, v -> {
                        cfg.fullbright = v;
                        if (!v) MinecraftClient.getInstance().options.getGamma().setValue(1.0);
                        cfg.save();
                    });
        } else {
            addToggle("Enable Melody Messages", "Send /pc messages during the terminal",
                    () -> cfg.melodyMessages, v -> { cfg.melodyMessages = v; cfg.save(); });
            addTextField("Start Message", "Sent to /pc when you open the terminal",
                    "e.g. doing melody!", () -> cfg.melodyMsgStart, v -> { cfg.melodyMsgStart = v; cfg.save(); });
            addTextField("Close Message", "Sent to /pc when terminal finishes",
                    "e.g. melody done!", () -> cfg.melodyMsgClose, v -> { cfg.melodyMsgClose = v; cfg.save(); });
            rows.add(new PoolHeaderRow());
            if (poolExpanded) {
                for (int i = 0; i < cfg.melodyMsgPool.size(); i++) {
                    rows.add(new PoolItemRow(i));
                }
            }
            addToggle("Leap Announce", "Send /pc message when you leap to a player",
                    () -> cfg.leapAnnounce, v -> { cfg.leapAnnounce = v; cfg.save(); });
            addTextField("Leap Message", "Use {player} for the player name",
                    "e.g. Leaped to {player}!", () -> cfg.leapAnnounceMsg, v -> { cfg.leapAnnounceMsg = v; cfg.save(); });
            addToggle("Berserker Tracker", "Send /pc messages for Berserker in F7/M7 Phase 3",
                    () -> cfg.berserkerTracker, v -> { cfg.berserkerTracker = v; cfg.save(); });
            addTextField("P3 Device Done Msg", "Sent when berserk completes device. Use (bers) for name",
                    "e.g. (bers) did device!", () -> cfg.berserkerMsgStart, v -> { cfg.berserkerMsgStart = v; cfg.save(); });
            addTextField("S1 End Not Done Msg", "Sent at S1 end if berserk didnt do device. Use (bers)",
                    "e.g. (bers) missed device!", () -> cfg.berserkerMsgNotDone, v -> { cfg.berserkerMsgNotDone = v; cfg.save(); });
            addTextField("S4 Start Msg", "Sent when S4 starts if berserk never did device. Use (bers)",
                    "e.g. (bers) still hasnt done device!", () -> cfg.berserkerMsgS4, v -> { cfg.berserkerMsgS4 = v; cfg.save(); });
        }
    }

    private void addToggle(String label, String desc, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        rows.add(new ToggleRow(label, desc, getter, setter));
    }

    private void addHudToggle(String label, String desc, String hudId, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        rows.add(new HudToggleRow(label, desc, hudId, getter, setter));
    }

    private void addTextField(String label, String desc, String placeholder, Supplier<String> getter, Consumer<String> setter) {
        rows.add(new TextRow(label, desc, placeholder, getter, setter));
    }

    private void rebuildTextFields() {
        fields.forEach(this::remove);
        fields.clear();
        int y = panelY + HEADER_H;
        ModConfig cfg = ModConfig.get();

        for (Row row : rows) {
            if (row instanceof TextRow tr) {
                TextFieldWidget field = new TextFieldWidget(
                        textRenderer, contentX + PADDING + 2, y + TEXT_H - 24 - scrollOffset,
                        CONTENT_W - PADDING * 2 - 4, 16, Text.literal(tr.label()));
                field.setMaxLength(128);
                field.setText(tr.getter().get());
                field.setPlaceholder(Text.literal("§8" + tr.placeholder()));
                field.setChangedListener(tr.setter()::accept);
                field.setDrawsBackground(true);
                addDrawableChild(field);
                fields.add(field);
            } else if (row instanceof PoolItemRow pr) {
                String val = pr.index() < cfg.melodyMsgPool.size() ? cfg.melodyMsgPool.get(pr.index()) : "";
                TextFieldWidget field = new TextFieldWidget(
                        textRenderer, contentX + PADDING + 2, y + POOL_H - 20 - scrollOffset,
                        CONTENT_W - PADDING * 2 - 32, 16, Text.literal("msg" + pr.index()));
                field.setMaxLength(128);
                field.setText(val);
                field.setPlaceholder(Text.literal("§8Enter message..."));
                int idx = pr.index();
                field.setChangedListener(text -> {
                    while (cfg.melodyMsgPool.size() <= idx) cfg.melodyMsgPool.add("");
                    cfg.melodyMsgPool.set(idx, text);
                    cfg.save();
                });
                field.setDrawsBackground(true);
                addDrawableChild(field);
                fields.add(field);
            }
            y += rowHeight(row);
        }
    }

    private float getAnim(String key, boolean target) {
        float current = animStates.getOrDefault(key, target ? 1f : 0f);
        float speed = 0.12f;
        float next = target ? Math.min(1f, current + speed) : Math.max(0f, current - speed);
        animStates.put(key, next);
        return next;
    }

    private static int lerpColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r  = (int)(ar + (br - ar) * t);
        int g  = (int)(ag + (bg - ag) * t);
        int bl = (int)(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int lerpAlpha(int color, float alpha) {
        int a = (int)(alpha * 255) & 0xFF;
        return (color & 0x00FFFFFF) | (a << 24);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, BG_COLOR);
        ctx.fill(panelX, panelY, panelX + TAB_W, panelY + panelH, 0xFF0F0F20);
        ctx.fill(contentX, panelY, contentX + CONTENT_W, panelY + panelH, PANEL_COLOR);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 3, ACCENT);

        for (int i = 0; i < TABS.length; i++) {
            renderTab(ctx, i, mouseX, mouseY);
        }

        ctx.fill(contentX, panelY + 3, contentX + 1, panelY + panelH, 0xFF333355);

        ctx.enableScissor(contentX, panelY + 3, contentX + CONTENT_W, panelY + panelH - FOOTER_H);
        renderContentHeader(ctx);

        int y = panelY + HEADER_H;
        int fi = 0;

        for (Row row : rows) {
            int rowH = rowHeight(row);
            int sy = y - scrollOffset;

            if (sy + rowH > panelY + 3 && sy < panelY + panelH - FOOTER_H) {
                if (row instanceof HudToggleRow tr) {
                    renderHudToggleRow(ctx, tr, sy, mouseX, mouseY);
                } else if (row instanceof ToggleRow tr) {
                    renderToggleRow(ctx, tr, sy, mouseX, mouseY);
                } else if (row instanceof TextRow tr) {
                    renderTextRow(ctx, tr, sy, fi, mouseX, mouseY);
                    if (fi < fields.size()) {
                        TextFieldWidget fw = fields.get(fi);
                        fw.setX(contentX + PADDING + 2);
                        fw.setY(sy + rowH - 22);
                        fw.visible = fw.getY() > panelY + HEADER_H && fw.getY() + 16 < panelY + panelH - FOOTER_H;
                    }
                    fi++;
                } else if (row instanceof PoolHeaderRow) {
                    renderPoolHeader(ctx, sy, mouseX, mouseY);
                } else if (row instanceof PoolItemRow pr) {
                    renderPoolItem(ctx, pr, sy, fi, mouseX, mouseY);
                    if (fi < fields.size()) {
                        TextFieldWidget fw = fields.get(fi);
                        fw.setX(contentX + PADDING + 2);
                        fw.setY(sy + rowH - 22);
                        fw.visible = fw.getY() > panelY + HEADER_H && fw.getY() + 16 < panelY + panelH - FOOTER_H;
                    }
                    fi++;
                }
            } else if (row instanceof TextRow || row instanceof PoolItemRow) {
                if (fi < fields.size()) fields.get(fi).visible = false;
                fi++;
            }
            y += rowH;
        }

        ctx.disableScissor();

        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§8ESC to close"),
                contentX + CONTENT_W / 2, panelY + panelH - 12, SUBTEXT_COLOR);
        if (maxScroll > 0) renderScrollbar(ctx);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderTab(DrawContext ctx, int index, int mouseX, int mouseY) {
        int ty = panelY + HEADER_H + index * (TAB_H + 4);
        boolean active  = index == activeTab;
        boolean hovered = mouseX >= panelX && mouseX <= panelX + TAB_W
                && mouseY >= ty && mouseY < ty + TAB_H;

        float t  = getAnim("tab_" + index, active || hovered);
        float at = getAnim("tab_accent_" + index, active);

        int bg = lerpColor(TAB_COLOR, active ? TAB_ACTIVE : 0xFF16162E, t);
        ctx.fill(panelX, ty, panelX + TAB_W, ty + TAB_H, bg);

        int accentW = (int)(at * 3);
        if (accentW > 0)
            ctx.fill(panelX + TAB_W - accentW, ty, panelX + TAB_W, ty + TAB_H, ACCENT);

        int textColor = lerpColor(SUBTEXT_COLOR, TEXT_COLOR, t);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(TABS[index]),
                panelX + TAB_W / 2, ty + (TAB_H - 9) / 2, textColor);
    }

    private void renderContentHeader(DrawContext ctx) {
        int hy = panelY + 8;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§bMelodyAddons §7— " + TABS[activeTab]),
                contentX + CONTENT_W / 2, hy, TEXT_COLOR);
        ctx.fill(contentX + PADDING, hy + 20, contentX + CONTENT_W - PADDING, hy + 21, 0xFF333355);
    }

    private void renderToggleRow(DrawContext ctx, ToggleRow row, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= contentX + PADDING && mouseX <= contentX + CONTENT_W - PADDING
                && mouseY >= y && mouseY < y + TOGGLE_H;

        float ht = getAnim("toggle_hover_" + row.label(), hovered);
        if (ht > 0) ctx.fill(contentX + PADDING, y, contentX + CONTENT_W - PADDING, y + TOGGLE_H,
                lerpAlpha(0xFFFFFF, ht * 0.07f));

        ctx.drawTextWithShadow(textRenderer, Text.literal(row.label()),
                contentX + PADDING + 4, y + 8, TEXT_COLOR);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8" + row.description()),
                contentX + PADDING + 4, y + 20, SUBTEXT_COLOR);

        int sx = contentX + CONTENT_W - PADDING - TOGGLE_SW - 4;
        int sy = y + (TOGGLE_H - TOGGLE_SH) / 2;
        renderSwitch(ctx, sx, sy, row.getter().get(), row.label());
    }

    private void renderHudToggleRow(DrawContext ctx, HudToggleRow row, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= contentX + PADDING && mouseX <= contentX + CONTENT_W - PADDING
                && mouseY >= y && mouseY < y + TOGGLE_H;

        float ht = getAnim("toggle_hover_" + row.label(), hovered);
        if (ht > 0) ctx.fill(contentX + PADDING, y, contentX + CONTENT_W - PADDING, y + TOGGLE_H,
                lerpAlpha(0xFFFFFF, ht * 0.07f));

        ctx.drawTextWithShadow(textRenderer, Text.literal(row.label()),
                contentX + PADDING + 4, y + 8, TEXT_COLOR);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8" + row.description()),
                contentX + PADDING + 4, y + 20, SUBTEXT_COLOR);

        int bx = contentX + CONTENT_W - PADDING - TOGGLE_SW - HUD_BTN_W - 10;
        int by = y + (TOGGLE_H - 18) / 2;
        boolean btnHovered = mouseX >= bx && mouseX <= bx + HUD_BTN_W && mouseY >= by && mouseY <= by + 18;
        float bt = getAnim("hud_btn_" + row.hudId(), btnHovered);
        ctx.fill(bx, by, bx + HUD_BTN_W, by + 18, lerpColor(0xFF2A2A4A, 0xFF4A90D9, bt));
        ctx.fill(bx + 1, by + 1, bx + HUD_BTN_W - 1, by + 17, lerpColor(0xFF1A1A3A, 0xFF2A5A8A, bt));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("⊹"), bx + HUD_BTN_W / 2, by + 5, TEXT_COLOR);

        int sx = contentX + CONTENT_W - PADDING - TOGGLE_SW - 4;
        int sy = y + (TOGGLE_H - TOGGLE_SH) / 2;
        renderSwitch(ctx, sx, sy, row.getter().get(), row.label());
    }

    private void renderTextRow(DrawContext ctx, TextRow row, int y, int fi, int mouseX, int mouseY) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(row.label()),
                contentX + PADDING + 4, y + 6, TEXT_COLOR);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§8" + row.description()),
                contentX + PADDING + 4, y + 18, SUBTEXT_COLOR);
        int fx = contentX + PADDING + 2, fy = y + TEXT_H - 22, fw = CONTENT_W - PADDING * 2 - 4;
        boolean active = fi < fields.size() && fields.get(fi).isFocused();
        ctx.fill(fx, fy, fx + fw, fy + 18, active ? FIELD_ACTIVE : FIELD_BORDER);
        ctx.fill(fx + 1, fy + 1, fx + fw - 1, fy + 17, FIELD_BG);
    }

    private void renderPoolHeader(DrawContext ctx, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= contentX + PADDING && mouseX < contentX + CONTENT_W - PADDING - 26
                && mouseY >= y && mouseY < y + SECTION_H;
        float ht = getAnim("pool_header_hover", hovered);
        if (ht > 0) ctx.fill(contentX + PADDING, y, contentX + CONTENT_W - PADDING, y + SECTION_H,
                lerpAlpha(0xFFFFFF, ht * 0.07f));

        String arrow = poolExpanded ? "v" : ">";
        int arrowX = contentX + CONTENT_W - PADDING - 30 - textRenderer.getWidth(arrow);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7Message Pool"),
                contentX + PADDING + 4, y + (SECTION_H - 9) / 2, SUBTEXT_COLOR);
        ctx.drawTextWithShadow(textRenderer, Text.literal("§7" + arrow),
                arrowX, y + (SECTION_H - 9) / 2, SUBTEXT_COLOR);

        int bx = contentX + CONTENT_W - PADDING - 24;
        int by = y + (SECTION_H - 18) / 2;
        boolean btnH = mouseX >= bx && mouseX <= bx + 24 && mouseY >= by && mouseY <= by + 18;
        float bt = getAnim("pool_add_btn", btnH);
        ctx.fill(bx, by, bx + 24, by + 18, lerpColor(0xFF3A8A3A, 0xFF5AAA5A, bt));
        ctx.fill(bx + 1, by + 1, bx + 23, by + 17, lerpColor(0xFF2A6A2A, 0xFF4A9A4A, bt));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f+"), bx + 12, by + 5, TEXT_COLOR);
    }

    private void renderPoolItem(DrawContext ctx, PoolItemRow row, int y, int fi, int mouseX, int mouseY) {
        int bx = contentX + CONTENT_W - PADDING - 24;
        int by = y + (POOL_H - 18) / 2;
        boolean hovered = mouseX >= bx && mouseX <= bx + 24 && mouseY >= by && mouseY <= by + 18;
        float bt = getAnim("pool_del_" + row.index(), hovered);
        ctx.fill(bx, by, bx + 24, by + 18, lerpColor(0xFF7A1A1A, 0xFFAA2A2A, bt));
        ctx.fill(bx + 1, by + 1, bx + 23, by + 17, lerpColor(0xFF8A2A2A, 0xFFBB3A3A, bt));
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§f✕"), bx + 12, by + 5, TEXT_COLOR);

        int fx = contentX + PADDING + 2, fy = y + POOL_H - 22, fw = CONTENT_W - PADDING * 2 - 32;
        boolean active = fi < fields.size() && fields.get(fi).isFocused();
        ctx.fill(fx, fy, fx + fw, fy + 18, active ? FIELD_ACTIVE : FIELD_BORDER);
        ctx.fill(fx + 1, fy + 1, fx + fw - 1, fy + 17, FIELD_BG);
    }

    private void renderSwitch(DrawContext ctx, int x, int y, boolean on, String key) {
        float t = getAnim("sw_" + key, on);
        int track = lerpColor(0xFF363650, ACCENT, t);
        ctx.fill(x, y, x + TOGGLE_SW, y + TOGGLE_SH, track);
        ctx.fill(x + 1, y - 1, x + TOGGLE_SW - 1, y, track);
        ctx.fill(x + 1, y + TOGGLE_SH, x + TOGGLE_SW - 1, y + TOGGLE_SH + 1, track);
        int kx = (int)(x + 2 + t * (TOGGLE_SW - TOGGLE_SH));
        ctx.fill(kx, y + 2, kx + TOGGLE_SH - 4, y + TOGGLE_SH - 2, 0xFFFFFFFF);
        ctx.fill(kx + 1, y + 2, kx + TOGGLE_SH - 5, y + 4, 0x33FFFFFF);
    }

    private void renderScrollbar(DrawContext ctx) {
        int totalH = panelH - FOOTER_H - HEADER_H;
        int barH = Math.max(20, totalH * totalH / (totalH + maxScroll));
        int barY = panelY + HEADER_H + (totalH - barH) * scrollOffset / maxScroll;
        int barX = contentX + CONTENT_W - 4;
        ctx.fill(barX, panelY + HEADER_H, barX + 3, panelY + panelH - FOOTER_H, 0xFF222233);
        ctx.fill(barX, barY, barX + 3, barY + barH, ACCENT);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            for (int i = 0; i < TABS.length; i++) {
                int ty = panelY + HEADER_H + i * (TAB_H + 4);
                if ((int) click.x() >= panelX && (int) click.x() <= panelX + TAB_W
                        && (int) click.y() >= ty && (int) click.y() < ty + TAB_H) {
                    if (activeTab != i) {
                        activeTab = i;
                        scrollOffset = 0;
                        init();
                    }
                    return true;
                }
            }

            int y = panelY + HEADER_H;
            ModConfig cfg = ModConfig.get();

            for (Row row : rows) {
                int rowH = rowHeight(row);
                int sy = y - scrollOffset;

                if (row instanceof HudToggleRow tr) {
                    int bx = contentX + CONTENT_W - PADDING - TOGGLE_SW - HUD_BTN_W - 10;
                    int by = sy + (TOGGLE_H - 18) / 2;
                    if ((int) click.x() >= bx && (int) click.x() <= bx + HUD_BTN_W
                            && (int) click.y() >= by && (int) click.y() <= by + 18) {
                        MinecraftClient.getInstance().setScreen(new HudEditorScreen());
                        return true;
                    }
                    if ((int) click.x() >= contentX + PADDING && (int) click.x() <= contentX + CONTENT_W - PADDING
                            && (int) click.y() >= sy && (int) click.y() < sy + rowH) {
                        tr.setter().accept(!tr.getter().get());
                        return true;
                    }
                } else if (row instanceof ToggleRow tr) {
                    if ((int) click.x() >= contentX + PADDING && (int) click.x() <= contentX + CONTENT_W - PADDING
                            && (int) click.y() >= sy && (int) click.y() < sy + rowH) {
                        tr.setter().accept(!tr.getter().get());
                        return true;
                    }
                } else if (row instanceof PoolHeaderRow) {
                    int bx = contentX + CONTENT_W - PADDING - 24;
                    int arrowMaxX = bx - 4;
                    if ((int) click.x() >= contentX + PADDING && (int) click.x() < arrowMaxX
                            && (int) click.y() >= sy && (int) click.y() < sy + rowH) {
                        poolExpanded = !poolExpanded;
                        init();
                        return true;
                    }
                    int by = sy + (SECTION_H - 18) / 2;
                    if ((int) click.x() >= bx && (int) click.x() <= bx + 24
                            && (int) click.y() >= by && (int) click.y() <= by + 18) {
                        cfg.melodyMsgPool.add("");
                        cfg.save();
                        init();
                        return true;
                    }
                } else if (row instanceof PoolItemRow pr) {
                    int bx = contentX + CONTENT_W - PADDING - 24;
                    int by = sy + (POOL_H - 18) / 2;
                    if ((int) click.x() >= bx && (int) click.x() <= bx + 24
                            && (int) click.y() >= by && (int) click.y() <= by + 18) {
                        if (pr.index() < cfg.melodyMsgPool.size()) {
                            cfg.melodyMsgPool.remove(pr.index());
                            cfg.save();
                            init();
                            return true;
                        }
                    }
                }
                y += rowH;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 12));
        rebuildTextFields();
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }
}