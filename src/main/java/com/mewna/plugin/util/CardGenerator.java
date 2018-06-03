package com.mewna.plugin.util;

import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.plugin.plugins.PluginLevels;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates the cards for rank, level up, ...
 *
 * @author amy
 * @since 6/2/18.
 */
@SuppressWarnings("WeakerAccess")
public final class CardGenerator {
    // colours
    
    public static final String PRIMARY_COLOUR_STR = "0xdb325c";
    @SuppressWarnings("unused")
    public static final int PRIMARY_COLOUR = 0xdb325c;
    private static final Color NINETY_PERCENT_OPAQUE_BLACK = new Color(0, 0, 0, 230);
    private static final Color SIXTY_SEVEN_PERCENT_OPAQUE_BLACK = new Color(0, 0, 0, 171);
    private static final Color PRIMARY_THEME_COLOUR = Color.decode(PRIMARY_COLOUR_STR);
    
    // fonts
    
    private static final Font USERNAME_FONT;
    private static final Font STATS_FONT;
    private static final Font STATS_FONT_SMALLER;
    
    private static final Map<TextAttribute, Object> FONT_SETTINGS = new HashMap<>();
    
    static {
        FONT_SETTINGS.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
    
        USERNAME_FONT = new Font("Yanone Kaffeesatz", Font.PLAIN, 42).deriveFont(FONT_SETTINGS);
        STATS_FONT = new Font("Droid Sans", Font.PLAIN, 32).deriveFont(FONT_SETTINGS);
        STATS_FONT_SMALLER = new Font("Droid Sans", Font.PLAIN, 30).deriveFont(FONT_SETTINGS);
    }
    
    private CardGenerator() {
    }
    
    private static void setRenderHints(final Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
    
    public static byte[] generateProfileCard(final Guild guild, final User user, final Player player) {
        final BufferedImage card = new BufferedImage(600, 600 , BufferedImage.TYPE_INT_ARGB);
        // TODO: Custom backgrounds
        try {
            // TODO: DO THIS RIGHT!z
            final Graphics2D g2 = card.createGraphics();
            setRenderHints(g2);
            // Background
            g2.setColor(PRIMARY_THEME_COLOUR);
            g2.fillRect(0, 0, 800, 200);
        
            // Main card panel
            g2.setColor(NINETY_PERCENT_OPAQUE_BLACK);
            g2.fillRect(100, 10, 676, 180);
        
            // Avatar panel
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
        
            g2.fillRect(34, 34, 132, 132);
        
            // Avatar
            final BufferedImage avatar = downloadAvatar(user.getAvatarURL().replaceAll("gif", "png") + "?size=128");
            g2.drawImage(avatar, 36, 36, 128, 128, null);
        
            // Username
            g2.setPaint(Color.WHITE);
            g2.setFont(USERNAME_FONT);
            setRenderHints(g2);
            g2.drawString(user.getName(), 187, 70);
        
            // Stats
        
            final String lvl = "LVL ";
            final String rank = "RANK #";
        
            g2.setFont(STATS_FONT);
            setRenderHints(g2);
        
            // User stats
            final long userXp = player.getXp(guild);
            final long userLevel = PluginLevels.xpToLevel(userXp);
            final long nextLevel = userLevel + 1;
            // Font sizing util
            final FontMetrics metrics = g2.getFontMetrics(STATS_FONT);
            final int lvlWidth = metrics.stringWidth(lvl);
            final int rankWidth = metrics.stringWidth(rank);
            final int playerLevelWidth = metrics.stringWidth(userLevel + "    ");
            final long playerRank = PluginLevels.getPlayerRankInGuild(guild, user);
        
            g2.drawString(lvl, 187, 113); // LVL
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(userLevel + "    ", 187 + lvlWidth, 113); // 1234
            g2.setPaint(Color.WHITE);
            g2.drawString(rank, 187 + lvlWidth + playerLevelWidth, 113); // RANK #
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(playerRank + "", 187 + lvlWidth + playerLevelWidth + rankWidth, 113); // 123456
        
            // XP bar
            final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
            final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
            final long xpNeeded = PluginLevels.nextLevelXp(userXp);
            final long nextXpTotal = nextLevelXp - currentLevelXp - xpNeeded;
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            g2.fillRect(188, 123, 566, 42);
            // calc. bar size
            final int barWidth = (int) (562 * (nextXpTotal / (double) nextLevelXp));
            g2.setColor(PRIMARY_THEME_COLOUR);
            g2.fillRect(190, 125, barWidth, 38);
            // XP text
            drawCenteredString(g2, String.format("%s / %s EXP", nextXpTotal, nextLevelXp),
                    new Rectangle(190, 125, 562, 38), STATS_FONT_SMALLER, Color.WHITE);
        
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(card, "png", baos);
            g2.dispose();
            final byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch(final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static byte[] generateRankCard(final Guild guild, final User user, final Player player) { // lol
        final BufferedImage card = new BufferedImage(800, 200, BufferedImage.TYPE_INT_ARGB);
        // TODO: Custom backgrounds
        try {
            final Graphics2D g2 = card.createGraphics();
            setRenderHints(g2);
            // Background
            g2.setColor(PRIMARY_THEME_COLOUR);
            g2.fillRect(0, 0, 800, 200);
            
            // Main card panel
            g2.setColor(NINETY_PERCENT_OPAQUE_BLACK);
            g2.fillRect(100, 10, 676, 180);
            
            // Avatar panel
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            
            g2.fillRect(34, 34, 132, 132);
            
            // Avatar
            final BufferedImage avatar = downloadAvatar(user.getAvatarURL().replaceAll("gif", "png") + "?size=128");
            g2.drawImage(avatar, 36, 36, 128, 128, null);
            
            // Username
            g2.setPaint(Color.WHITE);
            g2.setFont(USERNAME_FONT);
            setRenderHints(g2);
            g2.drawString(user.getName(), 187, 70);
            
            // Stats
            
            final String lvl = "LVL ";
            final String rank = "RANK #";
            
            g2.setFont(STATS_FONT);
            setRenderHints(g2);
            
            // User stats
            final long userXp = player.getXp(guild);
            final long userLevel = PluginLevels.xpToLevel(userXp);
            final long nextLevel = userLevel + 1;
            // Font sizing util
            final FontMetrics metrics = g2.getFontMetrics(STATS_FONT);
            final int lvlWidth = metrics.stringWidth(lvl);
            final int rankWidth = metrics.stringWidth(rank);
            final int playerLevelWidth = metrics.stringWidth(userLevel + "    ");
            final long playerRank = PluginLevels.getPlayerRankInGuild(guild, user);
            
            g2.drawString(lvl, 187, 113); // LVL
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(userLevel + "    ", 187 + lvlWidth, 113); // 1234
            g2.setPaint(Color.WHITE);
            g2.drawString(rank, 187 + lvlWidth + playerLevelWidth, 113); // RANK #
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(playerRank + "", 187 + lvlWidth + playerLevelWidth + rankWidth, 113); // 123456
            
            // XP bar
            final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
            final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
            final long xpNeeded = PluginLevels.nextLevelXp(userXp);
            final long nextXpTotal = nextLevelXp - currentLevelXp - xpNeeded;
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            g2.fillRect(188, 123, 566, 42);
            // calc. bar size
            final int barWidth = (int) (562 * (nextXpTotal / (double) nextLevelXp));
            g2.setColor(PRIMARY_THEME_COLOUR);
            g2.fillRect(190, 125, barWidth, 38);
            // XP text
            drawCenteredString(g2, String.format("%s / %s EXP", nextXpTotal, nextLevelXp),
                    new Rectangle(190, 125, 562, 38), STATS_FONT_SMALLER, Color.WHITE);
            
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(card, "png", baos);
            g2.dispose();
            final byte[] bytes = baos.toByteArray();
            baos.close();
            return bytes;
        } catch(final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static void drawCenteredString(final Graphics2D g2, final String text, final Rectangle rect,
                                           @SuppressWarnings("SameParameterValue") final Font font,
                                           @SuppressWarnings("SameParameterValue") final Color color) {
        final FontMetrics metrics = g2.getFontMetrics(font);
        final int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        final int y = rect.y + (rect.height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setFont(font);
        setRenderHints(g2);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }
    
    private static BufferedImage downloadAvatar(final String avatarUrl) {
        try {
            final URL url = new URL(avatarUrl);
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            // Default Java user agent gets blocked by Discord :(
            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/55.0.2883.87 Safari/537.36");
            final InputStream is = connection.getInputStream();
            final BufferedImage avatar = ImageIO.read(is);
            is.close();
            connection.disconnect();
            return avatar;
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
