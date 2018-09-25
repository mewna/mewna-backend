package com.mewna.plugin.plugins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mewna.cache.entity.User;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    
    private final ExecutorService pool = Executors.newCachedThreadPool();
    
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
    
    @Command(names = "cat", desc = "Get a random cat picture.", usage = "cat", examples = "cat")
    public void cat(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String cat = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://aws.random.cat/meow").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(cat); // meow
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Cat").setImage(json.getString("file")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find cat :(").queue();
        }
    }
    
    @Command(names = "dog", desc = "Get a random dog picture.", usage = "dog", examples = "dog")
    public void dog(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String dog = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://random.dog/woof.json").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(dog); // woof
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Dog").setImage(json.getString("url")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find dog :(").queue();
        }
    }
    
    @Ratelimit(type = RatelimitType.GUILD, time = 5)
    @Command(names = "catgirl", desc = "Get a random (SFW) catgirl picture.", usage = "catgirl", examples = "catgirl")
    public void catgirl(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String catgirl = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://nekos.life/api/neko").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(catgirl); // nya
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Catgirl").setImage(json.getString("neko")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find catgirl :(").queue();
        }
    }
    
    @Command(names = {"rubeface", "rf"}, desc = "Rubeface, the perfect meme for every situation.", usage = "rubeface",
            examples = "rubeface")
    public void rubeface(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Rubeface").setImage(rubeface[getRandom().nextInt(rubeface.length)]).build()).queue();
    }
    
    @Command(names = {"memetext", "bigtext", "mt"}, desc = "Convert stuff to big characters.", usage = "memetext <input>",
            examples = "memetext some text I want to be big!")
    public void memetext(final CommandContext ctx) {
        if(ctx.getArgstr().trim().isEmpty()) {
            getRestJDA().sendMessage(ctx.getChannel(), "You need to give me something to meme!").queue();
            return;
        }
        final StringBuilder sb = new StringBuilder();
        for(final char c : ctx.getArgstr().toCharArray()) {
            sb.append(MEMETEXT_MAP.getOrDefault(c, "" + c)).append(' ');
        }
        getRestJDA().sendMessage(ctx.getChannel(), ctx.getUser().asMention() + ": " + sb.toString().trim()).queue();
    }
    
    @Command(names = {"bootlegcat", "blc"}, desc = "See a bootleg cat, for when mew.cat doesn't work.", usage = "bootlegcat",
            examples = "bootlegcat")
    public void bootlegcat(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel(), "Do any of these describe YOU?\n" +
                '\n' +
                Emotes.YES + " Slow internet speeds?\n" +
                Emotes.YES + " Limited cellular data plan?\n" +
                Emotes.YES + " Too lazy to click a cat image?\n" +
                "`bootlegcat` has you covered! \uD83C\uDF89\uD83C\uDF89\uD83C\uDF89\n" +
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
                "```").queue();
    }
    
    @Command(
            names = {
                    "clappify",
                    "breadify",
                    "potatofy"
            },
            desc = "\uD83D\uDC4F Clap \uD83D\uDC4F some \uD83D\uDC4F text \uD83D\uDC4F (or \uD83C\uDF5E, or \uD83E\uDD54, whatever)",
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
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("You need to give me something to %s!", ctx.getCommand().toLowerCase()))
                    .queue();
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
        getRestJDA().sendMessage(ctx.getChannel(), ctx.getUser().asMention() + " > " + s.toString().trim()).queue();
    }
    
    @Command(names = {"help", "?"}, desc = "Get links to helpful information.", usage = "help", examples = "help")
    public void help(final CommandContext ctx) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Mewna help")
                .addField("Dashboard", System.getenv("DOMAIN"), false)
                .addField("Commands", System.getenv("DOMAIN") + "/discord/commands", false)
                .addField("Support server", "https://discord.gg/UwdDN6r", false)
                .addField("Follow us on Twitter!", "https://twitter.com/mewnabot", false)
                .addField("", "Everything can be enabled / disabled in the dashboard.", false)
        ;
        getRestJDA().sendMessage(ctx.getChannel().getId(), builder.build()).queue();
    }
    
    @Command(names = {"roll", "r"}, desc = "Roll some dice, D&D style", usage = "roll <dice expression>",
            examples = {"roll 5d6", "roll 5d6+2", "roll 1d20 + 5d6 - 10"})
    public void roll(final CommandContext ctx) {
        String message;
        try {
            final DiceNotationExpression expr = parser.parse(ctx.getArgstr());
            
            message = String.format("Input: %s\nOutput: %s", ctx.getArgstr(), expr.getValue());
        } catch(final Exception e) {
            message = "Invalid dice expression.";
        }
        getRestJDA().sendMessage(ctx.getChannel(), message).queue();
    }
    
    @Command(names = "ping", desc = "Check if Mewna's still working.", usage = "ping", examples = "ping")
    public void ping(final CommandContext ctx) {
        final long start = System.currentTimeMillis();
        getRestJDA().sendMessage(ctx.getChannel(), "Pinging...").queue(msg -> {
            final long end = System.currentTimeMillis();
            getRestJDA().editMessage(ctx.getChannel(), msg.getIdLong(),
                    new MessageBuilder().append("Pong! (took ").append(end - start).append("ms)").build()).queue();
        });
    }
    
    @Command(names = {"tato", "miner"}, desc = "Mewna Miner - Like Cookie Clicker, but tato-flavoured.",
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
                    final String m = "__Mewna Miner Help__\n\n" +
                            "- Get this info with `tato help`.\n" +
                            "- Check your stats by running `tato`.\n" +
                            "- View upgrades, or buy them, with `tato upgrade [buy <name>]`.\n" +
                            "- View or buy buildings with `tato build [buy <name>]`.\n" +
                            //"- Feed your Mewna Miners with `tato food`.\n" +
                            "- Remember to check back regularly to get your tato!";
                    getRestJDA().sendMessage(ctx.getChannel(), m).queue();
                    break;
                }
                case "upgrades":
                case "upgrade": {
                    if(args.isEmpty()) {
                        // List
                        final EmbedBuilder builder = new EmbedBuilder();
                        builder.setDescription("__**Upgrades**__\nUpgrades can be bought exactly once.");
                        for(final ClickerUpgrades u : ClickerUpgrades.values()) {
                            final String body = u.getFlowers() + " " + currencyHelper.getCurrencySymbol(ctx);
                            final String check = data.getUpgrades().contains(u) ? Emotes.YES + ' ' : "";
                            final StringBuilder sb = new StringBuilder();
                            final Item[] items = u.getItems();
                            if(items.length > 0) {
                                sb.append(" and ");
                                for(final Item item : items) {
                                    sb.append(item.getEmote()).append(' ');
                                }
                            }
                            
                            builder.addField(check + u.getName(), body + sb + "\n*" + u.getDesc() + '*',
                                    false);
                        }
                        getRestJDA().sendMessage(ctx.getChannel(), builder.build()).queue();
                    } else {
                        final String action = args.remove(0);
                        switch(action) {
                            case "buy":
                            case "b": {
                                if(args.isEmpty()) {
                                    getRestJDA().sendMessage(ctx.getChannel(),
                                            Emotes.NO + " You need to tell me what upgrade you want to buy!").queue();
                                } else {
                                    final String type = args.remove(0);
                                    final ClickerUpgrades upgrade = ClickerUpgrades.byName(type);
                                    if(upgrade == null) {
                                        getRestJDA().sendMessage(ctx.getChannel(),
                                                Emotes.NO + " That's not a real upgrade!").queue();
                                    } else if(data.getUpgrades().contains(upgrade)) {
                                        getRestJDA().sendMessage(ctx.getChannel(),
                                                Emotes.NO + " You already have that upgrade, silly!").queue();
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
                                                getDatabase().savePlayer(ctx.getPlayer());
                                                getRestJDA().sendMessage(ctx.getChannel(),
                                                        Emotes.YES + " You bought the `" + upgrade.getName() + "` upgrade.").queue();
                                            }
                                        } else {
                                            getRestJDA().sendMessage(ctx.getChannel(),
                                                    Emotes.NO + " You don't have the items needed to buy that!").queue();
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
                        final EmbedBuilder builder = new EmbedBuilder();
                        builder.setDescription("__**Buildings**__\nBuildings can be bought many times.");
                        for(final ClickerBuildings u : ClickerBuildings.values()) {
                            final String body = u.getFlowers() + " " + currencyHelper.getCurrencySymbol(ctx);
                            final long amount = data.getBuildings().getOrDefault(u, 0L);
                            final StringBuilder sb = new StringBuilder();
                            final Item[] items = u.getItems();
                            if(items.length > 0) {
                                sb.append(" and ");
                                for(final Item item : items) {
                                    sb.append(item.getEmote()).append(' ');
                                }
                            }
                            
                            // lol
                            builder.addField(u.getName() + " (you have: " + amount + ')', body + sb + "\n*"
                                    + u.getDesc() + "*\nOutput: " + u.getOutput() + " tato per second", false);
                        }
                        getRestJDA().sendMessage(ctx.getChannel(), builder.build()).queue();
                        break;
                    } else {
                        final String action = args.remove(0);
                        switch(action) {
                            case "buy":
                            case "b": {
                                if(args.isEmpty()) {
                                    getRestJDA().sendMessage(ctx.getChannel(),
                                            Emotes.NO + " You need to tell me what upgrade you want to buy!").queue();
                                } else {
                                    final String type = args.remove(0);
                                    final ClickerBuildings building = ClickerBuildings.byName(type);
                                    if(building == null) {
                                        getRestJDA().sendMessage(ctx.getChannel(),
                                                Emotes.NO + " That's not a real building!").queue();
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
                                                getDatabase().savePlayer(ctx.getPlayer());
                                                getRestJDA().sendMessage(ctx.getChannel(),
                                                        Emotes.YES + " You bought a `" + building.getName() + "`.").queue();
                                            }
                                        } else {
                                            getRestJDA().sendMessage(ctx.getChannel(),
                                                    Emotes.NO + " You don't have the items needed to buy that!").queue();
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
                    getRestJDA().sendMessage(ctx.getChannel(), Emotes.LOADING_ICON
                            + " Counting tato (this will take a few seconds)")
                            .queue(msg -> pool.execute(() -> {
                                final String query = "SELECT id, (data->'clickerData'->>'totalClicks') AS clicks FROM players " +
                                        "WHERE (data->'clickerData'->>'totalClicks')::bigint > 0 " +
                                        "ORDER BY (data->'clickerData'->>'totalClicks')::bigint DESC " +
                                        "LIMIT 10;";
                                getDatabase().getStore().sql(query, p -> {
                                    final ResultSet resultSet = p.executeQuery();
                                    final Collection<String> rows = new ArrayList<>();
                                    while(resultSet.next()) {
                                        final String id = resultSet.getString("id");
                                        final String clicks = resultSet.getString("clicks");
                                        final User user = getCache().getUser(id);
                                        rows.add("**" + user.getName() + '#' + user.getDiscriminator() + "** - " + clicks + " tato");
                                    }
                                    final String res = "__Mewna Miner Leaderboards__\n\n" + String.join("\n", rows);
                                    getRestJDA().editMessage(ctx.getChannel(), msg.getIdLong(),
                                            new MessageBuilder().setContent(res).build()).queue();
                                });
                            }));
                    break;
                }
                default: {
                    getRestJDA().sendMessage(ctx.getChannel(), Emotes.NO + " I don't know how to do that...").queue();
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
            getDatabase().savePlayer(ctx.getPlayer());
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
        getDatabase().savePlayer(ctx.getPlayer());
        
        final ClickerTiers tier = data.getTier();
        
        final StringBuilder upgradeSB = new StringBuilder();
        if(data.getUpgrades().isEmpty()) {
            upgradeSB.append("No upgrades!\n");
        } else {
            data.getUpgrades().forEach(e -> upgradeSB.append('.').append(e.getName()).append('\n'));
        }
        final StringBuilder buildingSB = new StringBuilder();
        if(data.getBuildings().isEmpty()) {
            buildingSB.append("No buildings!\n");
        } else {
            data.getBuildings().forEach((b, c) -> buildingSB.append('.').append(b.getName()).append(" x").append(c).append('\n'));
        }
        
        final StringBuilder stats = new StringBuilder("```CSS\n")
                // .append("       [Delta] : ").append(deltaSeconds).append("s\n")
                .append("         [TPS] : ").append(data.getTatoPerSecond()).append(" tato / sec\n")
                .append("  [Total tato] : ").append(data.getTotalClicks().setScale(0, RoundingMode.FLOOR)).append(" tato\n")
                .append("      [Gained] : ").append(increase.setScale(0, RoundingMode.FLOOR)).append(" tato\n")
                .append("[Current Tier] : ").append(tier.name()).append(" - ").append(tier.getName()).append("\n\n")
                // oh god why
                .append("[Upgrades]\n").append(upgradeSB.substring(0, upgradeSB.length() - 1)).append("\n\n")
                .append("[Buildings]\n").append(buildingSB.substring(0, buildingSB.length() - 1)).append(" \n")
                .append("```\n\n")
                .append("(Try `").append(ctx.getPrefix()).append(ctx.getCommand()).append(" help` if you're confused)");
        
        // Finally, display
        getRestJDA().sendMessage(ctx.getChannel(), ctx.getUser().asMention() + "'s tato stats:\n" + stats).queue();
    }
    
    @Command(names = "dnd", desc = "Get useful information for your D&D 5e game.", usage = {
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
            sendResponse(ctx, "Not enough arguments provided.");
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
                sendResponse(ctx, "Invalid search type.");
        }
        
        final String search = String.join(" ", args);
        if(search.length() < 3) {
            sendResponse(ctx, "Search queries must be at least 3 characters long.");
        }
        
        switch(searchType) {
            case "monster":
                final List<XMonster> monsters = getMonsters().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(monsters.size() == 1) {
                    sendEmbedResponse(ctx, sendMonster(monsters.get(0)));
                    return;
                } else if(monsters.size() > 1) {
                    // Check for exact match
                    for(final XMonster monster : monsters) {
                        if(monster.getName().equalsIgnoreCase(search)) {
                            sendEmbedResponse(ctx, sendMonster(monster));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder("Too many possible matches:\n");
                    for(final XMonster match : monsters) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendResponse(ctx, sb.toString());
                    return;
                } else {
                    sendResponse(ctx, "Invalid search");
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
                                    .setTitle(String.format("%s %s spells", StringUtils.capitalize(args.get(0)), level), null);
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
                            sendEmbedResponse(ctx, builder.addField("Spells", sb.toString(), false));
                            return;
                        }
                    }
                }
                final List<XSpell> spells = getSpells().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(spells.isEmpty()) {
                    sendResponse(ctx, "Invalid search");
                    return;
                } else if(spells.size() == 1) {
                    final XSpell spell = spells.get(0);
                    sendEmbedResponse(ctx, sendSpell(spell));
                    return;
                } else {
                    for(final XSpell spell : spells) {
                        if(spell.getName().equalsIgnoreCase(search)) {
                            sendEmbedResponse(ctx, sendSpell(spell));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder("Too many possible matches:\n");
                    for(final XSpell match : spells) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendResponse(ctx, sb.toString());
                    return;
                }
            case "magicitem":
                final List<XMagicItem> magicItems = getMagicItems().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(magicItems.isEmpty()) {
                    sendResponse(ctx, "Invalid search");
                    return;
                } else if(magicItems.size() == 1) {
                    final XMagicItem magicItem = magicItems.get(0);
                    sendEmbedResponse(ctx, sendMagicItem(magicItem));
                    return;
                } else {
                    for(final XMagicItem magicItem : magicItems) {
                        if(magicItem.getName().equalsIgnoreCase(search)) {
                            sendEmbedResponse(ctx, sendMagicItem(magicItem));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder("Too many possible matches:\n");
                    for(final XMagicItem match : magicItems) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendResponse(ctx, sb.toString());
                    return;
                }
            case "item":
                final List<XItem> items = getItems().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(items.isEmpty()) {
                    sendResponse(ctx, "Invalid search");
                    return;
                } else if(items.size() == 1) {
                    final XItem item = items.get(0);
                    sendEmbedResponse(ctx, sendItem(item));
                    return;
                } else {
                    for(final XItem item : items) {
                        if(item.getName().equalsIgnoreCase(search)) {
                            sendEmbedResponse(ctx, sendItem(item));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder("Too many possible matches:\n");
                    for(final XItem match : items) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendResponse(ctx, sb.toString());
                    return;
                }
            case "feat":
                final List<XFeat> feats = getFeats().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(feats.isEmpty()) {
                    sendResponse(ctx, "Invalid search");
                    return;
                } else if(feats.size() == 1) {
                    final XFeat feat = feats.get(0);
                    sendEmbedResponse(ctx, sendFeat(feat));
                    return;
                } else {
                    for(final XFeat feat : feats) {
                        if(feat.getName().equalsIgnoreCase(search)) {
                            sendEmbedResponse(ctx, sendFeat(feat));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder("Too many possible matches:\n");
                    for(final XFeat match : feats) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendResponse(ctx, sb.toString());
                    return;
                }
            case "race":
                final List<XRace> races = getRaces().stream()
                        .filter(e -> e.getName().toLowerCase().contains(search.toLowerCase()))
                        .collect(Collectors.toList());
                if(races.isEmpty()) {
                    sendResponse(ctx, "Invalid search");
                } else if(races.size() == 1) {
                    sendEmbedResponse(ctx, sendRace(races.get(0)));
                } else {
                    for(final XRace race : races) {
                        if(race.getName().equalsIgnoreCase(search)) {
                            sendEmbedResponse(ctx, sendRace(race));
                            return;
                        }
                    }
                    final StringBuilder sb = new StringBuilder("Too many possible matches:\n");
                    for(final XRace match : races) {
                        sb.append(" * ").append(match.getName()).append('\n');
                    }
                    sendResponse(ctx, sb.toString());
                }
        }
    }
    
    private void sendResponse(final CommandContext ctx, final String res) {
        getRestJDA().sendMessage(ctx.getChannel(), res).queue();
    }
    
    private void sendEmbedResponse(final CommandContext ctx, final EmbedBuilder builder) {
        getRestJDA().sendMessage(ctx.getChannel(), builder.build()).queue();
    }
    
    private EmbedBuilder sendRace(final XRace race) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(race.getName(), null);
        builder.addField("Size", race.getSize(), false)
                .addField("Speed", race.getSpeed() + "ft", false);
        if(!race.getAbility().equalsIgnoreCase("None")) {
            builder.addField("Ability", race.getAbility(), false);
        }
        if(!race.getProficiency().equalsIgnoreCase("None")) {
            builder.addField("Proficiency", race.getProficiency(), false);
        }
        if(!race.getTraits().isEmpty()) {
            for(int i = 0; i < race.getTraits().size(); i++) {
                final Trait trait = race.getTraits().get(i);
                builder.addField("Trait " + (i + 1), trait.getName(), false);
                for(int j = 0; j < trait.getText().size(); j++) {
                    builder.addField("Trait " + (i + 1) + '.' + (j + 1), trait.getText().get(j), false);
                }
            }
        }
        return builder;
    }
    
    private EmbedBuilder sendFeat(final XFeat feat) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(feat.getName(), null);
        if(!feat.getPrereq().equalsIgnoreCase("None")) {
            builder.addField("Prerequisite", feat.getPrereq(), false);
        }
        if(!feat.getText().isEmpty()) {
            for(int i = 0; i < feat.getText().size(); i++) {
                builder.addField("Text " + (i + 1), feat.getText().get(i), false);
            }
        }
        
        if(!feat.getModifiers().isEmpty()) {
            for(int i = 0; i < feat.getModifiers().size(); i++) {
                final Modifier modifier = feat.getModifiers().get(i);
                builder.addField("Modifier " + (i + 1), String.format("%s\n%s", modifier.getCategory(), modifier.getText()), false);
            }
        }
        return builder;
    }
    
    private EmbedBuilder sendItem(final XItem item) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(item.getName(), null);
        if(!item.getType().equalsIgnoreCase("None")) {
            builder.addField("Type", item.getType(), false);
        }
        if(!item.getValue().equalsIgnoreCase("None")) {
            builder.addField("Value", item.getValue(), false);
        }
        
        if(!item.getWeight().equalsIgnoreCase("None")) {
            builder.addField("Weight", item.getWeight(), false);
        }
        
        if(!item.getDmg1().equalsIgnoreCase("None")) {
            String damage = item.getDmg1();
            if(!item.getDmg2().equalsIgnoreCase("None")) {
                damage += " (" + item.getDmg2() + ')';
            }
            builder.addField("Damage", damage, false);
        }
        if(!item.getDmgType().equalsIgnoreCase("None")) {
            builder.addField("Damage Type", item.getDmgType(), false);
        }
        if(!item.getProperty().equalsIgnoreCase("None")) {
            builder.addField("Properties", item.getProperty(), false);
        }
        if(!item.getRange().equalsIgnoreCase("None")) {
            builder.addField("Range", item.getRange(), false);
        }
        if(!item.getRoll().isEmpty()) {
            for(int i = 0; i < item.getRoll().size(); i++) {
                builder.addField("Roll " + (i + 1), item.getRoll().get(i), false);
            }
        }
        if(!item.getText().isEmpty()) {
            for(int i = 0; i < item.getText().size(); i++) {
                builder.addField("Text " + (i + 1), item.getText().get(i), false);
            }
        }
        return builder;
    }
    
    private EmbedBuilder sendMagicItem(final XMagicItem magicItem) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(magicItem.getName(), null);
        builder.addField("Type", magicItem.getType(), false);
        for(int i = 0; i < magicItem.getText().size(); i++) {
            builder.addField("Text " + (i + 1), magicItem.getText().get(i), false);
        }
        return builder;
    }
    
    private EmbedBuilder sendSpell(final XSpell spell) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(spell.getName(), null)
                .addField("Type", spell.getSchool(), false)
                .addField("Class", spell.getClasses(), false)
                .addField("Level", spell.getLevel(), false)
                .addField("Target", spell.getRange(), false)
                .addField("Casting time", spell.getTime(), false)
                .addField("Duration", spell.getDuration(), false)
                .addField("Components", spell.getComponents(), false);
        for(int i = 0; i < spell.getText().size(); i++) {
            builder.addField("Text " + (i + 1), spell.getText().get(i), false);
        }
        return builder;
    }
    
    private EmbedBuilder sendMonster(final XMonster monster) {
        final EmbedBuilder builder = new EmbedBuilder().setTitle(monster.getName(), null)
                .addField("Type", monster.getType(), false)
                .addField("Size", monster.getSize(), false)
                .addField("Alignment", monster.getAlignment(), false)
                .addField("AC", String.format("%s", monster.getAc()), false)
                .addField("HP", String.format("%s\n**%s**", monster.getHp(), monster.getSave()), false)
                .addField("Speed", monster.getSpeed(), false)
                .addField("Ability scores",
                        String.format("STR: %s\nDEX: %s\nCON: %s\nINT: %s\nWIS: %s\nCHA:%s\n",
                                monster.getStrength(), monster.getDexterity(), monster.getConstitution(),
                                monster.getIntelligence(), monster.getWisdom(), monster.getCharisma()), false)
                .addField("Senses", monster.getSenses(), false)
                .addField("Languages", monster.getLanguages(), false)
                .addField("CR", monster.getCr(), false);
        
        for(int i = 0; i < monster.getActions().size(); i++) {
            final Action action = monster.getActions().get(i);
            builder.addField("Action " + (i + 1), action.getName(), false);
            for(int j = 0; j < action.getText().size(); j++) {
                builder.addField("Action " + (i + 1) + '.' + (j + 1), action.getText().get(j), false);
            }
            for(int j = 0; j < action.getAttack().size(); j++) {
                builder.addField("Action " + (i + 1) + '.' + (j + 1), action.getAttack().get(j), false);
            }
        }
        
        for(int i = 0; i < monster.getTraits().size(); i++) {
            final Trait trait = monster.getTraits().get(i);
            builder.addField("Trait " + (i + 1), trait.getName(), false);
            for(int j = 0; j < trait.getText().size(); j++) {
                builder.addField("Trait " + (i + 1) + '.' + (j + 1), trait.getText().get(j), false);
            }
        }
        
        for(int i = 0; i < monster.getLegendaries().size(); i++) {
            final Legendary action = monster.getLegendaries().get(i);
            builder.addField("Legendary " + (i + 1), action.getName(), false);
            for(int j = 0; j < action.getText().size(); j++) {
                builder.addField("Legendary " + (i + 1) + '.' + (j + 1), action.getText().get(j), false);
            }
            for(int j = 0; j < action.getAttack().size(); j++) {
                builder.addField("Legendary " + (i + 1) + '.' + (j + 1), action.getAttack().get(j), false);
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
        
        public XMonster(final String name, final String size, final String type, final String alignment, final String ac,
                        final String hp, final String speed, final String strength, final String dexterity,
                        final String constitution, final String intelligence, final String wisdom, final String charisma,
                        final String save, final String skill, final String resist, final String immune,
                        final String conditionImmune, final String senses, final String passive, final String languages,
                        final String cr, final List<Trait> traits, final List<Action> actions,
                        final List<Legendary> legendaries) {
            this.name = name;
            this.size = size;
            this.type = type;
            this.alignment = alignment;
            this.ac = ac;
            this.hp = hp;
            this.speed = speed;
            this.strength = strength;
            this.dexterity = dexterity;
            this.constitution = constitution;
            this.intelligence = intelligence;
            this.wisdom = wisdom;
            this.charisma = charisma;
            this.save = save;
            this.skill = skill;
            this.resist = resist;
            this.immune = immune;
            this.conditionImmune = conditionImmune;
            this.senses = senses;
            this.passive = passive;
            this.languages = languages;
            this.cr = cr;
            this.traits = traits;
            this.actions = actions;
            this.legendaries = legendaries;
        }
        
        public String getName() {
            return name;
        }
        
        public String getSize() {
            return size;
        }
        
        public String getType() {
            return type;
        }
        
        public String getAlignment() {
            return alignment;
        }
        
        public String getAc() {
            return ac;
        }
        
        public String getHp() {
            return hp;
        }
        
        public String getSpeed() {
            return speed;
        }
        
        public String getStrength() {
            return strength;
        }
        
        public String getDexterity() {
            return dexterity;
        }
        
        public String getConstitution() {
            return constitution;
        }
        
        public String getIntelligence() {
            return intelligence;
        }
        
        public String getWisdom() {
            return wisdom;
        }
        
        public String getCharisma() {
            return charisma;
        }
        
        public String getSave() {
            return save;
        }
        
        public String getSkill() {
            return skill;
        }
        
        public String getResist() {
            return resist;
        }
        
        public String getImmune() {
            return immune;
        }
        
        public String getConditionImmune() {
            return conditionImmune;
        }
        
        public String getSenses() {
            return senses;
        }
        
        public String getPassive() {
            return passive;
        }
        
        public String getLanguages() {
            return languages;
        }
        
        public String getCr() {
            return cr;
        }
        
        public List<Trait> getTraits() {
            return traits;
        }
        
        public List<Action> getActions() {
            return actions;
        }
        
        @SuppressWarnings("WeakerAccess")
        public List<Legendary> getLegendaries() {
            return legendaries;
        }
        
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
