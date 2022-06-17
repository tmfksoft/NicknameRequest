package ca.pn.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class Command {

    // Permission required to use this command, null allows anyone to use it.
    public String requiredPermission = null;
    public Command parentCommand = null;

    // Subcommands!
    public List<Command> subCommands = new ArrayList<>();

    // Used by the help command.
    public String commandName = "command";
    public String[] commandAliases = new String[0];
    public String commandUsage = "";
    public String commandDescription = "Its a command";

    protected CommandManager manager;

    /**
     * Defines a command
     * @param manager The command manager
     * @param parentCommand The parent command if this is a sub command. Null for top level commands.
     */
    public Command(CommandManager manager, Command parentCommand) {
        this.manager = manager;
        this.parentCommand = parentCommand;
    }

    /**
     * The main call command. This is called when your command should be run.
     * @param sender The user who ran the command, may be a Player or Console.. or other!
     * @param args Array of arguments passed to the command
     * @return True/False - False means the command doesn't exist and True means it does.
     */
    public boolean onCall(CommandSender sender, String[] args) {
        return false;
    }

    /**
     * Registers a sub command, shortcut for adding to the sub command list.
     * @param command The command to add.
     */
    public void registerSubcommand(Command command) {
        this.subCommands.add(command);
    }

    /**
     * Get a sub command, cheats by using the managers getSubCommand method
     * @param commandName
     * @return
     */
    public Command getSubCommand(String commandName) {
        // Lazy! :D
        return this.manager.getSubCommand(this, commandName);
    }

    /**
     * Quick method to format strings for Minecraft message. Uses &
     * @param text Input text
     * @return Formatted text.
     */
    public String format(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
