package com.mewna.plugin;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.cache.ChannelType;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.CommandSettings;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.PluginManager.PluginMetadata;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.message.MessageCreateEvent;
import com.mewna.plugin.metadata.Payment;
import com.mewna.plugin.metadata.Ratelimit;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import com.mewna.util.Time;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection", "WeakerAccess"})
public class CommandManager {
    private static final List<String> PREFIXES;
    private static final String CLIENT_ID = System.getenv("CLIENT_ID");
    
    static {
        PREFIXES = new ArrayList<>(Arrays.asList(Optional.ofNullable(System.getenv("PREFIXES"))
                .orElse("bmew.,bmew ,=").split(",")));
    }
    
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private final Collection<CommandMetadata> commandMetadata = new ArrayList<>();
    private final Map<String, CommandWrapper> commands = new HashMap<>();
    
    public CommandManager(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    public void loadCommandsFromMethod(final Object pluginInstance, final Plugin pluginAnnotation,
                                       final Class<?> pluginClass, final Method m) {
        if(m.getParameterCount() != 1) {
            logger.warn("@Command method '{}' doesn't take a single parameter!", m.getName());
            return;
        }
        if(!m.getParameterTypes()[0].equals(CommandContext.class)) {
            logger.warn("@Command method '{}' doesn't take a single CommandContext parameter!", m.getName());
            return;
        }
        final Command cmd = m.getDeclaredAnnotation(Command.class);
        // Load commands
        for(final String s : cmd.names()) {
            commands.put(s.toLowerCase(), new CommandWrapper(s.toLowerCase(),
                    cmd.aliased() ? cmd.names()[0].toLowerCase() : s.toLowerCase(), cmd.names(),
                    m.getDeclaredAnnotation(Ratelimit.class),
                    m.getDeclaredAnnotation(Payment.class),
                    cmd.desc(),
                    cmd.owner(), pluginInstance, m));
            logger.info("Loaded plugin command '{}' for plugin '{}' ({})", s,
                    pluginAnnotation.name(), pluginClass.getName());
        }
        // Load metadata
        if(!cmd.owner()) {
            if(cmd.aliased()) {
                final List<String> tmp = new ArrayList<>(Arrays.asList(cmd.names()));
                final String name = tmp.remove(0);
                final String[] aliases = tmp.toArray(new String[0]);
                commandMetadata.add(new CommandMetadata(name, pluginAnnotation.name(), cmd.desc(), aliases, cmd.usage(), cmd.examples()));
            } else {
                for(final String name : cmd.names()) {
                    commandMetadata.add(new CommandMetadata(name, pluginAnnotation.name(), cmd.desc(), new String[0], cmd.usage(), cmd.examples()));
                }
            }
        }
    }
    
    @SuppressWarnings("TypeMayBeWeakened")
    private List<String> getAllPrefixes(final Guild guild) {
        //noinspection UnnecessaryLocalVariable
        final List<String> prefixes = new ArrayList<>();
        final BehaviourSettings settings = mewna.getDatabase().getOrBaseSettings(BehaviourSettings.class, guild.getId());
        if(settings.getPrefix() != null && !settings.getPrefix().isEmpty() && settings.getPrefix().length() <= 16) {
            prefixes.add(settings.getPrefix());
        } else {
            prefixes.addAll(PREFIXES);
        }
        prefixes.add("<@" + CLIENT_ID + '>');
        prefixes.add("<@!" + CLIENT_ID + '>');
        return prefixes;
    }
    
    public <T> List<CommandWrapper> getCommandsForPlugin(final Class<T> pluginClass) {
        return commands.values().stream().filter(e -> e.getPlugin().getClass().equals(pluginClass)).collect(Collectors.toList());
    }
    
    public void tryExecCommand(final JSONObject data) {
        try {
            if(!data.has("guild_id") || data.isNull("guild_id")) {
                return;
            }
            final String channelId = data.getString("channel_id");
            // See https://github.com/discordapp/discord-api-docs/issues/582
            // Note this isn't in the docs yet, but should be at some point (hopefully soon)
            final String guildId = data.getString("guild_id");
            final String userId = data.getJSONObject("author").getString("id");
            if(guildId == null) {
                logger.warn("Ignoring DM: user {}, channel {}, message {}, content {}", userId, channelId,
                        data.getString("id"), data.getString("content"));
                return;
            }
            
            final User user = mewna.getCache().getUser(userId);
            if(user == null) {
                logger.error("Got message from unknown (uncached) user {}!?", userId);
                return;
            }
            if(user.isBot()) {
                return;
            }
            
            final Channel channel = mewna.getCache().getChannel(channelId);
            if(channel == null) {
                logger.error("Got message from unknown (uncached) channel {}!?", channelId);
                return;
            }
            if(channel.getType() != ChannelType.GUILD_TEXT.getType()) {
                // Ignore it if it's not a guild message
                return;
            }
            if(channel.getId() == null) {
                return;
            }
            
            // Collect cache data
            final Guild guild = mewna.getCache().getGuild(guildId);
            if(guild == null) {
                logger.error("Got message from unknown (uncached) guild {}!?", guildId);
                return;
            }
            if(guild.getId() == null) {
                return;
            }
            
            final List<User> mentions = new ArrayList<>();
            for(final Object o : data.getJSONArray("mentions")) {
                final JSONObject j = (JSONObject) o;
                // TODO: Build this from the JSON object instead of hitting the cache all the time?
                mentions.add(mewna.getCache().getUser(j.getString("id")));
            }
            
            if(System.getenv("DEBUG") != null) {
                if(!user.getId().equals("128316294742147072")) {
                    /*
                    mewna.getPluginManager().processEvent("MESSAGE_CREATE", new MessageCreateEvent(user, channel, guild, mentions,
                            data.getString("content"), data.getBoolean("mention_everyone")));
                            */
                    return;
                }
            }
            
            // Parse prefixes
            String content = data.getString("content");
            String prefix = null;
            boolean found = false;
            for(final String p : getAllPrefixes(guild)) {
                if(p != null && !p.isEmpty()) {
                    if(content.toLowerCase().startsWith(p.toLowerCase())) {
                        prefix = p;
                        found = true;
                    }
                }
            }
            // TODO: There's gotta be a way to refactor this out into smaller methods...
            if(found) {
                final boolean mentionPrefix = prefix.equals("<@" + CLIENT_ID + '>')
                        || prefix.equals("<@!" + CLIENT_ID + '>');
                if(mentionPrefix) {
                    mentions.removeIf(u -> u.getId().equalsIgnoreCase(CLIENT_ID));
                }
                content = content.substring(prefix.length()).trim();
                if(content.isEmpty()) {
                    return;
                }
                final String[] split = content.split("\\s+", 2);
                final String commandName = split[0];
                final String argstr = split.length > 1 ? split[1] : "";
                // Somehow, even *with* trim()ing, we *still* end up with empty strings in the list
                // This filters them out so it doesn't cause issues elsewhere
                final ArrayList<String> args = Arrays.stream(argstr.trim().split("\\s+")).filter(e -> !e.isEmpty())
                        .collect(Collectors.toCollection(ArrayList::new));
                if(commands.containsKey(commandName)) {
                    final CommandWrapper cmd = commands.get(commandName);
                    if(cmd.isOwner() && !user.getId().equalsIgnoreCase("128316294742147072")) {
                        return;
                    }
                    // Make sure it's not disabled
                    final Optional<PluginMetadata> first = mewna.getPluginManager().getPluginMetadata().stream()
                            .filter(e -> e.getPluginClass().equals(cmd.getPlugin().getClass())).findFirst();
                    if(first.isPresent()) {
                        final Class<? extends PluginSettings> settingsClass = first.get().getSettingsClass();
                        final PluginSettings settings = mewna.getDatabase().getOrBaseSettings(settingsClass, guild.getId());
                        final Map<String, CommandSettings> commandSettings = settings.getCommandSettings();
                        if(commandSettings.containsKey(cmd.getBaseName())) {
                            if(!commandSettings.get(cmd.getBaseName()).isEnabled()) {
                                mewna.getRestJDA().sendMessage(channel, "Sorry, but that command is disabled here.").queue();
                                return;
                            }
                        } else {
                            logger.warn("Adding missing command {} to {} for {}", cmd.getBaseName(), settings.getClass().getSimpleName(), guild.getId());
                            settings.getCommandSettings().put(cmd.getBaseName(), new CommandSettings(true));
                            mewna.getDatabase().saveSettings(settings);
                        }
                    } else {
                        logger.warn("No plugin metadata for command {}!?", cmd.getBaseName());
                    }
                    
                    if(cmd.getRatelimit() != null) {
                        final String baseName = cmd.getBaseName();
                        
                        final String ratelimitKey;
                        switch(cmd.getRatelimit().type()) {
                            case GUILD:
                                ratelimitKey = guildId;
                                break;
                            case GLOBAL:
                                ratelimitKey = user.getId();
                                break;
                            case CHANNEL:
                                ratelimitKey = channelId;
                                break;
                            default:
                                ratelimitKey = user.getId();
                                break;
                        }
                        
                        final ImmutablePair<Boolean, Long> check = mewna.getRatelimiter().checkUpdateRatelimit(user.getId(),
                                baseName + ':' + ratelimitKey,
                                TimeUnit.SECONDS.toMillis(cmd.getRatelimit().time()));
                        if(check.left) {
                            mewna.getRestJDA().sendMessage(channelId,
                                    String.format("You're using that command too fast! Try again in **%s**.",
                                            Time.toHumanReadableDuration(check.right))).queue();
                            return;
                        }
                    }
                    final Player player = mewna.getDatabase().getPlayer(user);
                    Optional<Account> maybeAccount = mewna.getAccountManager().getAccountByLinkedDiscord(user.getId());
                    if(!maybeAccount.isPresent()) {
                        logger.error("No account present for Discord account {}!!!", user.getId());
                        mewna.getAccountManager().createNewDiscordLinkedAccount(player, user);
                        maybeAccount = mewna.getAccountManager().getAccountByLinkedDiscord(user.getId());
                        if(!maybeAccount.isPresent()) {
                            logger.error("No account present for Discord account {} after creation!?", user.getId());
                            return;
                        }
                    } else {
                        final Account account = maybeAccount.get();
                        if(account.isBanned()) {
                            logger.warn("Denying command from banned account {}: {}", account.getId(), account.getBanReason());
                            mewna.getRestJDA().sendMessage(channel, "Banned from Mewna. Reason: " + account.getBanReason()).queue();
                            return;
                        }
                    }
                    long cost = 0L;
                    // Commands may require payment before running
                    final CommandContext paymentCtx = new CommandContext(user, commandName, args, argstr, guild, channel,
                            mentions, player, maybeAccount.get(), 0L, prefix);
                    if(cmd.getPayment() != null) {
                        // By default, try to make the minimum payment
                        String maybePayment = cmd.getPayment().min() + "";
                        if(cmd.getPayment().fromFirstArg()) {
                            // If we calculate the payment from the first argument:
                            // - if there is no argument, go with the minimum payment
                            // - if there is an argument, try to parse it
                            if(args.isEmpty()) {
                                maybePayment = cmd.getPayment().min() + "";
                            } else {
                                maybePayment = args.get(0);
                            }
                        }
                        
                        final ImmutablePair<Boolean, Long> res = mewna.getPluginManager().getCurrencyHelper().handlePayment(paymentCtx,
                                maybePayment, cmd.getPayment().min(), cmd.getPayment().max());
                        // If we can make the payment, set the cost and continue
                        // Otherwise, return early (the payment-handler sends error messages for us)
                        if(res.left) {
                            cost = res.right;
                        } else {
                            return;
                        }
                    }
                    
                    final CommandContext ctx = paymentCtx.toBuilder().cost(cost).build();
                    
                    try {
                        logger.info("Command: {}#{} ({}, account: {}) in {}#{}-{}: {} {}", user.getName(), user.getDiscriminator(),
                                user.getId(), ctx.getAccount().getId(), guild.getId(), channel.getId(), data.getString("id"), commandName, argstr);
                        cmd.getMethod().invoke(cmd.getPlugin(), ctx);
                    } catch(final IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // No prefix found, pass it down as an event
                mewna.getPluginManager().processEvent(EventType.MESSAGE_CREATE, new MessageCreateEvent(user, channel, guild, mentions,
                        data.getString("content"), data.getBoolean("mention_everyone")));
            }
        } catch(final Throwable t) {
            logger.error("Error at high-level command processor:", t);
        }
    }
    
    @Value
    public static final class CommandMetadata {
        private String name;
        private String plugin;
        private String desc;
        private String[] aliases;
        private String[] usage;
        private String[] examples;
    }
    
    @Value
    public static final class CommandWrapper {
        private String name;
        private String baseName;
        private String[] names;
        private Ratelimit ratelimit;
        private Payment payment;
        private String desc;
        private boolean owner;
        private Object plugin;
        private Method method;
    }
}
