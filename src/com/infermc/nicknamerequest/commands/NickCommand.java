package com.infermc.nicknamerequest.commands;

import ca.pn.commands.Command;
import ca.pn.commands.CommandManager;
import ca.pn.commands.HelpCommand;

public class NickCommand extends Command {
    /**
     * Defines a command
     *  @param manager        The NicknameRequest plugin.
     *
     */
    public NickCommand(CommandManager manager) {
        super(manager, null);

        this.commandName = "nick";
        this.commandDescription = "NicknameRequest Command";

        this.registerSubcommand(new HelpCommand(manager, this));
        this.registerSubcommand(new StatusCommand(manager, this));
        this.registerSubcommand(new RequestCommand(manager, this));
        this.registerSubcommand(new CancelCommand(manager, this));
        this.registerSubcommand(new AcceptCommand(manager, this));
        this.registerSubcommand(new DenyCommand(manager, this));
        this.registerSubcommand(new InfoCommand(manager, this));
        this.registerSubcommand(new ListCommand(manager, this));
        this.registerSubcommand(new RestrictCommand(manager, this));
        this.registerSubcommand(new UnrestrictCommand(manager, this));
        this.registerSubcommand(new VersionCommand(manager, this));
        this.registerSubcommand(new SetCommand(manager, this));
        this.registerSubcommand(new RemoveCommand(manager, this));
    }
}
