package com.mewna.plugin.plugins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.DiscordCache;
import com.mewna.data.Player.ClickerBuildings;
import com.mewna.data.Player.ClickerData;
import com.mewna.data.Player.ClickerTiers;
import com.mewna.data.Player.ClickerUpgrades;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.metadata.Ratelimit;
import com.mewna.plugin.metadata.RatelimitType;
import com.mewna.plugin.plugins.PluginMisc.XMonster.Action;
import com.mewna.plugin.plugins.PluginMisc.XMonster.Legendary;
import com.mewna.plugin.plugins.dnd.dice.notation.DiceNotationExpression;
import com.mewna.plugin.plugins.dnd.dice.parser.DefaultDiceNotationParser;
import com.mewna.plugin.plugins.dnd.dice.parser.DiceNotationParser;
import com.mewna.plugin.plugins.economy.Item;
import com.mewna.plugin.plugins.misc.serial.*;
import com.mewna.plugin.plugins.settings.MiscSettings;
import com.mewna.plugin.util.CurrencyHelper;
import com.mewna.plugin.util.Emotes;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;
import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings("OverlyCoupledClass")
@Plugin(name = "Misc", desc = "Miscellaneous things, like kittens and puppies.", settings = MiscSettings.class)
public class PluginMisc extends BasePlugin {
    private static final Map<Character, String> MEMETEXT_MAP = new HashMap<>();
    private static final Collection<String> SPELL_CLASSES = new CopyOnWriteArrayList<>(
            Arrays.asList(
                    "bard", "cleric", "druid", "paladin", "ranger", "sorcerer", "warlock", "wizard"
            )
    );
    
    static {
        MEMETEXT_MAP.put('a', "\uD83C\uDDE6");
        MEMETEXT_MAP.put('b', "\uD83C\uDDE7");
        MEMETEXT_MAP.put('c', "\uD83C\uDDE8");
        MEMETEXT_MAP.put('d', "\uD83C\uDDE9");
        MEMETEXT_MAP.put('e', "\uD83C\uDDEA");
        MEMETEXT_MAP.put('f', "\uD83C\uDDEB");
        MEMETEXT_MAP.put('g', "\uD83C\uDDEC");
        MEMETEXT_MAP.put('h', "\uD83C\uDDED");
        MEMETEXT_MAP.put('i', "\uD83C\uDDEE");
        MEMETEXT_MAP.put('j', "\uD83C\uDDEF");
        MEMETEXT_MAP.put('k', "\uD83C\uDDF0");
        MEMETEXT_MAP.put('l', "\uD83C\uDDF1");
        MEMETEXT_MAP.put('m', "\uD83C\uDDF2");
        MEMETEXT_MAP.put('n', "\uD83C\uDDF3");
        MEMETEXT_MAP.put('o', "\uD83C\uDDF4");
        MEMETEXT_MAP.put('p', "\uD83C\uDDF5");
        MEMETEXT_MAP.put('q', "\uD83C\uDDF6");
        MEMETEXT_MAP.put('r', "\uD83C\uDDF7");
        MEMETEXT_MAP.put('s', "\uD83C\uDDF8");
        MEMETEXT_MAP.put('t', "\uD83C\uDDF9");
        MEMETEXT_MAP.put('u', "\uD83C\uDDFA");
        MEMETEXT_MAP.put('v', "\uD83C\uDDFB");
        MEMETEXT_MAP.put('w', "\uD83C\uDDFC");
        MEMETEXT_MAP.put('x', "\uD83C\uDDFD");
        MEMETEXT_MAP.put('y', "\uD83C\uDDFE");
        MEMETEXT_MAP.put('z', "\uD83C\uDDFF");
        MEMETEXT_MAP.put('?', "\u2753");
        MEMETEXT_MAP.put('!', "\u2757");
    }
    
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(XMonster.class, new XMonsterDeserializer())
            .registerTypeAdapter(XSpell.class, new XSpellDeserializer())
            .registerTypeAdapter(XMagicItem.class, new XMagicItemDeserializer())
            .registerTypeAdapter(XItem.class, new XItemDeserializer())
            .registerTypeAdapter(XFeat.class, new XFeatDeserializer())
            .registerTypeAdapter(XRace.class, new XRaceDeserializer())
            .create();
    
    @Getter
    private final List<XSpell> spells = new CopyOnWriteArrayList<>();
    @Getter
    private final List<XMonster> monsters = new CopyOnWriteArrayList<>();
    @Getter
    private final List<XMagicItem> magicItems = new CopyOnWriteArrayList<>();
    @Getter
    private final List<XItem> items = new CopyOnWriteArrayList<>();
    @Getter
    private final List<XFeat> feats = new CopyOnWriteArrayList<>();
    @Getter
    private final List<XRace> races = new CopyOnWriteArrayList<>();
    private final DiceNotationParser parser = new DefaultDiceNotationParser();
    
    @Inject
    private OkHttpClient client;
    @Inject
    private CurrencyHelper currencyHelper;
    
    private String[] rubeface = {};
    
    public PluginMisc() {
        parseRubeface();
        parseMonsters();
        parseSpells();
        parseMagicItems();
        parseItems();
        parseFeats();
        parseRaces();
    }
    
    private void parseRubeface() {
        rubeface = readFile("misc/rubeface.txt").toArray(new String[0]);
    }
    
    private void parseMonsters() {
        final XMonster[] monsters = gson
                .fromJson(String.join("\n", readFile("dnd/monsters.json")), XMonster[].class);
        this.monsters.addAll(Arrays.asList(monsters));
    }
    
    private void parseSpells() {
        final XSpell[] spells = gson
                .fromJson(String.join("\n", readFile("dnd/spells.json")), XSpell[].class);
        this.spells.addAll(Arrays.asList(spells));
    }
    
    private void parseMagicItems() {
        final XMagicItem[] magicItems = gson
                .fromJson(String.join("\n", readFile("dnd/magicitems.json")), XMagicItem[].class);
        this.magicItems.addAll(Arrays.asList(magicItems));
    }
    
    private void parseItems() {
        final XItem[] items = gson
                .fromJson(String.join("\n", readFile("dnd/items.json")), XItem[].class);
        this.items.addAll(Arrays.asList(items));
    }
    
    private void parseFeats() {
        final XFeat[] feats = gson
                .fromJson(String.join("\n", readFile("dnd/feats.json")), XFeat[].class);
        this.feats.addAll(Arrays.asList(feats));
    }
    
    private void parseRaces() {
        final XRace[] races = gson
                .fromJson(String.join("\n", readFile("dnd/races.json")), XRace[].class);
        this.races.addAll(Arrays.asList(races));
    }
    
    private Collection<String> readFile(final String file) {
        final List<String> list = new ArrayList<>();
        
        final InputStream in = getClass().getResourceAsStream('/' + file);
        final BufferedReader input = new BufferedReader(new InputStreamReader(in));
        input.lines().forEach(list::add);
        try {
            input.close();
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new RuntimeException(e);
        }
        
        return list;
    }
    
    @Command(names = "cat", desc = "commands.misc.cat", usage = "cat", examples = "cat")
    public void cat(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String cat = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://aws.random.cat/meow").build())
                    .execute().body()).string();
            final JsonObject json = new JsonObject(cat); // meow
            ctx.sendMessage(new EmbedBuilder().title("Cat").image(json.getString("file")).build());
        } catch(final IOException e) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.misc.commands.cat.invalid"));
        }
    }
    
    @Command(names = "dog", desc = "commands.misc.dog", usage = "dog", examples = "dog")
    public void dog(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String dog = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://random.dog/woof.json").build())
                    .execute().body()).string();
            final JsonObject json = new JsonObject(dog); // woof
            ctx.sendMessage(new EmbedBuilder().title("Dog").image(json.getString("url")).build());
        } catch(final IOException e) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.misc.commands.dog.invalid"));
        }
    }
    
    @Ratelimit(type = RatelimitType.GUILD, time = 5)
    @Command(names = "catgirl", desc = "commands.misc.catgirl", usage = "catgirl", examples = "catgirl")
    public void catgirl(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String catgirl = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://nekos.life/api/neko").build())
                    .execute().body()).string();
            final JsonObject json = new JsonObject(catgirl); // nya
            ctx.sendMessage(new EmbedBuilder().title("Catgirl").image(json.getString("neko")).build());
        } catch(final IOException e) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.misc.commands.catgirl.invalid"));
        }
    }
    
    @Command(names = {"rubeface", "rf"}, desc = "commands.misc.rubeface", usage = "rubeface",
            examples = "rubeface")
    public void rubeface(final CommandContext ctx) {
        ctx.sendMessage(new EmbedBuilder().title("Rubeface").image(rubeface[random().nextInt(rubeface.length)]).build());
    }
    
    @Command(names = {"memetext", "bigtext", "mt"}, desc = "commands.misc.memetext", usage = "memetext <input>",
            examples = "memetext some text I want to be big!")
    public void memetext(final CommandContext ctx) {
        if(ctx.getArgstr().trim().isEmpty()) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.misc.commands.memetext.invalid"));
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for(final char c : ctx.getArgstr().toCharArray()) {
            sb.append(MEMETEXT_MAP.getOrDefault(c, "" + c)).append(' ');
        }
        ctx.sendMessage(ctx.getUser().asMention() + ": " + sb.toString().trim()
                .replace("@everyone", "[haha very funny]")
                .replace("@here", "[haha very funny]"));
    }
    
    @Command(names = "snowman", desc = "commands.misc.snowman", usage = "snowman", examples = "snowman")
    public void snowman(final CommandContext ctx) {
        ctx.sendMessage("☃");
    }
    
    @Command(names = {"bootlegcat", "blc"}, desc = "commands.misc.bootlegcat", usage = "bootlegcat",
            examples = "bootlegcat")
    public void bootlegcat(final CommandContext ctx) {
        ctx.sendMessage($(ctx.getLanguage(), "plugins.misc.commands.bootlegcat.base") + "\n\n" +
                Emotes.YES + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.bootlegcat.1") + '\n' +
                Emotes.YES + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.bootlegcat.2") + '\n' +
                Emotes.YES + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.bootlegcat.3") + '\n' +
                Emotes.YES + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.bootlegcat.4") + '\n' +
                '\n' +
                "```\n" +
                "___|____|____|____|____|____|__\n" +
                "__|____|____|____|____|____|___\n" +
                "|____|___|_         ____|____|\n" +
                "___|___|    (\\.-./)  _|____|__\n" +
                "_|____|_  = (^ Y ^) =  _|____|\n" +
                "__|____|___ /`---`\\ __|____|___\n" +
                "|____|____|_U___|_U|____|____\n" +
                "___|____|____|____|____|____|__\n" +
                "__|____|____|____|____|____|____\n" +
                "```");
    }
    
    @Command(
            names = {
                    "clappify",
                    "breadify",
                    "potatofy"
            },
            desc = "commands.misc.clappify",
            usage = {
                    "clappify <text>",
                    "breadify <text>",
                    "potatofy <text>"
            },
            examples = {
                    "clappify don't clap if you don't know how to clappify",
                    "breadify :tomato:",
                    "potatofy tato is the best food"
            }
    )
    public void emojify(final CommandContext ctx) {
        if(ctx.getArgstr() == null || ctx.getArgstr().isEmpty()) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.misc.commands.emojify.invalid")
                    .replace("$category", ctx.getCommand().toLowerCase()));
            return;
        }
        final String emoji;
        switch(ctx.getCommand().toLowerCase()) {
            case "clappify": {
                emoji = "\uD83D\uDC4F";
                break;
            }
            case "breadify": {
                emoji = "\uD83C\uDF5E";
                break;
            }
            case "potatofy": {
                emoji = "\uD83E\uDD54";
                break;
            }
            default: {
                emoji = "\u2753";
                break;
            }
        }
        final StringBuilder s = new StringBuilder(emoji + ' ');
        for(final String e : ctx.getArgstr().split("\\s+")) {
            s.append(e).append(' ').append(emoji).append(' ');
        }
        ctx.sendMessage(ctx.getUser().asMention() + " > " + s.toString().trim()
                .replace("@everyone", "[haha very funny]")
                .replace("@here", "[haha very funny]")
                .replace(ctx.getGuild().id(), "haha no"));
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Command(names = {"help", "?"}, desc = "commands.misc.help", usage = "help", examples = "help")
    public void help(final CommandContext ctx) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.title("Mewna help")
                .field("Dashboard", System.getenv("DOMAIN"), false)
                .field("Commands", System.getenv("DOMAIN") + "/discord/commands", false)
                .field("Support server", "https://discord.gg/UwdDN6r", false)
                .field("Follow us on Twitter!", "https://twitter.com/mewnabot", false)
        ;
        ctx.sendMessage(builder.build());
    }
    
    @Command(names = {"roll", "r"}, desc = "commands.misc.roll", usage = "roll <dice expression>",
            examples = {"roll 5d6", "roll 5d6+2", "roll 1d20 + 5d6 - 10"})
    public void roll(final CommandContext ctx) {
        String message;
        try {
            final DiceNotationExpression expr = parser.parse(ctx.getArgstr());
            message =
                    $(ctx.getLanguage(), "plugins.misc.commands.roll.input") + ": " + ctx.getArgstr() + '\n'
                            + $(ctx.getLanguage(), "plugins.misc.commands.roll.output") + ": " + expr.getValue();
        } catch(final Exception e) {
            message = $(ctx.getLanguage(), "plugins.misc.commands.roll.invalid");
        }
        ctx.sendMessage(message);
    }
    
    @Command(names = "ping", desc = "commands.misc.ping", usage = "ping", examples = "ping")
    public void ping(final CommandContext ctx) {
        final long start = System.currentTimeMillis();
        ctx.getProfiler().section("startPing");
        ctx.sendMessage("Pinging...").thenAccept(msg -> {
            ctx.getProfiler().section("edit");
            final long end = System.currentTimeMillis();
            msg.edit(new MessageOptions().content("Pong! (took " + (end - start) + "ms)").buildMessage()).thenAccept(_msg -> {
                ctx.getProfiler().end();
                if(ctx.getArgstr().equalsIgnoreCase("--profile")
                        && ctx.getUser().id().equalsIgnoreCase("128316294742147072")) {
                    final StringBuilder sb = new StringBuilder("```CSS\n");
                    ctx.getProfiler().sections().forEach(section -> sb.append('[').append(section.name()).append("] ")
                            .append(section.end() - section.start()).append("ms\n"));
                    sb.append('\n');
                    try {
                        sb.append("[worker] ").append(InetAddress.getLocalHost().getHostName()).append('\n');
                    } catch(final UnknownHostException e) {
                        sb.append("[worker] unknown (check sentry)\n");
                        Sentry.capture(e);
                    }
                    
                    sb.append("```");
                    
                    ctx.sendMessage(sb.toString());
                }
            });
        });
    }
    
    /*
    @Command(names = {"page", "blog", "site"}, desc = "commands.misc.page", usage = "page", examples = "page")
    public void blog(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        ctx.sendMessage(System.getenv("DOMAIN") + "/server/" + guild.id());
    }
    */
    
    @Command(names = "chargen", desc = "commands.misc.chargen", usage = "chargen", examples = "chargen")
    public void chargen(final CommandContext ctx) {
        final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.chargen.stats")).append("\n```\n");
        
        for(int i = 0; i < 6; i++) {
            final List<Integer> numbers = new ArrayList<>();
            for(int j = 0; j < 4; j++) {
                numbers.add(ThreadLocalRandom.current().nextInt(6) + 1);
            }
            numbers.sort(Integer::compareTo);
            final int sum = numbers.get(1) + numbers.get(2) + numbers.get(3);
            sb.append(StringUtils.leftPad("" + sum, 2, ' '))
                    .append(" (").append(numbers.get(1)).append(", ").append(numbers.get(2)).append(", ")
                    .append(numbers.get(3)).append(", dropped ").append(numbers.get(0)).append(")\n");
        }
        
        sb.append("```");
        ctx.sendMessage(sb.toString());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Command(names = {"tato", "miner"}, desc = "commands.misc.tato",
            usage = {"tato", "tato help", "tato upgrade [buy <upgrade>]", "tato building [buy <building>]"/*,
                    "tato food [food[,food,...]]"*/},
            examples = {"tato", "tato upgrade", "tato building", "tato help"})
    public void clicker(final CommandContext ctx) {
        final ClickerData data = ctx.getPlayer().getClickerData();
        final List<String> args = ctx.getArgs();
        
        if(!args.isEmpty()) {
            final String subCmd = args.remove(0);
            switch(subCmd.toLowerCase()) {
                case "help": {
                    final String m = "__" + $(ctx.getLanguage(), "plugins.misc.commands.tato.help.1") + "__\n\n" +
                            $(ctx.getLanguage(), "plugins.misc.commands.tato.help.2") + '\n' +
                            $(ctx.getLanguage(), "plugins.misc.commands.tato.help.3") + '\n' +
                            $(ctx.getLanguage(), "plugins.misc.commands.tato.help.4") + '\n' +
                            $(ctx.getLanguage(), "plugins.misc.commands.tato.help.5") + '\n' +
                            $(ctx.getLanguage(), "plugins.misc.commands.tato.help.6");
                    //"- Feed your Mewna Miners with `tato food`.\n" +
                    ctx.sendMessage(m);
                    break;
                }
                case "upgrades":
                case "upgrade": {
                    if(args.isEmpty()) {
                        // List
                        final EmbedBuilder builder = new EmbedBuilder()
                                .description(
                                        "__**" + $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.upgrades") + "**__\n" +
                                                $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.only-once")
                                );
                        for(final ClickerUpgrades u : ClickerUpgrades.values()) {
                            final String body = u.getFlowers() + " " + currencyHelper.getCurrencySymbol(ctx);
                            final String check = data.getUpgrades().contains(u) ? Emotes.YES + ' ' : "";
                            final StringBuilder sb = new StringBuilder();
                            final Item[] items = u.getItems();
                            if(items.length > 0) {
                                sb.append(' ').append($(ctx.getLanguage(), "plugins.misc.commands.tato.and")).append(' ');
                                for(final Item item : items) {
                                    sb.append(item.getEmote()).append(' ');
                                }
                            }
                            
                            builder.field(check + u.getName(), body + sb + "\n*" + u.getDesc() + '*',
                                    false);
                        }
                        ctx.sendMessage(builder.build());
                    } else {
                        final String action = args.remove(0);
                        switch(action) {
                            case "buy":
                            case "b": {
                                if(args.isEmpty()) {
                                    ctx.sendMessage(
                                            Emotes.NO + ' '
                                                    + $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.buy.no-upgrade"));
                                } else {
                                    final String type = args.remove(0);
                                    final ClickerUpgrades upgrade = ClickerUpgrades.byName(type);
                                    if(upgrade == null) {
                                        ctx.sendMessage(
                                                Emotes.NO + ' '
                                                        + $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.buy.invalid-upgrade"));
                                    } else if(data.getUpgrades().contains(upgrade)) {
                                        ctx.sendMessage(
                                                Emotes.NO + ' '
                                                        + $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.buy.already-owned"));
                                    } else {
                                        // We check items first so that we don't take money on a failure
                                        final boolean hasItems = Arrays.stream(upgrade.getItems())
                                                .allMatch(ctx.getPlayer()::hasItem);
                                        if(hasItems) {
                                            final ImmutablePair<Boolean, Long> check = currencyHelper.handlePayment(ctx,
                                                    upgrade.getFlowers() + "", upgrade.getFlowers(),
                                                    upgrade.getFlowers());
                                            if(check.left) {
                                                // Payment worked
                                                Arrays.stream(upgrade.getItems()).forEach(ctx.getPlayer()::removeOneFromInventory);
                                                data.getUpgrades().add(upgrade);
                                                ctx.getPlayer().setClickerData(data);
                                                database().savePlayer(ctx.getPlayer());
                                                ctx.sendMessage(
                                                        Emotes.YES + ' '
                                                                + $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.buy.success")
                                                                .replace("$upgrade", upgrade.getName()));
                                            }
                                        } else {
                                            ctx.sendMessage(
                                                    Emotes.NO + ' '
                                                            + $(ctx.getLanguage(), "plugins.misc.commands.tato.upgrades.buy.need-items"));
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
                case "building":
                case "buildings":
                case "builds":
                case "build": {
                    if(args.isEmpty()) {
                        // List
                        final EmbedBuilder builder = new EmbedBuilder()
                                .description(
                                        "__**" + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.buildings") + "**__\n" +
                                                $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.many-times")
                                );
                        for(final ClickerBuildings u : ClickerBuildings.values()) {
                            final String body = u.getFlowers() + " " + currencyHelper.getCurrencySymbol(ctx);
                            final long amount = data.getBuildings().getOrDefault(u, 0L);
                            final StringBuilder sb = new StringBuilder();
                            final Item[] items = u.getItems();
                            if(items.length > 0) {
                                sb.append(' ').append($(ctx.getLanguage(), "plugins.misc.commands.tato.and")).append(' ');
                                for(final Item item : items) {
                                    sb.append(item.getEmote()).append(' ');
                                }
                            }
                            
                            // lol
                            builder.field(u.getName() + " - " + u.getTier().tierString() + " ("
                                    + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.you-have")
                                    .replace("$count", amount + "")
                                    + ')', body + sb + "\n*"
                                    + u.getDesc() + "*\n"
                                    + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.output")
                                    .replace("$tato", "" + u.getOutput()), false);
                        }
                        ctx.sendMessage(builder.build());
                        break;
                    } else {
                        final String action = args.remove(0);
                        switch(action) {
                            case "buy":
                            case "b": {
                                if(args.isEmpty()) {
                                    ctx.sendMessage(
                                            Emotes.NO + ' '
                                                    + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.buy.no-building"));
                                } else {
                                    final String type = args.remove(0);
                                    final ClickerBuildings building = ClickerBuildings.byName(type);
                                    if(building == null) {
                                        ctx.sendMessage(
                                                Emotes.NO + ' '
                                                        + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.buy.invalid-building"));
                                    } else if(!building.playerHasTier(ctx.getPlayer())) {
                                        ctx.sendMessage(
                                                Emotes.NO + ' '
                                                        + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.buy.tier-too-low"));
                                    } else {
                                        // We check items first so that we don't take money on a failure
                                        final boolean hasItems = Arrays.stream(building.getItems())
                                                .allMatch(ctx.getPlayer()::hasItem);
                                        if(hasItems) {
                                            final ImmutablePair<Boolean, Long> check = currencyHelper.handlePayment(ctx,
                                                    building.getFlowers() + "", building.getFlowers(),
                                                    building.getFlowers());
                                            if(check.left) {
                                                // Payment worked
                                                Arrays.stream(building.getItems()).forEach(ctx.getPlayer()::removeOneFromInventory);
                                                if(data.getBuildings().containsKey(building)) {
                                                    data.getBuildings().put(building, data.getBuildings().get(building) + 1);
                                                } else {
                                                    data.getBuildings().put(building, 1L);
                                                }
                                                ctx.getPlayer().setClickerData(data);
                                                database().savePlayer(ctx.getPlayer());
                                                ctx.sendMessage(
                                                        Emotes.YES + ' '
                                                                + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.buy.success")
                                                                .replace("$building", building.getName()));
                                            }
                                        } else {
                                            ctx.sendMessage(
                                                    Emotes.NO + ' '
                                                            + $(ctx.getLanguage(), "plugins.misc.commands.tato.buildings.buy.need-items"));
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                    break;
                }
                /*
                case "food": {
                    break;
                }
                */
                case "top": {
                    ctx.sendMessage(Emotes.LOADING_ICON
                            + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.tato.top.loading"))
                            .thenAccept(msg -> move(() -> {
                                final String query = "SELECT id, (data->'clickerData'->>'totalClicks') AS clicks FROM players " +
                                        "WHERE (data->'clickerData'->>'totalClicks')::bigint > 0 " +
                                        "ORDER BY (data->'clickerData'->>'totalClicks')::bigint DESC " +
                                        "LIMIT 10;";
                                database().getStore().sql(query, p -> {
                                    final ResultSet resultSet = p.executeQuery();
                                    final Collection<String> rows = new ArrayList<>();
                                    while(resultSet.next()) {
                                        final String id = resultSet.getString("id");
                                        final String clicks = resultSet.getString("clicks");
                                        final User user = DiscordCache.user(id).toCompletableFuture().join();
                                        rows.add("**" + user.username() + '#' + user.discriminator() + "** - " + clicks + " tato");
                                    }
                                    final String res = "__Mewna Miner Leaderboards__\n\n" + String.join("\n", rows);
                                    catnip().rest().channel().editMessage(ctx.getMessage().channelId(), msg.id(), res);
                                });
                            }));
                    break;
                }
                default: {
                    ctx.sendMessage(Emotes.NO + ' '
                            + $(ctx.getLanguage(), "plugins.misc.commands.tato.unknown"));
                    break;
                }
            }
            return;
        }
        
        long last = data.getLastCheck();
        final long now = System.currentTimeMillis();
        if(last == -1L) {
            // Never checked, start them off
            data.setLastCheck(now);
            ctx.getPlayer().setClickerData(data);
            database().savePlayer(ctx.getPlayer());
            last = now;
        }
        // Calculate delta
        final long delta = now - last;
        // MS -> S and floor
        final long deltaSeconds = Math.round(Math.floor(delta / 1000D));
        
        // Compute stats and update in db
        
        data.setLastCheck(now);
        final BigDecimal increase = data.getTatoPerSecond().multiply(BigDecimal.valueOf(deltaSeconds));
        data.setTotalClicks(data.getTotalClicks().add(increase));
        ctx.getPlayer().setClickerData(data);
        database().savePlayer(ctx.getPlayer());
        
        final ClickerTiers tier = data.getTier();
        
        final StringBuilder upgradeSB = new StringBuilder();
        if(data.getUpgrades().isEmpty()) {
            upgradeSB.append($(ctx.getLanguage(), "plugins.misc.commands.tato.no-upgrades")).append('\n');
        } else {
            data.getUpgrades().forEach(e -> upgradeSB.append('.').append(e.getName()).append('\n'));
        }
        final StringBuilder buildingSB = new StringBuilder();
        if(data.getBuildings().isEmpty()) {
            buildingSB.append($(ctx.getLanguage(), "plugins.misc.commands.tato.no-buildings")).append('\n');
        } else {
            data.getBuildings().forEach((b, c) -> buildingSB.append('.').append(b.getName()).append(" x").append(c).append('\n'));
        }
        
        final StringBuilder stats = new StringBuilder("```CSS\n")
                // .append("       [Delta] : ").append(deltaSeconds).append("s\n")
                .append("         [TPS] : ").append(data.getTatoPerSecond()).append(" tato / sec\n")
                .append("  [Total tato] : ").append(data.getTotalClicks().setScale(0, RoundingMode.FLOOR)).append(" tato\n")
                .append("      [Gained] : ").append(increase.setScale(0, RoundingMode.FLOOR)).append(" tato\n")
                .append("[Current Tier] : ").append(tier.tierString()).append(" - ").append(tier.getName()).append("\n\n")
                // oh god why
                .append("[Upgrades]\n").append(upgradeSB.substring(0, upgradeSB.length() - 1)).append("\n\n")
                .append("[Buildings]\n").append(buildingSB.substring(0, buildingSB.length() - 1)).append(" \n")
                .append("```\n\n")
                .append($(ctx.getLanguage(), "plugins.misc.commands.tato.try-help").replace("$command", ctx.getCommand()));
        
        // Finally, display
        
        ctx.sendMessage(
                $(ctx.getLanguage(), "plugins.misc.commands.tato.stats-header")
                        .replace("$target", ctx.getUser().asMention()) + ":\n" + stats);
    }
    
    @Command(names = "dnd", desc = "commands.misc.dnd", usage = {
            "dnd spell <spell name>",
            "dnd spell <class name> <level>",
            "dnd item <item name>",
            "dnd magicitem <item name>",
            "dnd monster <monster name>",
            "dnd race <race name>",
            "dnd feat <feat name>"
    }, examples = {
            "dnd spell fireball",
            "dnd spell wizard 9",
            "dnd item longsword",
            "dnd magicitem Greatsword +1",
            "dnd monster aboleth",
            "dnd race elf (drow)",
            "dnd feat actor"
    })
    public void dnd(final CommandContext ctx) {
        final List<String> args = ctx.getArgs();
        if(args.size() < 2) {
            sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.not-enough-args"));
            return;
        }
        
        final String searchType = args.get(0);
        args.remove(0);
        switch(searchType.toLowerCase()) {
            case "monster":
            case "spell":
            case "magicitem":
            case "item":
            case "feat":
            case "race":
                break;
            default:
                sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search-type"));
        }
        
        final String search = String.join(" ", args);
        if(search.length() < 3) {
            sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search-length"));
        }
        
        switch(searchType) {
            case "monster":
                final List<XMonster> monsters = getMonsters().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(monsters.size() == 1) {
                    try {
                        sendDndEmbedResponse(ctx, sendMonster(monsters.get(0)));
                    } catch(final IllegalStateException e) {
                        ctx.sendMessage(Emotes.NO + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.dnd.monster-too-big"));
                    }
                    return;
                } else if(monsters.size() > 1) {
                    // Check for exact match
                    for(final XMonster monster : monsters) {
                        if(monster.getName().equalsIgnoreCase(search)) {
                            try {
                                sendDndEmbedResponse(ctx, sendMonster(monster));
                            } catch(final IllegalStateException e) {
                                ctx.sendMessage(Emotes.NO + ' ' + $(ctx.getLanguage(), "plugins.misc.commands.dnd.monster-too-big"));
                            }
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.dnd.too-many-matches") + ":\n");
                    for(final XMonster match : monsters) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendDndResponse(ctx, sb.toString());
                    return;
                } else {
                    sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search"));
                    return;
                }
            case "spell":
                // Check if it's a spell list first
                if(SPELL_CLASSES.contains(args.get(0).toLowerCase())) {
                    if(args.size() > 1) {
                        int level = -1;
                        try {
                            level = Integer.parseInt(args.get(1));
                        } catch(final Exception ignored) {
                        }
                        if(level >= 0 && level <= 9) {
                            final List<XSpell> spells = getSpells().stream()
                                    .filter(e -> e.getClasses().toLowerCase().contains(args.get(0).toLowerCase())
                                            && e.getLevel().equalsIgnoreCase(args.get(1)))
                                    .collect(Collectors.toList());
                            final EmbedBuilder builder = new EmbedBuilder()
                                    .title(String.format("%s %s spells", StringUtils.capitalize(args.get(0)), level));
                            final StringBuilder sb = new StringBuilder();
                            for(final XSpell spell : spells) {
                                final String[] split = spell.getClasses().split(", ");
                                String domain = "";
                                for(final String e : split) {
                                    if(e.toLowerCase().contains(args.get(0).toLowerCase())) {
                                        if(e.contains("(")) {
                                            domain += e.split(" ")[1] + ' ';
                                        } else {
                                            domain = "(all)";
                                            break;
                                        }
                                    }
                                }
                                sb.append(" * ").append(spell.getName());
                                if(!domain.isEmpty()) {
                                    sb.append(' ').append(domain.trim());
                                }
                                sb.append('\n');
                            }
                            sendDndEmbedResponse(ctx, builder.field("Spells", sb.toString(), false));
                            return;
                        }
                    }
                }
                final List<XSpell> spells = getSpells().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(spells.isEmpty()) {
                    sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search"));
                    return;
                } else if(spells.size() == 1) {
                    final XSpell spell = spells.get(0);
                    sendDndEmbedResponse(ctx, sendSpell(spell));
                    return;
                } else {
                    for(final XSpell spell : spells) {
                        if(spell.getName().equalsIgnoreCase(search)) {
                            sendDndEmbedResponse(ctx, sendSpell(spell));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.dnd.too-many-matches") + ":\n");
                    for(final XSpell match : spells) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendDndResponse(ctx, sb.toString());
                    return;
                }
            case "magicitem":
                final List<XMagicItem> magicItems = getMagicItems().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(magicItems.isEmpty()) {
                    sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search"));
                    return;
                } else if(magicItems.size() == 1) {
                    final XMagicItem magicItem = magicItems.get(0);
                    sendDndEmbedResponse(ctx, sendMagicItem(magicItem));
                    return;
                } else {
                    for(final XMagicItem magicItem : magicItems) {
                        if(magicItem.getName().equalsIgnoreCase(search)) {
                            sendDndEmbedResponse(ctx, sendMagicItem(magicItem));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.dnd.too-many-matches") + ":\n");
                    for(final XMagicItem match : magicItems) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendDndResponse(ctx, sb.toString());
                    return;
                }
            case "item":
                final List<XItem> items = getItems().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(items.isEmpty()) {
                    sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search"));
                    return;
                } else if(items.size() == 1) {
                    final XItem item = items.get(0);
                    sendDndEmbedResponse(ctx, sendItem(item));
                    return;
                } else {
                    for(final XItem item : items) {
                        if(item.getName().equalsIgnoreCase(search)) {
                            sendDndEmbedResponse(ctx, sendItem(item));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.dnd.too-many-matches") + ":\n");
                    for(final XItem match : items) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendDndResponse(ctx, sb.toString());
                    return;
                }
            case "feat":
                final List<XFeat> feats = getFeats().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(feats.isEmpty()) {
                    sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search"));
                    return;
                } else if(feats.size() == 1) {
                    final XFeat feat = feats.get(0);
                    sendDndEmbedResponse(ctx, sendFeat(feat));
                    return;
                } else {
                    for(final XFeat feat : feats) {
                        if(feat.getName().equalsIgnoreCase(search)) {
                            sendDndEmbedResponse(ctx, sendFeat(feat));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.dnd.too-many-matches") + ":\n");
                    for(final XFeat match : feats) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendDndResponse(ctx, sb.toString());
                    return;
                }
            case "race":
                final List<XRace> races = getRaces().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(races.isEmpty()) {
                    sendDndResponse(ctx, $(ctx.getLanguage(), "plugins.misc.commands.dnd.invalid-search"));
                } else if(races.size() == 1) {
                    sendDndEmbedResponse(ctx, sendRace(races.get(0)));
                } else {
                    for(final XRace race : races) {
                        if(race.getName().equalsIgnoreCase(search)) {
                            sendDndEmbedResponse(ctx, sendRace(race));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.misc.commands.dnd.too-many-matches") + ":\n");
                    for(final XRace match : races) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendDndResponse(ctx, sb.toString());
                }
        }
    }
    
    private void sendDndResponse(final CommandContext ctx, final String res) {
        ctx.sendMessage(res);
    }
    
    private void sendDndEmbedResponse(final CommandContext ctx, final EmbedBuilder builder) {
        ctx.sendMessage(builder.build());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private EmbedBuilder sendRace(final XRace race) {
        final EmbedBuilder builder = new EmbedBuilder().title(race.getName());
        builder.field("Size", race.getSize(), false)
                .field("Speed", race.getSpeed() + "ft", false);
        if(!race.getAbility().equalsIgnoreCase("None")) {
            builder.field("Ability", race.getAbility(), false);
        }
        if(!race.getProficiency().equalsIgnoreCase("None")) {
            builder.field("Proficiency", race.getProficiency(), false);
        }
        if(!race.getTraits().isEmpty()) {
            for(int i = 0; i < race.getTraits().size(); i++) {
                final Trait trait = race.getTraits().get(i);
                builder.field("Trait " + (i + 1), trait.getName(), false);
                for(int j = 0; j < trait.getText().size(); j++) {
                    builder.field("Trait " + (i + 1) + '.' + (j + 1), trait.getText().get(j), false);
                }
            }
        }
        return builder;
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private EmbedBuilder sendFeat(final XFeat feat) {
        final EmbedBuilder builder = new EmbedBuilder().title(feat.getName());
        if(!feat.getPrereq().equalsIgnoreCase("None")) {
            builder.field("Prerequisite", feat.getPrereq(), false);
        }
        if(!feat.getText().isEmpty()) {
            for(int i = 0; i < feat.getText().size(); i++) {
                builder.field("Text " + (i + 1), feat.getText().get(i), false);
            }
        }
        
        if(!feat.getModifiers().isEmpty()) {
            for(int i = 0; i < feat.getModifiers().size(); i++) {
                final Modifier modifier = feat.getModifiers().get(i);
                builder.field("Modifier " + (i + 1), String.format("%s\n%s", modifier.getCategory(), modifier.getText()), false);
            }
        }
        return builder;
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private EmbedBuilder sendItem(final XItem item) {
        final EmbedBuilder builder = new EmbedBuilder().title(item.getName());
        if(!item.getType().equalsIgnoreCase("None")) {
            builder.field("Type", item.getType(), false);
        }
        if(!item.getValue().equalsIgnoreCase("None")) {
            builder.field("Value", item.getValue(), false);
        }
        
        if(!item.getWeight().equalsIgnoreCase("None")) {
            builder.field("Weight", item.getWeight(), false);
        }
        
        if(!item.getDmg1().equalsIgnoreCase("None")) {
            String damage = item.getDmg1();
            if(!item.getDmg2().equalsIgnoreCase("None")) {
                damage += " (" + item.getDmg2() + ')';
            }
            builder.field("Damage", damage, false);
        }
        if(!item.getDmgType().equalsIgnoreCase("None")) {
            builder.field("Damage Type", item.getDmgType(), false);
        }
        if(!item.getProperty().equalsIgnoreCase("None")) {
            builder.field("Properties", item.getProperty(), false);
        }
        if(!item.getRange().equalsIgnoreCase("None")) {
            builder.field("Range", item.getRange(), false);
        }
        if(!item.getRoll().isEmpty()) {
            for(int i = 0; i < item.getRoll().size(); i++) {
                builder.field("Roll " + (i + 1), item.getRoll().get(i), false);
            }
        }
        if(!item.getText().isEmpty()) {
            for(int i = 0; i < item.getText().size(); i++) {
                builder.field("Text " + (i + 1), item.getText().get(i), false);
            }
        }
        return builder;
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private EmbedBuilder sendMagicItem(final XMagicItem magicItem) {
        final EmbedBuilder builder = new EmbedBuilder().title(magicItem.getName());
        builder.field("Type", magicItem.getType(), false);
        for(int i = 0; i < magicItem.getText().size(); i++) {
            builder.field("Text " + (i + 1), magicItem.getText().get(i), false);
        }
        return builder;
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private EmbedBuilder sendSpell(final XSpell spell) {
        final EmbedBuilder builder = new EmbedBuilder().title(spell.getName())
                .field("Type", spell.getSchool(), false)
                .field("Class", spell.getClasses(), false)
                .field("Level", spell.getLevel(), false)
                .field("Target", spell.getRange(), false)
                .field("Casting time", spell.getTime(), false)
                .field("Duration", spell.getDuration(), false)
                .field("Components", spell.getComponents(), false);
        for(int i = 0; i < spell.getText().size(); i++) {
            builder.field("Text " + (i + 1), spell.getText().get(i), false);
        }
        return builder;
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private EmbedBuilder sendMonster(final XMonster monster) {
        final EmbedBuilder builder = new EmbedBuilder().title(monster.getName())
                .field("Type", monster.getType(), false)
                .field("Size", monster.getSize(), false)
                .field("Alignment", monster.getAlignment(), false)
                .field("AC", String.format("%s", monster.getAc()), false)
                .field("HP", String.format("%s\n**%s**", monster.getHp(), monster.getSave()), false)
                .field("Speed", monster.getSpeed(), false)
                .field("Ability scores",
                        String.format("STR: %s\nDEX: %s\nCON: %s\nINT: %s\nWIS: %s\nCHA:%s\n",
                                monster.getStrength(), monster.getDexterity(), monster.getConstitution(),
                                monster.getIntelligence(), monster.getWisdom(), monster.getCharisma()), false)
                .field("Senses", monster.getSenses(), false)
                .field("Languages", monster.getLanguages(), false)
                .field("CR", monster.getCr(), false);
        
        for(int i = 0; i < monster.getActions().size(); i++) {
            final Action action = monster.getActions().get(i);
            builder.field("Action " + (i + 1), action.getName(), false);
            for(int j = 0; j < action.getText().size(); j++) {
                builder.field("Action " + (i + 1) + '.' + (j + 1), action.getText().get(j), false);
            }
            for(int j = 0; j < action.getAttack().size(); j++) {
                builder.field("Action " + (i + 1) + '.' + (j + 1), action.getAttack().get(j), false);
            }
        }
        
        for(int i = 0; i < monster.getTraits().size(); i++) {
            final Trait trait = monster.getTraits().get(i);
            builder.field("Trait " + (i + 1), trait.getName(), false);
            for(int j = 0; j < trait.getText().size(); j++) {
                builder.field("Trait " + (i + 1) + '.' + (j + 1), trait.getText().get(j), false);
            }
        }
        
        for(int i = 0; i < monster.getLegendaries().size(); i++) {
            final Legendary action = monster.getLegendaries().get(i);
            builder.field("Legendary " + (i + 1), action.getName(), false);
            for(int j = 0; j < action.getText().size(); j++) {
                builder.field("Legendary " + (i + 1) + '.' + (j + 1), action.getText().get(j), false);
            }
            for(int j = 0; j < action.getAttack().size(); j++) {
                builder.field("Legendary " + (i + 1) + '.' + (j + 1), action.getAttack().get(j), false);
            }
        }
        return builder;
    }
    
    @Value
    public static final class XRace {
        private final String name;
        private final String size;
        private final String speed;
        private final String ability;
        private final String proficiency;
        private final List<Trait> traits;
    }
    
    @Value
    public static final class Modifier {
        private final String category;
        private final String text;
    }
    
    @Value
    public static final class XMagicItem {
        private final String name;
        private final String type;
        private final String weight;
        private final String dmg1;
        private final String dmgType;
        private final String property;
        private final String rarity;
        private final List<String> text;
        private final List<Modifier> modifiers;
    }
    
    @Value
    public static final class XSpell {
        private final String name;
        private final String level;
        private final String school;
        private final String time;
        private final String range;
        private final String components;
        private final String duration;
        private final String classes;
        private final List<String> text;
        private final List<String> roll;
    }
    
    // This class is NOT lombok'd because of compiler errors
    @Value
    public static final class XMonster {
        private final String name;
        private final String size;
        private final String type;
        private final String alignment;
        private final String ac;
        private final String hp;
        private final String speed;
        private final String strength;
        private final String dexterity;
        private final String constitution;
        private final String intelligence;
        private final String wisdom;
        private final String charisma;
        private final String save;
        private final String skill;
        private final String resist;
        private final String immune;
        private final String conditionImmune;
        private final String senses;
        private final String passive;
        private final String languages;
        private final String cr;
        private final List<Trait> traits;
        private final List<Action> actions;
        private final List<Legendary> legendaries;
        
        @SuppressWarnings("InnerClassTooDeeplyNested")
        @Value
        @RequiredArgsConstructor
        public static final class Action {
            private final String name;
            private final List<String> text;
            private final List<String> attack;
        }
        
        @SuppressWarnings("InnerClassTooDeeplyNested")
        @Value
        @RequiredArgsConstructor
        public static final class Legendary {
            private final String name;
            private final List<String> text;
            private final List<String> attack;
        }
    }
    
    @Value
    public static final class XItem {
        private final String name;
        private final String type;
        private final String value;
        private final String weight;
        private final String dmg1;
        private final String dmg2;
        private final String dmgType;
        private final String property;
        private final String range;
        private final List<String> roll;
        private final List<String> text;
    }
    
    @Value
    public static final class XFeat {
        private final String name;
        private final String prereq;
        private final List<String> text;
        private final List<Modifier> modifiers;
    }
    
    @Value
    public static final class Trait {
        private final String name;
        private final List<String> text;
    }
}
