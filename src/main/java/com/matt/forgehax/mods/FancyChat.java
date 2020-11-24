package com.matt.forgehax.mods;

import com.google.common.collect.Sets;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.command.Options;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.command.exception.CommandExecuteException;
import com.matt.forgehax.util.entry.ChatCommandEntry;
import com.matt.forgehax.util.entry.CommandPrefixEntry;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import joptsimple.internal.Strings;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;
import java.util.stream.Collectors;

import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.util.text.CapitalizationHelper.makeWave;
import static com.matt.forgehax.util.text.CapitalizationHelper.randomCase;
import static com.matt.forgehax.util.text.CircledChatHelper.toCircled;
import static com.matt.forgehax.util.text.FullwidthHelper.toFullwidthFancyChat;
import static com.matt.forgehax.util.text.LeetspeakHelper.toLeetspeak;
import static com.matt.forgehax.util.text.MorseHelper.toMorse;
import static com.matt.forgehax.util.text.ParenthesizedHelper.toParenthesized;
import static com.matt.forgehax.util.text.PhoneticHelper.toPhoneticFancyChat;

/*
 * Originally made by Babbaj.
 * Reworked by Fraaz on the 3rd of November 2020.
 * Check com.matt.forgehax.util.text!
 * Now it's less cancerous to read and it doesn't have mismatched characters. Plus, it has some new features.
 */

@RegisterMod
public class FancyChat extends ToggleMod {
    public FancyChat() {
        super(Category.CHAT, "FancyChat", false, "Replaces the text you send with fancier unicode equivalents");
    }

    private enum FONT {
        DEFAULT, FULLWIDTH, CIRCLE, PARENTHESES, SMALL, LEET, MORSE
    }

    private enum CAPS {
        NONE, WAVE, RANDOMCASE
    }

    private final Setting<FONT> font =
            getCommandStub()
                    .builders()
                    .<FONT>newSettingEnumBuilder()
                    .name("font")
                    .description("Font to use")
                    .defaultTo(FONT.DEFAULT)
                    .build();

    private final Setting<CAPS> caps =
            getCommandStub()
                    .builders()
                    .<CAPS>newSettingEnumBuilder()
                    .name("caps")
                    .description("Capitalization modifier to use (if the font allows it)")
                    .defaultTo(CAPS.NONE)
                    .build();

    public final Setting<Boolean> consoleSpam =
            getCommandStub()
                    .builders()
                    .<Boolean>newSettingBuilder()
                    .name("consoleSpam")
                    .description("Spams the console with random characters in an attempt to hide what you're saying")
                    .defaultTo(false)
                    .build();

    private final Options<CommandPrefixEntry> commandPrefixes =
            getCommandStub()
                    .builders()
                    .<CommandPrefixEntry>newOptionsBuilder()
                    .name("prefix")
                    .description("Prefixes that will mark the message as a command")
                    .factory(CommandPrefixEntry::new)
                    .supplier(Sets::newConcurrentHashSet)
                    .build();

    private final Options<ChatCommandEntry> commands =
            getCommandStub()
                    .builders()
                    .<ChatCommandEntry>newOptionsBuilder()
                    .name("command")
                    .description("Commands that will bypass FancyChat")
                    .factory(ChatCommandEntry::new)
                    .supplier(Sets::newConcurrentHashSet)
                    .build();

    @Override
    protected void onLoad() {
        commandPrefixes
                .builders()
                .newCommandBuilder()
                .name("add")
                .description("Adds a new prefix")
                .processor(
                        data -> {
                            data.requiredArguments(1);
                            final String prefix = data.getArgumentAsString(0).toLowerCase();
                            if (Strings.isNullOrEmpty(prefix))
                                throw new CommandExecuteException("Empty or null argument");
                            commandPrefixes.add(new CommandPrefixEntry(prefix));
                            data.write(String.format("Added prefix \"%s\"", prefix));
                            data.markSuccess();
                        })
                .build();
        commandPrefixes
                .builders()
                .newCommandBuilder()
                .name("remove")
                .description("Removes a prefix")
                .processor(
                        data -> {
                            data.requiredArguments(1);
                            final String prefix = data.getArgumentAsString(0);
                            boolean check = false;
                            for(CommandPrefixEntry p : commandPrefixes) {
                                if (p.toString().equalsIgnoreCase(prefix)) {
                                    commandPrefixes.remove(p);
                                    check = true;
                                }
                            }
                            if (check) {
                                data.write("Deleted prefix " + prefix);
                                data.markSuccess();
                            } else {
                                data.write("The prefix " + prefix + " doesn't exist!");
                                data.markFailed();
                            }
                        })
                .build();
        commandPrefixes
                .builders()
                .newCommandBuilder()
                .name("list")
                .description("Prints a list of all saved prefixes")
                .processor(
                        data -> {
                            if(commandPrefixes.isEmpty()) {
                                data.write("No prefixes currently saved!");
                                data.markFailed();
                            } else {
                                data.write(
                                        "Prefixes: "
                                                + commandPrefixes
                                                .stream()
                                                .map(CommandPrefixEntry::toString)
                                                .collect(Collectors.joining(", ")));
                                data.markSuccess();
                            }
                        })
                .build();
        commands
                .builders()
                .newCommandBuilder()
                .name("add")
                .description("Adds a new command and specifies how many of its arguments should be ignored by FancyChat")
                .processor(
                        data -> {
                            data.requiredArguments(2);
                            if (Strings.isNullOrEmpty(data.getArgumentAsString(0)))
                                throw new CommandExecuteException("Empty or null first argument");
                            if(Strings.isNullOrEmpty(data.getArgumentAsString(1)))
                                throw new CommandExecuteException("Empty or null second argument");
                            final String cmd = data.getArgumentAsString(0).toLowerCase();
                            final int args =  Integer.parseInt(data.getArgumentAsString(1));
                            if(args < 1)
                                throw new CommandExecuteException("Argument number must be superior or equal to 1");
                            ChatCommandEntry newEntry = new ChatCommandEntry(cmd);
                            newEntry.setArguments(args);
                            commands.add(newEntry);
                            data.write(String.format("Added command \"%s\"", cmd) + String.format("with \"%s\" arguments", args));
                            data.markSuccess();
                        })
                .build();
        commands
                .builders()
                .newCommandBuilder()
                .name("remove")
                .description("Removes a command")
                .processor(
                        data -> {
                            data.requiredArguments(1);
                            final String cmd = data.getArgumentAsString(0);
                            boolean check = false;
                            for(ChatCommandEntry c : commands) {
                                if (c.toString().equalsIgnoreCase(cmd)) {
                                    commands.remove(c);
                                    check = true;
                                }
                            }
                            if (check) {
                                data.write("Deleted command " + cmd);
                                data.markSuccess();
                            } else {
                                data.write("The command " + cmd + " doesn't exist!");
                                data.markFailed();
                            }
                        })
                .build();
        commands
                .builders()
                .newCommandBuilder()
                .name("list")
                .description("Prints a list of all saved commands")
                .processor(
                        data -> {
                            if(commands.isEmpty()) {
                                data.write("No commands currently saved!");
                                data.markFailed();
                            } else {
                                data.write("Commands:");
                                for (ChatCommandEntry c : commands)
                                    data.write(" - " + c.toString() + ", " + c.getArguments() + " ignored arguments.");
                                data.markSuccess();
                            }
                        })
                .build();
    }

    @SubscribeEvent
    public void onPacketSent(PacketEvent.Outgoing.Pre event) {
        if (event.getPacket() instanceof CPacketChatMessage && !PacketHelper.isIgnored(event.getPacket())) {
            if (getNetworkManager() != null) {
                String msg = ((CPacketChatMessage) event.getPacket()).getMessage();
                String[] split = msg.split(" ");
                for(int i = argsToIgnore(msg); i < split.length; i++)
                    split[i] = applyChanges(split[i]);

                StringBuilder sb = new StringBuilder();
                for(String s : split) sb.append(s).append(" ");
                msg = sb.toString();

                CPacketChatMessage packet = new CPacketChatMessage(msg);
                PacketHelper.ignore(packet);
                getNetworkManager().sendPacket(packet);
                event.setCanceled(true);
            }
        }
    }

    //from Seppuku
    public String consoleSpam(String input) {
        String ret = "";

        final char[] unicodeChars = new char[] {
                0x2E3B, 0x26D0, 0x26E8, 0x26BD, 0x26BE, 0x26F7, 0x23EA,
                0x23E9, 0x23EB, 0x23EC, 0x2705, 0x274C, 0x26C4
        };

        for (int i = 1, current = 0; i <= input.length() || current < input.length(); current = i, i += 1) {
            if (current != 0) {
                final Random random = new Random();
                for (int j = 0; j <= 2; j++)
                    ret += unicodeChars[random.nextInt(unicodeChars.length)];
            }

            if (i <= input.length()) ret += input.substring(current, i);
            else ret += input.substring(current);
        }

        return ret;
    }

    private String applyChanges(String s) {
        //Apply capitalization modifiers
        switch(caps.get()) {
            case WAVE:
                s = makeWave(s);
                break;
            case RANDOMCASE:
                s = randomCase(s);
                break;
        }
        //Apply font modifiers
        switch(font.get()) {
            case FULLWIDTH:
                s = toFullwidthFancyChat(s);
                break;
            case CIRCLE:
                s = toCircled(s);
                break;
            case PARENTHESES:
                s = toParenthesized(s);
                break;
            case SMALL:
                s = toPhoneticFancyChat(s);
                break;
            case LEET:
                s = toLeetspeak(s);
                break;
            case MORSE:
                s = toMorse(s);
                break;
        }
        if(consoleSpam.getAsBoolean()) s = consoleSpam(s); //Apply console spam
        return s;
    }

    private int argsToIgnore(String msg) {
        int args = 0; //Will return 0 if all else fails
        for(CommandPrefixEntry p : commandPrefixes) {
            if (!Strings.isNullOrEmpty(p.toString())
                    && msg.startsWith(p.toString())) {
                args = 1; //Ignores the first word, aka the actual "command"
                for(ChatCommandEntry c : commands)
                    if (msg.startsWith(p.toString() + c.toString()))
                        args += c.getArguments(); //Sums the number of arguments of the command
            }
        }
        return args;
    }

    //This is used by the .convert command
    public String applyChanges(String msg, String mode) {
        try {
            /* Save the original settings */
            FONT f = font.get();
            CAPS c = caps.get();
            boolean spam = consoleSpam.getAsBoolean();
            /* Apply the temporary ones */
            font.set(FONT.valueOf(mode.toUpperCase()));
            caps.set(CAPS.NONE); //TODO: figure out a way to allow for caps here as well
            consoleSpam.set(false);
            /* Apply the change */
            String s = applyChanges(msg);
            /* Restore the settings */
            font.set(f);
            caps.set(c);
            consoleSpam.set(spam);
            /* Return the final value */
            return s;
        } catch(IllegalArgumentException e) {
            /* If the mode is not valid, returns the original message */
            return msg;
        }
    }
}
