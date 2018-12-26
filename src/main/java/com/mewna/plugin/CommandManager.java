package com.mewna.plugin;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import com.mewna.data.CommandSettings;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.event.discord.DiscordMessageCreate;
import com.mewna.plugin.PluginManager.PluginMetadata;
import com.mewna.plugin.metadata.Payment;
import com.mewna.plugin.metadata.Ratelimit;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import com.mewna.plugin.plugins.settings.EconomySettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.Time;
import io.sentry.Sentry;
import io.vertx.core.Future;
import lombok.Getter;
import lombok.Value;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mewna.util.Translator.$;

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
                    cmd.owner() || pluginAnnotation.owner(), pluginInstance, m));
            logger.info("Loaded plugin command '{}' for plugin '{}' ({})", s,
                    pluginAnnotation.name(), pluginClass.getName());
        }
        // Load metadata
        if(!cmd.owner() && !pluginAnnotation.owner()) {
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
    private CompletableFuture<List<String>> getAllPrefixes(final Guild guild) {
        final Future<List<String>> future = Future.future();
        
        mewna.database().getOrBaseSettings(BehaviourSettings.class, guild.id()).exceptionally(e -> {
            Sentry.capture(e);
            return null;
        }).thenAccept(settings -> {
            final List<String> prefixes = new ArrayList<>();
            if(settings.getPrefix() != null && !settings.getPrefix().isEmpty() && settings.getPrefix().length() <= 16) {
                prefixes.add(settings.getPrefix());
            } else {
                prefixes.addAll(PREFIXES);
            }
            prefixes.add("<@" + CLIENT_ID + '>');
            prefixes.add("<@!" + CLIENT_ID + '>');
            future.complete(prefixes);
        });
        
        return VertxCompletableFuture.from(mewna.vertx(), future);
    }
    
    public <T> List<CommandWrapper> getCommandsForPlugin(final Class<T> pluginClass) {
        return commands.values().stream().filter(e -> e.getPlugin().getClass().equals(pluginClass)).collect(Collectors.toList());
    }
    
    public void tryExecCommand(final DiscordMessageCreate event) {
        try {
            final User user = event.message().author();
            if(user.bot()) {
                return;
            }
    
            final Guild guild = event.guild();
            final String channelId = event.message().channelId();
            
            if(System.getenv("DEBUG") != null) {
                if(!user.id().equals("128316294742147072")) {
                    mewna.pluginManager().processEvent(Raw.MESSAGE_CREATE, event);
                    return;
                }
            }
            
            // ENTER THE REALM OF THE ASYNC HELL
            
            getAllPrefixes(guild).exceptionally(e -> {
                Sentry.capture(e);
                return Collections.emptyList();
            }).thenAccept(prefixes -> {
                // Parse prefixes
                final String content = event.message().content();
                String prefix = null;
                boolean found = false;
                for(final String p : prefixes) {
                    if(p != null && !p.isEmpty()) {
                        if(content.toLowerCase().startsWith(p.toLowerCase())) {
                            prefix = p;
                            found = true;
                        }
                    }
                }
                // TODO: There's gotta be a way to refactor this out into smaller methods...
                final List<User> mentions = new ArrayList<>(event.message().mentionedUsers());
                if(found) {
                    parseCommand(user, guild, mentions, prefix, content, channelId, event);
                } else {
                    // No prefix found, pass it down as an event
                    mewna.pluginManager().processEvent(Raw.MESSAGE_CREATE, event);
                }
            });
        } catch(final Throwable t) {
            Sentry.capture(t);
            logger.error("Error at high-level command processor:", t);
        }
    }
    
    private void parseCommand(final User user, final Guild guild, final List<User> mentions, final String prefix,
                              String content, final String channelId, final DiscordMessageCreate event) {
        final boolean mentionPrefix = prefix.equals("<@" + CLIENT_ID + '>')
                || prefix.equals("<@!" + CLIENT_ID + '>');
        if(mentionPrefix) {
            mentions.removeIf(u -> u.id().equalsIgnoreCase(CLIENT_ID));
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
        final List<String> args = Arrays.stream(argstr.trim().split("\\s+")).filter(e -> !e.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        if(commands.containsKey(commandName)) {
            executeCommand(user, guild, mentions, prefix, channelId, event, commandName, argstr, args);
        }
    }
    
    private void executeCommand(final User user, final Guild guild, final List<User> mentions, final String prefix,
                                final String channelId, final DiscordMessageCreate event, final String commandName,
                                final String argstr, final List<String> args) {
        final CommandWrapper cmd = commands.get(commandName);
        if(cmd.isOwner() && !user.id().equalsIgnoreCase("128316294742147072")) {
            return;
        }
        // Make sure it's not disabled
        final Optional<PluginMetadata> first = mewna.pluginManager().getPluginMetadata().stream()
                .filter(e -> e.getPluginClass().equals(cmd.getPlugin().getClass())).findFirst();
        
        final Future<Boolean> canExec = Future.future();
        
        if(first.isPresent()) {
            final Class<? extends PluginSettings> settingsClass = first.get().getSettingsClass();
            mewna.database().getOrBaseSettings(settingsClass, guild.id()).thenAccept(settings -> {
                final Map<String, CommandSettings> commandSettings = settings.getCommandSettings();
                if(commandSettings.containsKey(cmd.getBaseName())) {
                    if(!commandSettings.get(cmd.getBaseName()).isEnabled()) {
                        mewna.catnip().rest().channel().sendMessage(channelId,
                                $(mewna.database().language(guild.id()), "plugins.disabled-command"));
                        canExec.complete(false);
                    } else {
                        canExec.complete(true);
                    }
                } else {
                    logger.warn("Adding missing command {} to {} for {}", cmd.getBaseName(), settings.getClass().getSimpleName(), guild.id());
                    settings.getCommandSettings().put(cmd.getBaseName(), new CommandSettings(true));
                    mewna.database().saveSettings(settings);
                    canExec.complete(true);
                }
            }).exceptionally(e -> {
                Sentry.capture(e);
                logger.warn("Couldn't load plugin settings:", e);
                canExec.complete(false);
                return null;
            });
        } else {
            logger.warn("No plugin metadata for command {}!?", cmd.getBaseName());
            canExec.complete(true);
        }
        
        VertxCompletableFuture.from(mewna.vertx(), canExec).thenAccept(b -> {
            if(b) {
                if(cmd.getRatelimit() != null) {
                    final String baseName = cmd.getBaseName();
                    
                    final String ratelimitKey;
                    switch(cmd.getRatelimit().type()) {
                        case GUILD:
                            ratelimitKey = guild.id();
                            break;
                        case GLOBAL:
                            ratelimitKey = user.id();
                            break;
                        case CHANNEL:
                            ratelimitKey = channelId;
                            break;
                        default:
                            ratelimitKey = user.id();
                            break;
                    }
                    
                    final ImmutablePair<Boolean, Long> check = mewna.ratelimiter().checkUpdateRatelimit(user.id(),
                            baseName + ':' + ratelimitKey,
                            TimeUnit.SECONDS.toMillis(cmd.getRatelimit().time()));
                    if(check.left) {
                        mewna.statsClient().count("discord.backend.commands.ratelimit", 1,
                                "name:" + cmd.getName());
                        mewna.catnip().rest().channel().sendMessage(channelId,
                                $(mewna.database().language(guild.id()), "plugins.ratelimited-command")
                                        .replace("$time", Time.toHumanReadableDuration(check.right)));
                        return;
                    }
                }
                final Player player = mewna.database().getPlayer(user);
                Optional<Account> maybeAccount = mewna.accountManager().getAccountByLinkedDiscord(user.id());
                if(!maybeAccount.isPresent()) {
                    logger.error("No account present for Discord account {}!!!", user.id());
                    //Sentry.capture("No account present for Discord account: " + user.id());
                    mewna.accountManager().createNewDiscordLinkedAccount(player, user);
                    maybeAccount = mewna.accountManager().getAccountByLinkedDiscord(user.id());
                    if(!maybeAccount.isPresent()) {
                        logger.error("No account present for Discord account {} after creation!?", user.id());
                        Sentry.capture("No account present for Discord account despite creation: " + user.id());
                        return;
                    }
                } else {
                    final Account account = maybeAccount.get();
                    if(account.isBanned()) {
                        mewna.statsClient().count("discord.backend.commands.banned", 1);
                        logger.warn("Denying command from banned account {}: {}", account.id(), account.banReason());
                        // TODO: I18n this
                        mewna.catnip().rest().channel().sendMessage(channelId, Emotes.NO + " Banned from Mewna. Reason: "
                                + account.banReason());
                        return;
                    }
                }
                
                final Optional<Account> finalMaybeAccount = maybeAccount;
                mewna.database().getOrBaseSettings(EconomySettings.class, guild.id()).thenAccept(settings -> {
                    long cost = 0L;
                    // Commands may require payment before running
                    final CommandContext paymentCtx = new CommandContext(user, commandName, args, argstr, guild, event.message(),
                            mentions, player, finalMaybeAccount.get(), 0L, prefix, mewna.database().language(guild.id()),
                            settings.getCurrencySymbol());
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
                        
                        final ImmutablePair<Boolean, Long> res = mewna.pluginManager().getCurrencyHelper()
                                .handlePayment(paymentCtx, maybePayment, cmd.getPayment().min(), cmd.getPayment().max());
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
                        logger.info("Command: {}#{} ({}, account: {}) in {}#{}-{}: {} {}", user.username(), user.discriminator(),
                                user.id(), ctx.getAccount().id(), guild.id(), channelId, event.message().id(), commandName, argstr);
                        mewna.statsClient().count("discord.backend.commands.run", 1, "name:" + cmd.getName());
                        cmd.getMethod().invoke(cmd.getPlugin(), ctx);
                    } catch(final IllegalAccessException | InvocationTargetException e) {
                        Sentry.capture(e);
                        e.printStackTrace();
                    }
                });
            }
        });
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
