package com.mewna.plugin.commands;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import com.mewna.data.CommandSettings;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.event.discord.DiscordMessageCreate;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.PluginManager.PluginMetadata;
import com.mewna.plugin.commands.annotations.*;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import com.mewna.plugin.plugins.settings.EconomySettings;
import com.mewna.plugin.plugins.settings.SecretSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.Profiler;
import com.mewna.util.Time;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;
import static com.mewna.util.MewnaFutures.block;
import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings({"unused", "FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection", "WeakerAccess", "OverlyCoupledClass"})
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
            logger.error("@Command method '{}' doesn't take a single parameter!", m.getName());
            return;
        }
        if(!m.getParameterTypes()[0].equals(CommandContext.class)) {
            logger.error("@Command method '{}' doesn't take a single CommandContext parameter!", m.getName());
            return;
        }
        final Command cmd = m.getDeclaredAnnotation(Command.class);
        final boolean isStaff = m.isAnnotationPresent(Staff.class) || pluginClass.isAnnotationPresent(Staff.class);
        final boolean isOwner = m.isAnnotationPresent(Owner.class) || pluginClass.isAnnotationPresent(Owner.class);
        final boolean isMulti = m.isAnnotationPresent(Multi.class);
        // Load commands
        for(final String s : cmd.names()) {
            commands.put(s.toLowerCase(),
                    new CommandWrapper(
                            s.toLowerCase(),
                            !isMulti ? cmd.names()[0].toLowerCase() : s.toLowerCase(), cmd.names(),
                            m.getDeclaredAnnotation(Ratelimit.class),
                            m.getDeclaredAnnotation(Payment.class),
                            cmd.desc(),
                            isOwner, isStaff,
                            pluginInstance, m
                    )
            );
            // logger.info("Loaded plugin command '{}' for plugin '{}' ({})", s, pluginAnnotation.name(), pluginClass.getName());
        }
        // Load metadata
        if(!isOwner && !isStaff) { // ignore secret commands
            if(!isMulti) {
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
    
    private CompletableFuture<List<String>> getAllPrefixes(final Guild guild, final Profiler profiler) {
        return mewna.database().getOrBaseSettings(BehaviourSettings.class, guild.id()).thenApply(settings -> {
            profiler.section("prefixesCleanup");
            final List<String> prefixes = new ArrayList<>();
            if(settings.getPrefix() != null && !settings.getPrefix().isEmpty() && settings.getPrefix().length() <= 16) {
                prefixes.add(settings.getPrefix());
            } else {
                prefixes.addAll(PREFIXES);
            }
            profiler.section("prefixesAddMentions");
            prefixes.add("<@" + CLIENT_ID + '>');
            prefixes.add("<@!" + CLIENT_ID + '>');
            return prefixes;
        }).exceptionally(e -> {
            profiler.section("prefixesError");
            Sentry.capture(e);
            return Collections.emptyList();
        });
    }
    
    public <T> List<CommandWrapper> getCommandsForPlugin(final Class<T> pluginClass) {
        return commands.values().stream().filter(e -> e.getPlugin().getClass().equals(pluginClass)).collect(Collectors.toList());
    }
    
    public void tryExecCommand(final DiscordMessageCreate event) {
        try {
            final Profiler profiler = new Profiler("basicChecks", event.received());
            final User user = event.message().author();
            if(user.bot()) {
                return;
            }
            
            final Guild guild = event.guild();
            final String channelId = event.message().channelId();
            
            if(System.getenv("DEBUG") != null) {
                if(!user.id().equals("128316294742147072") && !user.id().equalsIgnoreCase("456704606605672449")) {
                    mewna.pluginManager().processEvent(Raw.MESSAGE_CREATE, event);
                    return;
                }
            }
            profiler.section("prefixesStart");
            
            // ENTER THE REALM OF THE ASYNC HELL
            
            move(() -> {
                List<String> prefixes;
                try {
                    prefixes = block(getAllPrefixes(guild, profiler));
                } catch(final Exception e) {
                    Sentry.capture(e);
                    prefixes = Collections.emptyList();
                }
                // Empty prefixes == no prefixes found because db errors
                if(!prefixes.isEmpty()) {
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
                    final List<User> mentions = new ArrayList<>(event.message().mentionedUsers());
                    if(found) {
                        parseCommand(event, prefix, profiler);
                    } else {
                        // No prefix found, pass it down as an event
                        mewna.pluginManager().processEvent(Raw.MESSAGE_CREATE, event);
                    }
                }
            });
        } catch(final Exception e) {
            Sentry.capture(e);
            logger.error("Error at high-level command processor:", e);
        }
    }
    
    private void parseCommand(final DiscordMessageCreate event, final String prefix, final Profiler profiler) {
        final User user = event.user();
        final Guild guild = event.guild();
        final List<User> mentions = event.message().mentionedUsers();
        final String channelId = event.message().channelId();
        final String content = event.message().content();
        
        profiler.section("parse");
        final boolean mentionPrefix = prefix.equals("<@" + CLIENT_ID + '>')
                || prefix.equals("<@!" + CLIENT_ID + '>');
        final String cmdStr = content.substring(prefix.length()).trim();
        if(cmdStr.isEmpty()) {
            return;
        }
        final String[] split = cmdStr.split("\\s+", 2);
        final String commandName = split[0];
        final String argstr = split.length > 1 ? split[1] : "";
        if(mentionPrefix && !argstr.contains("<@" + CLIENT_ID + '>') && !argstr.contains("<@!" + CLIENT_ID + '>')) {
            mentions.removeIf(u -> u.id().equalsIgnoreCase(CLIENT_ID));
        }
        // Somehow, even *with* trim()ing, we *still* end up with empty strings in the list
        // This filters them out so it doesn't cause issues elsewhere
        final List<String> args = Arrays.stream(argstr.trim().split("\\s+")).filter(e -> !e.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        if(commands.containsKey(commandName)) {
            executeCommand(event, mentions, prefix, commandName, argstr, args, profiler);
        }
    }
    
    private void executeCommand(final DiscordMessageCreate event, final List<User> mentions, final String prefix,
                                final String commandName, final String argStr, final List<String> args,
                                final Profiler profiler) {
        final User user = event.user();
        final Guild guild = event.guild();
        final String channelId = event.message().channelId();
        // We don't derive mentions from the event mainly so that we don't end
        // up accidentally including the bot in the mentions when it's only
        // used as a mention prefix.
        
        profiler.section("metadata");
        final CommandWrapper cmd = commands.get(commandName);
        if(cmd.isOwner() && !user.id().equalsIgnoreCase("128316294742147072")) {
            return;
        }
        if(cmd.isStaff() && !(event.member().roleIds().contains("406558129699160066") // TODO: Don't hardcode staff role...
                || user.id().equalsIgnoreCase("128316294742147072"))) {
            return;
        }
        // Make sure it's not disabled
        final Optional<PluginMetadata> first = mewna.pluginManager().getPluginMetadata().stream()
                .filter(e -> e.getPluginClass().equals(cmd.getPlugin().getClass())).findFirst();
        
        if(canExec(cmd, first, guild, channelId, profiler)) {
            profiler.section("ratelimit");
            if(cmd.getRatelimit() != null) {
                final var check = checkRatelimit(cmd, user, guild, channelId);
                if(check.left) {
                    mewna.statsClient().count("discord.backend.commands.ratelimit", 1,
                            "name:" + cmd.getName());
                    mewna.catnip().rest().channel().sendMessage(channelId,
                            $(mewna.database().language(guild.id()), "plugins.ratelimited-command")
                                    .replace("$time", Time.toHumanReadableDuration(check.right)));
                    return;
                }
            }
            profiler.section("player");
            
            final Player player;
            try {
                player = block(mewna.database().getPlayer(user, profiler));
            } catch(final Exception e) {
                logger.error("Couldn't fetch player {}!", user.id(), e);
                Sentry.capture(e);
                return;
            }
            profiler.section("account");
            // Fill out accounts if they don't exist
            // This is more of just a safety / sanity check than
            // anything.
            // In general, this should never be reached since
            // getPlayer() will populate an account if it also has
            // to create a player object.
            // ie. this will only really be necessary for ex.
            // some levels imports.
            Optional<Account> maybeAccount = mewna.accountManager().getAccountByLinkedDiscord(user.id());
            if(maybeAccount.isEmpty()) {
                logger.error("No account present for Discord account {}!!!", user.id());
                //Sentry.capture("No account present for Discord account: " + user.id());
                mewna.accountManager().createNewDiscordLinkedAccount(player, user);
                maybeAccount = mewna.accountManager().getAccountByLinkedDiscord(user.id());
                if(maybeAccount.isEmpty()) {
                    logger.error("No account present for Discord account {} after creation!?", user.id());
                    Sentry.capture("No account present for Discord account despite creation: " + user.id());
                    return;
                }
            } else {
                final Account account = maybeAccount.get();
                if(!account.id().equalsIgnoreCase(account.discordAccountId())) {
                    mewna.database().saveAccount(account.toBuilder().id(account.discordAccountId()).build());
                }
                if(account.banned()) {
                    mewna.statsClient().count("discord.backend.commands.banned", 1);
                    logger.warn("Denying command from banned account {}: {}", account.id(), account.banReason());
                    mewna.catnip().rest().channel().sendMessage(channelId, Emotes.NO + ' '
                            + $(mewna.database().language(guild.id()), "commands.banned") + ' ' + account.banReason());
                    return;
                }
            }
            
            final Optional<Account> finalMaybeAccount = maybeAccount;
            profiler.section("paymentStart");
            
            final EconomySettings settings = block(mewna.database().getOrBaseSettings(EconomySettings.class, guild.id()));
            
            profiler.section("paymentSettings");
            long cost = 0L;
            // Commands may require payment before running
            final CommandContext paymentCtx = new CommandContext(user, commandName, args, argStr, guild, event.message(),
                    mentions, player, finalMaybeAccount.get(), 0L, prefix, mewna.database().language(guild.id()),
                    settings.getCurrencySymbol(), profiler, event);
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
                profiler.section("paymentProcessing");
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
            
            // Add cost to the context
            final CommandContext ctx = paymentCtx.toBuilder().cost(cost).build();
            
            profiler.section("exec");
            logger.info("Command: {}#{} ({}) in {}#{}-{}: {} {}", user.username(), user.discriminator(),
                    user.id(), guild.id(), channelId, event.message().id(), commandName, argStr);
            mewna.statsClient().count("discord.backend.commands.run", 1,
                    "name:" + cmd.getName());
            
            try {
                cmd.getMethod().invoke(cmd.getPlugin(), ctx);
            } catch(final IllegalAccessException | InvocationTargetException e) {
                Sentry.capture(e);
                e.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean canExec(final CommandWrapper cmd, final Optional<PluginMetadata> maybeMetadata, final Guild guild,
                            final String channelId, final Profiler profiler) {
        profiler.section("canExec");
        boolean canExec;
        if(maybeMetadata.isPresent()) {
            final Class<? extends PluginSettings> settingsClass = maybeMetadata.get().getSettingsClass();
            final PluginSettings settings = block(mewna.database().getOrBaseSettings(settingsClass, guild.id()));
            
            try {
                if(settings instanceof SecretSettings) {
                    canExec = true;
                } else {
                    final Map<String, CommandSettings> commandSettings = settings.getCommandSettings();
                    if(commandSettings.containsKey(cmd.getBaseName())) {
                        if(!commandSettings.get(cmd.getBaseName()).isEnabled()) {
                            mewna.catnip().rest().channel().sendMessage(channelId,
                                    $(mewna.database().language(guild.id()), "plugins.disabled-command"));
                            canExec = false;
                        } else {
                            canExec = true;
                        }
                    } else {
                        logger.warn("Adding missing command {} to {} for {}", cmd.getBaseName(), settings.getClass().getSimpleName(), guild.id());
                        settings.getCommandSettings().put(cmd.getBaseName(), new CommandSettings(true));
                        mewna.database().saveSettings(settings);
                        canExec = true;
                    }
                }
            } catch(final Exception e) {
                Sentry.capture(e);
                logger.warn("Couldn't load plugin settings:", e);
                canExec = false;
            }
        } else if(!cmd.isOwner() && !cmd.isStaff()) {
            logger.warn("No plugin metadata for command {}!?", cmd.getBaseName());
            Sentry.capture("No plugin metadata for command " + cmd.getBaseName() + " loaded from class " + cmd.getPlugin().getClass().getName());
            canExec = false;
        } else {
            canExec = true;
        }
        return canExec;
    }
    
    private ImmutablePair<Boolean, Long> checkRatelimit(final CommandWrapper cmd, final User user, final Guild guild,
                                                        final String channelId) {
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
        
        return mewna.ratelimiter().checkUpdateRatelimit(user.id(),
                baseName + ':' + ratelimitKey,
                TimeUnit.SECONDS.toMillis(cmd.getRatelimit().time()));
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
        private boolean staff;
        private Object plugin;
        private Method method;
    }
}
