package com.matt.forgehax.mods.commands;

/*
 * Made by Fraaz on the 18th of November 2020.
 * Applies FancyChat to the input and copies it in your clipboard.
 */

import com.matt.forgehax.mods.FancyChat;
import com.matt.forgehax.util.command.Command;
import com.matt.forgehax.util.command.CommandBuilders;
import com.matt.forgehax.util.mod.CommandMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.matt.forgehax.Helper.getLocalPlayer;

@RegisterMod
public class TextConvertCommand extends CommandMod {
    public TextConvertCommand() {
        super("TextConvertCommand");
    }

    @RegisterCommand
    public Command convert(CommandBuilders builders) {
        FancyChat fc = new FancyChat(); //This is to reduce the amount of redundant code
        return builders
                .newCommandBuilder()
                .name("convert")
                .description("Applies FancyChat to an input and copies it over to your clipboard")
                .options(
                        parser -> {
                            //TODO: Find a better way to do this because doing this without aliases is shit
                            parser.accepts("fullwidth", "Applies the \uFF26\uFF35\uFF2C\uFF2C\uFF37\uFF29\uFF24\uFF34\uFF28 modifier");
                            parser.accepts("circled", "Applies the \u24D2\u24D8\u24E1\u24D2\u24DB\u24D4\u24D3 modifier");
                            parser.accepts("leet", "Applies the 13375p34k modifier");
                            parser.accepts("morse", "Converts the text to morse");
                            parser.accepts("parentheses", "Applies the \u24AB\u249C\u24AD\u24A0\u24A9\u24AF\u24A3\u24A0\u24AE\u24A4\u24B5\u24A0\u249F modifier");
                            parser.accepts("small", "Applies the \u02E2\u1D50\u1D43\u02E1\u02E1 FancyChat option");
                        }
                )
                .processor(
                        data -> {
                            if (getLocalPlayer() != null) {
                                StringBuilder sb = new StringBuilder();
                                for(int i = 1; i < data.getArgumentCount(); i++) {
                                    sb.append(data.getArgumentAsString(i));
                                    if(i != data.getArgumentCount() - 1) sb.append(" ");
                                }
                                String text = sb.toString();
                                if(!text.equals(fc.applyChanges(text, data.getArgumentAsString(0))))
                                    text = fc.applyChanges(text, data.getArgumentAsString(0));
                                final StringSelection clip = new StringSelection(text);
                                final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                                clipboard.setContents(clip, null);
                                data.write("Copied the converted text to your clipboard");
                                data.markSuccess();
                            }
                        }
                )
                .build();
    }
}