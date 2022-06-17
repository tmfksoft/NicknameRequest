package ca.pn.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandManager {

    private List<Command> commands = new ArrayList<>();
    public Plugin parentPlugin;

    public CommandManager(Plugin parentPlugin) {
        this.parentPlugin = parentPlugin;
    }

    public void registerCommand(Command command) {
        this.commands.add(command);
    }
    public List<Command> getCommands() {
        return this.commands;
    }

    /**
     * DON'T OVERRIDE THIS, Allows a commands sub commands to be called.
     * @param sender The Commands sender
     * @param cmd The Bukkit Command
     * @param label The command label
     * @param args The commands arguments
     * @return Result of the command, false means the command doesn't exist.
     */
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {

        String commandName = cmd.getName().toLowerCase();
        Command currentCommand = this.getCommand(commandName);

        if (currentCommand == null) {
            // No such command
            return false;
        }

        List<String> newArgs = new ArrayList<>();
        for (int i=0; i<args.length; i++) {
            newArgs.add(args[i]);
        }

        for (int i=0; i<args.length; i++) {


            commandName = args[i];

            // Check if we can match a sub command.
            Command subCommand = getSubCommand(currentCommand, commandName);
            if (subCommand == null) {
                // Can't go any further, abort.
                break;
            }

            // We found sub command, next iteration may find a further one.
            currentCommand = subCommand;

            // Remove this command from the array.
            newArgs.remove(0);
        }

        // Call the command we found, if any. Otherwise the original top level command.
        String[] castArgs = new String[newArgs.size()];
        newArgs.toArray(castArgs);

        return currentCommand.onCall(sender, castArgs);
    }

    /**
     * Attempts to get a top level command.
     * @param commandName The commands name.
     * @return The command, null if it doesn't exist.
     */
    public Command getCommand(String commandName) {
        for (Command command : this.commands) {
            // Check if this commands main name or alias' match.
            if (command.commandName.equalsIgnoreCase(commandName)) {
                return command;
            }
            for (int i = 0; i < command.commandAliases.length; i++) {
                if (command.commandAliases[i].equalsIgnoreCase(commandName)) {
                    return command;
                }
            }
        }
        return null;
    }

    /**
     * Dodgy method to slice a string array with a start and end
     * @param input String array
     * @param start Start index
     * @return
     */
    private String[] sliceArray(String[] input, int start) {

        return input;
    }

    /**
     * Attempts to get a commands sub command.
     * @param command The command to check the sub commands of.
     * @param commandName The name of the sub command to get.
     * @return The sub command, null if it doesn't exist.
     */
    public Command getSubCommand(Command command, String commandName) {
        for (Command subCommand : command.subCommands) {

            // Check if this sub commands main name or alias' match.

            if (subCommand.commandName.equalsIgnoreCase(commandName)) {
                return subCommand;
            }
            for (int i = 0; i < subCommand.commandAliases.length; i++) {
                if (subCommand.commandAliases[i].equalsIgnoreCase(commandName)) {
                    return subCommand;
                }
            }
        }
        // No such sub command.
        return null;
    }

    public String[] getCommandPath(Command command) {
        if (command.subCommands.size() == 0) {
            String[] commandPath = new String[1];
            commandPath[0] = command.commandName;
            return commandPath;
        }

        ArrayList<String> commandPath = new ArrayList<>();
        commandPath.add(command.commandName);

        Command parentCommand = command.parentCommand;

        if (parentCommand != null) {
            boolean hasParent = true;
            while (hasParent) {
                commandPath.add(parentCommand.commandName);
                if (parentCommand.parentCommand != null) {
                    parentCommand = parentCommand.parentCommand;
                } else {
                    hasParent = false;
                }
            }
        }

        // Reverse the array
        Collections.reverse(commandPath);

        String[] commandPathCast = new String[commandPath.size()];
        commandPath.toArray(commandPathCast);


        return commandPathCast;
    }
}
