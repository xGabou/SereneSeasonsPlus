package com.Gabou.sereneseasonsplus.client.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class SereneExtendedList extends ObjectSelectionList<SereneExtendedList.Row> {

    public SereneExtendedList(Minecraft mc, int width, int height, int top, int itemHeight) {
        super(mc, width, height, top, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return 360;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowLeft() + this.getRowWidth() + 8;
    }

    public void addRow(Component label, AbstractWidget... widgets) {
        this.addEntry(new Row(this, label, widgets));
    }

    public static class Row extends ObjectSelectionList.Entry<Row> {
        private final SereneExtendedList owner;
        private final Component label;
        private final List<AbstractWidget> widgets;

        private int lastX = Integer.MIN_VALUE;
        private int lastY = Integer.MIN_VALUE;
        private int lastRowW = Integer.MIN_VALUE;
        private int lastRowH = Integer.MIN_VALUE;

        Row(SereneExtendedList owner, Component label, AbstractWidget... widgets) {
            this.owner = owner;
            this.label = label;
            this.widgets = Arrays.asList(widgets);
        }

        private void layoutIfNeeded(int x, int y, int rowWidth, int rowHeight) {
            if (x == lastX && y == lastY && rowWidth == lastRowW && rowHeight == lastRowH) {
                return;
            }
            lastX = x;
            lastY = y;
            lastRowW = rowWidth;
            lastRowH = rowHeight;

            final int widgetX = x + rowWidth - 200;
            final int widgetY = y + (rowHeight - 20) / 2;

            for (AbstractWidget widget : widgets) {
                if (widget.getX() != widgetX) {
                    widget.setX(widgetX);
                }
                if (widget.getY() != widgetY) {
                    widget.setY(widgetY);
                }
                if (widget.getWidth() != 200) {
                    widget.setWidth(200);
                }
            }
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float delta) {
            int x = this.getX();
            int y = this.getY();
            int rowWidth = this.getWidth();
            int rowHeight = this.getHeight();
            layoutIfNeeded(x, y, rowWidth, rowHeight);

            graphics.drawString(owner.minecraft.font, label, x, y + 6, 0xFFFFFF, false);

            for (AbstractWidget widget : widgets) {
                widget.render(graphics, mouseX, mouseY, delta);
            }
        }

        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            for (AbstractWidget widget : widgets) {
                if (widget.mouseClicked(event, doubleClick)) {
                    owner.setSelected(this);
                    widget.setFocused(true);
                    return true;
                }
            }
            owner.setSelected(this);
            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            for (AbstractWidget widget : widgets) {
                widget.mouseReleased(event);
            }
            return false;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            for (AbstractWidget widget : widgets) {
                if (widget.mouseDragged(event, dx, dy)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double dx, double dy) {
            for (AbstractWidget widget : widgets) {
                if (widget.mouseScrolled(mx, my, dx, dy)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            for (AbstractWidget widget : widgets) {
                if (widget.keyPressed(event)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            for (AbstractWidget widget : widgets) {
                if (widget.charTyped(event)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void updateNarration(NarrationElementOutput out) {
        }

        public Component getNarration() {
            return label;
        }
    }
}
