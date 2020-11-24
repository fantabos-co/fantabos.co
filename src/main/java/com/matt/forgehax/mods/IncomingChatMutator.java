package com.matt.forgehax.mods;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.util.text.CircledChatHelper.revertCircled;
import static com.matt.forgehax.util.text.FullwidthHelper.revertFullwidthFancyChat;
import static com.matt.forgehax.util.text.ParenthesizedHelper.revertParenthesized;
import static com.matt.forgehax.util.text.PhoneticHelper.revertPhoneticFancyChat;

/*
 * Made by Fraaz on the 3rd of November 2020.
 * Replaces annoying FancyChat messages with normal ones.
 */

@RegisterMod
public class IncomingChatMutator extends ToggleMod {
    public final Setting<Boolean> fancyChat =
            getCommandStub()
                    .builders()
                    .<Boolean>newSettingBuilder()
                    .name("fancyChat")
                    .description("Turns FancyChat messages back into normal text")
                    .defaultTo(true)
                    .build();

    public final Setting<Boolean> smallFancyChat =
            getCommandStub()
                    .builders()
                    .<Boolean>newSettingBuilder()
                    .name("phoneticFancyChat")
                    .description("Sometimes phonetic symbols are used as FancyChat: this reverts them to normal text")
                    .defaultTo(true)
                    .build();

    public final Setting<Boolean> ignoreOwnMessages =
            getCommandStub()
                    .builders()
                    .<Boolean>newSettingBuilder()
                    .name("ignoreOwnMessages")
                    .description("Prevents reverting your own FancyChat messages")
                    .defaultTo(true)
                    .build();

    public IncomingChatMutator() { //TODO: We should probably find a better name
        super(Category.CHAT, "IncomingChatMutator", false, "Manipulates incoming chat");
    }

    @SubscribeEvent
    public void onChatReceive(ClientChatReceivedEvent event) {
        if(ignoreOwnMessages.getAsBoolean()) {
            String playerTag = "<" + getLocalPlayer().getName() + ">";
            if(event.getMessage().getUnformattedText().split(" ")[0].equalsIgnoreCase(playerTag)) return;
        }
        String s = event.getMessage().getFormattedText();
        if(fancyChat.getAsBoolean()) { //Applies supported reverters
            s = revertParenthesized(s);
            s = revertCircled(s);
            s = revertFullwidthFancyChat(s);
        }
        if(smallFancyChat.getAsBoolean()) s = revertPhoneticFancyChat(s);
        event.setMessage(new TextComponentString(s));
    }
}
