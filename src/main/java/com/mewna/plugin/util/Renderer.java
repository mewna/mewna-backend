package com.mewna.plugin.util;

import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.plugin.plugins.PluginLevels;
import com.mewna.plugin.util.TextureManager.Background;
import com.mewna.util.CacheUtil;
import com.mewna.util.CacheUtil.CachedImage;
import com.mewna.util.Numbers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.AttributedString;
import java.util.*;
import java.util.List;

/**
 * Generates the cards for rank, level up, ...
 * <p>
 * TODO: Use redis to cache avatars so we can share the cache between multiple clients
 *
 * @author amy
 * @since 6/2/18.
 */
@SuppressWarnings("WeakerAccess")
public final class Renderer {
    // colours
    
    public static final String PRIMARY_COLOUR_STR = "0xdb325c";
    @SuppressWarnings("unused")
    public static final int PRIMARY_COLOUR = 0xdb325c;
    private static final Color NINETY_PERCENT_OPAQUE_BLACK = new Color(0, 0, 0, 180);
    private static final Color SIXTY_SEVEN_PERCENT_OPAQUE_BLACK = new Color(0, 0, 0, 127);
    private static final Color PRIMARY_THEME_COLOUR = Color.decode(PRIMARY_COLOUR_STR);
    
    // fonts
    
    private static final Font USERNAME_FONT;
    private static final Font STATS_FONT;
    private static final Font STATS_FONT_SMALLER;
    private static final Font ABOUT_ME_FONT;
    
    private static final Map<TextAttribute, Object> FONT_SETTINGS = new HashMap<>();
    
    static {
        FONT_SETTINGS.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        
        USERNAME_FONT = new Font("Yanone Kaffeesatz", Font.PLAIN, 42).deriveFont(FONT_SETTINGS);
        STATS_FONT = new Font("Droid Sans", Font.PLAIN, 32).deriveFont(FONT_SETTINGS);
        STATS_FONT_SMALLER = new Font("Droid Sans", Font.PLAIN, 30).deriveFont(FONT_SETTINGS);
        ABOUT_ME_FONT = new Font("Droid Sans", Font.PLAIN, 24).deriveFont(FONT_SETTINGS);
    }
    
    private Renderer() {
    }
    
    private static CachedImage getBackground(final Player player) {
        final Optional<Background> background = TextureManager.getBackground(player);
        final Background bg = background.orElse(TextureManager.defaultBg);
        return CacheUtil.getImageResource(bg.getPath());
    }
    
    private static void setRenderHints(final Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    }
    
    private static List<String> wrap(final String txt, final FontMetrics fm, @SuppressWarnings("SameParameterValue") final int maxWidth) {
        final StringTokenizer st = new StringTokenizer(txt);
        
        final List<String> list = new ArrayList<>();
        String line = "";
        String lineBeforeAppend;
        while(st.hasMoreTokens()) {
            final String seg = st.nextToken();
            lineBeforeAppend = line;
            final int width = fm.stringWidth(line + seg + ' ');
            if(width >= maxWidth) {
                list.add(lineBeforeAppend);
                line = seg + ' ';
            } else {
                line += seg + ' ';
            }
        }
        //the remaining part.
        if(!line.isEmpty()) {
            list.add(line);
        }
        return list;
    }
    
    public static byte[] generateProfileCard(final User user, final Player player) {
        final BufferedImage card = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);
        // TODO: Custom backgrounds
        try {
            final Graphics2D g2 = card.createGraphics();
            setRenderHints(g2);
            // Background
            final CachedImage bg = getBackground(player);
            final AffineTransform transform = new AffineTransform();
            // compute scale
            // this is a square, so we scale the smallest dimension (height) to reach it
            final int bgHeight = bg.getImage().getHeight();
            final double scale = 1 / (bgHeight / 600D);
            transform.scale(scale, scale);
            final BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
            g2.drawImage(bg.getImage(), op, 0, 0);
            
            // Main card panel
            g2.setColor(NINETY_PERCENT_OPAQUE_BLACK);
            g2.fillRect(10, 100, 580, 476);
            
            // Avatar panel
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            
            g2.fillRect(234, 34, 132, 132);
            
            // Avatar
            final BufferedImage avatar = TextureManager.getCachedAvatar(user);
            g2.drawImage(avatar, 236, 36, 128, 128, null);
            
            // Username
            // centered string at y=246
            setRenderHints(g2);
            drawCenteredString(g2, user.getName().toUpperCase(),
                    new Rectangle(10, 202, 580, USERNAME_FONT.getSize()), USERNAME_FONT, Color.WHITE);
            
            g2.setFont(ABOUT_ME_FONT);
            g2.setColor(Color.WHITE);
            // About text
            final FontMetrics aboutMeFontMetrics = g2.getFontMetrics(ABOUT_ME_FONT);
            // 32, 268
            // 536x122
            final List<String> wrap = wrap(player.getAboutText(), aboutMeFontMetrics, 536);
            int y = 268;
            for(final String line : wrap) {
                g2.drawString(line, 32, y + ABOUT_ME_FONT.getSize());
                y += ABOUT_ME_FONT.getSize();
            }
            
            // Stats
            final FontMetrics statsFontSmallerMetrics = g2.getFontMetrics(ABOUT_ME_FONT);
            
            final String globalLevel = Numbers.format(PluginLevels.xpToLevel(player.getGlobalXp()));
            final String globalLevelLabel = "GLOBAL LEVEL";
            final int globalLevelLabelWidth = statsFontSmallerMetrics.stringWidth(globalLevelLabel);
            
            final int globalRankTmp = PluginLevels.getPlayerRankGlobally(user);
            final String globalRank = globalRankTmp == -1 ? "UNRANKED" : '#' + Numbers.format(globalRankTmp);
            final String globalRankLabel = "GLOBAL RANK";
            final int globalRankLabelWidth = statsFontSmallerMetrics.stringWidth(globalRankLabel);
            
            final String score = Numbers.format(player.calculateScore());
            final String scoreLabel = "OVERALL SCORE";
            final int scoreLabelWidth = statsFontSmallerMetrics.stringWidth(scoreLabel);
            
            // Ensure everything is roughly the same size
            final int allLabelSizes = Math.max(scoreLabelWidth, Math.max(globalRankLabelWidth, globalLevelLabelWidth));
            
            // 32, 423
            // Global level
            drawCenteredString(g2, globalLevel, new Rectangle(32, 423, allLabelSizes,
                    STATS_FONT.getSize()), STATS_FONT, PRIMARY_THEME_COLOUR);
            drawCenteredString(g2, globalLevelLabel, new Rectangle(32, 423, allLabelSizes,
                    STATS_FONT.getSize() * 2 + ABOUT_ME_FONT.getSize()), ABOUT_ME_FONT, Color.WHITE);
            
            // 234, 423
            // Global rank
            final AttributedString rankString = new AttributedString(globalRank);
            if(globalRankTmp > 0) {
                rankString.addAttribute(TextAttribute.FOREGROUND, Color.WHITE, 0, 1);
                rankString.addAttribute(TextAttribute.FOREGROUND, PRIMARY_THEME_COLOUR, 1, globalRank.length());
            } else {
                rankString.addAttribute(TextAttribute.FOREGROUND, PRIMARY_THEME_COLOUR);
            }
            drawCenteredString(g2, globalRank, rankString, new Rectangle(300 - allLabelSizes / 2, 423, allLabelSizes,
                    STATS_FONT.getSize()), STATS_FONT);
            drawCenteredString(g2, globalRankLabel, new Rectangle(300 - allLabelSizes / 2, 423, allLabelSizes,
                    STATS_FONT.getSize() * 2 + ABOUT_ME_FONT.getSize()), ABOUT_ME_FONT, Color.WHITE);
            
            // 435, 423
            // Global score
            drawCenteredString(g2, score, new Rectangle(568 - allLabelSizes, 423, allLabelSizes,
                    STATS_FONT.getSize()), STATS_FONT, PRIMARY_THEME_COLOUR);
            drawCenteredString(g2, scoreLabel, new Rectangle(568 - allLabelSizes, 423, allLabelSizes,
                    STATS_FONT.getSize() * 2 + ABOUT_ME_FONT.getSize()), ABOUT_ME_FONT, Color.WHITE);
            
            // XP bar
            // 32, 519
            // 536x35
            final long userXp = player.getGlobalXp();
            final long userLevel = PluginLevels.xpToLevel(userXp);
            final long nextLevel = userLevel + 1;
            final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
            final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
            final long xpNeeded = PluginLevels.nextLevelXp(userXp);
            final long nextXpTotal = nextLevelXp - currentLevelXp - xpNeeded;
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            g2.fillRect(32, 519, 536, 35);
            // calc. bar size
            final int barWidth = (int) (532 * (nextXpTotal / (double) nextLevelXp));
            g2.setColor(PRIMARY_THEME_COLOUR);
            g2.fillRect(34, 521, barWidth, 31);
            // XP text
            drawCenteredString(g2, String.format("%s / %s EXP", Numbers.format(nextXpTotal), Numbers.format(nextLevelXp)),
                    new Rectangle(32, 519, 536, 35), STATS_FONT_SMALLER, Color.WHITE);
            
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
        try {
            final Graphics2D g2 = card.createGraphics();
            setRenderHints(g2);
            // Background
            final CachedImage bg = getBackground(player);
            final AffineTransform transform = new AffineTransform();
            // compute scale
            // this is a 800x200 rect, so we scale the smallest dimension (width) to reach it
            final int bgWidth = bg.getImage().getWidth();
            final double scale = 1 / (bgWidth / 800D);
            transform.scale(scale, scale);
            final BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
            g2.drawImage(bg.getImage(), op, 0, 0);
            
            // Main card panel
            g2.setColor(NINETY_PERCENT_OPAQUE_BLACK);
            g2.fillRect(100, 10, 676, 180);
            
            // Avatar panel
            g2.setColor(SIXTY_SEVEN_PERCENT_OPAQUE_BLACK);
            
            g2.fillRect(34, 34, 132, 132);
            
            // Avatar
            final BufferedImage avatar = TextureManager.getCachedAvatar(user);
            g2.drawImage(avatar, 36, 36, 128, 128, null);
            
            // Username
            g2.setPaint(Color.WHITE);
            g2.setFont(USERNAME_FONT);
            setRenderHints(g2);
            g2.drawString(user.getName().toUpperCase(), 187, 70);
            
            // Stats
            
            final String lvl = "LVL ";
            final String rank = "RANK #";
            
            g2.setFont(STATS_FONT);
            setRenderHints(g2);
            
            // User stats
            final long userXp = player.getXp(guild);
            final long userLevel = PluginLevels.xpToLevel(userXp);
            final long nextLevel = userLevel + 1;
            final long playerRank = PluginLevels.getPlayerRankInGuild(guild, user);
            // Text
            final String userLevelText = Numbers.format(userLevel) + "    ";
            final String playerRankText = Numbers.format(playerRank);
            // Font sizing util
            final FontMetrics metrics = g2.getFontMetrics(STATS_FONT);
            final int lvlWidth = metrics.stringWidth(lvl);
            final int rankWidth = metrics.stringWidth(rank);
            final int playerLevelWidth = metrics.stringWidth(userLevelText);
            
            g2.drawString(lvl, 187, 113); // LVL
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(userLevelText, 187 + lvlWidth, 113); // 1234
            g2.setPaint(Color.WHITE);
            g2.drawString(rank, 187 + lvlWidth + playerLevelWidth, 113); // RANK #
            g2.setPaint(PRIMARY_THEME_COLOUR);
            g2.drawString(playerRankText, 187 + lvlWidth + playerLevelWidth + rankWidth, 113); // 123456
            
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
            drawCenteredString(g2, String.format("%s / %s EXP", Numbers.format(nextXpTotal), Numbers.format(nextLevelXp)),
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
    
    private static void drawCenteredString(final Graphics2D g2, final String text, final Rectangle rect, final Font font,
                                           final Color color) {
        final FontMetrics metrics = g2.getFontMetrics(font);
        final int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        final int y = rect.y + (rect.height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setFont(font);
        setRenderHints(g2);
        g2.setColor(color);
        g2.drawString(text, x, y);
    }
    
    private static void drawCenteredString(final Graphics2D g2, final String text, final AttributedString renderable,
                                           final Rectangle rect, @SuppressWarnings("SameParameterValue") final Font font) {
        final FontMetrics metrics = g2.getFontMetrics(font);
        final int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        final int y = rect.y + (rect.height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setFont(font);
        renderable.addAttribute(TextAttribute.FONT, font);
        setRenderHints(g2);
        g2.drawString(renderable.getIterator(), x, y);
    }
}
