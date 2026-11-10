package edu.scu.phantommaze.view;

import edu.scu.phantommaze.Config;
import edu.scu.phantommaze.model.Difficulty;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;

/**
 * The main menu: pick a difficulty and mouse sensitivity, then start or quit. Built from
 * ordinary Swing components (buttons, radio buttons, a slider) stacked top to bottom.
 */
public class MenuPanel extends JPanel {

    /** Called when the player presses Start. */
    @FunctionalInterface
    public interface StartListener {
        /**
         * Starts a game.
         *
         * @param difficulty       the chosen difficulty
         * @param mouseSensitivity the chosen sensitivity, 1 to 10
         */
        void start(Difficulty difficulty, int mouseSensitivity);
    }

    private static final Color TEXT = new Color(230, 220, 200);
    private static final Color DIM_TEXT = new Color(170, 160, 150);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private static final Font HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 18);

    private Difficulty selected = Difficulty.NORMAL;

    /**
     * Builds the menu.
     *
     * @param onStart called with the chosen settings when Start is pressed
     * @param onQuit  called when Quit is pressed
     */
    public MenuPanel(StartListener onStart, Runnable onQuit) {
        setPreferredSize(new Dimension(Config.WINDOW_WIDTH, Config.WINDOW_HEIGHT));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(60, 0, 40, 0));

        add(center(new JLabel(new ImageIcon(renderTitle("PHANTOM MAZE", 7)))));
        add(Box.createVerticalStrut(50));

        add(center(label("Difficulty", HEADING, TEXT)));
        add(Box.createVerticalStrut(8));
        JLabel description = label(selected.describe(), BODY, DIM_TEXT);
        JPanel choices = transparentRow();
        ButtonGroup group = new ButtonGroup(); // only one radio button in a group can be selected
        for (Difficulty d : Difficulty.values()) {
            JRadioButton button = new JRadioButton(d.label, d == selected);
            button.setFont(HEADING);
            button.setForeground(TEXT);
            button.setOpaque(false);
            button.setFocusable(false);
            button.addActionListener(e -> {
                selected = d;
                description.setText(d.describe());
            });
            group.add(button);
            choices.add(button);
            choices.add(Box.createHorizontalStrut(24));
        }
        add(center(choices));
        add(Box.createVerticalStrut(6));
        add(center(description));
        add(Box.createVerticalStrut(36));

        JLabel sensitivityLabel = label("Mouse sensitivity: " + Config.DEFAULT_MOUSE_SENSITIVITY, HEADING, TEXT);
        JSlider sensitivity = new JSlider(1, 10, Config.DEFAULT_MOUSE_SENSITIVITY);
        sensitivity.setOpaque(false);
        sensitivity.setFocusable(false);
        sensitivity.setMaximumSize(new Dimension(320, 40));
        sensitivity.addChangeListener(e -> sensitivityLabel.setText("Mouse sensitivity: " + sensitivity.getValue()));
        add(center(sensitivityLabel));
        add(center(sensitivity));
        add(Box.createVerticalStrut(36));

        JPanel buttons = transparentRow();
        JButton start = new JButton("Start");
        start.setFont(HEADING);
        start.addActionListener(e -> onStart.start(selected, sensitivity.getValue()));
        JButton quit = new JButton("Quit");
        quit.setFont(HEADING);
        quit.addActionListener(e -> onQuit.run());
        buttons.add(start);
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(quit);
        add(center(buttons));
        add(Box.createVerticalStrut(40));

        add(center(label("Controls", HEADING, TEXT)));
        add(Box.createVerticalStrut(6));
        String[] controls = {
            "W A S D  move      Mouse / Left-Right arrows  turn",
            "Left click / Space  shoot      R  reload      M  minimap",
            "Esc  pause (click to resume, Q for this menu)",
            "Grab a corner power-up for the shotgun and armor: enemies flee, one shell kills.",
        };
        for (String line : controls) {
            add(center(label(line, BODY, DIM_TEXT)));
        }
        add(Box.createVerticalGlue());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setPaint(new GradientPaint(0, 0, new Color(10, 4, 8), 0, getHeight(), new Color(70, 10, 10)));
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /** Draws text in the Doom font into an image, so it can sit in a normal JLabel. */
    private static BufferedImage renderTitle(String text, int scale) {
        int w = DoomFont.width(text) * scale;
        int h = DoomFont.lineHeight() * scale;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        DoomFont.draw(g, text, 0, 0, scale);
        g.dispose();
        return image;
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static JPanel transparentRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        return row;
    }

    /** Centers a component horizontally in the menu's column. */
    private static <T extends JComponent> T center(T component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        return component;
    }
}
