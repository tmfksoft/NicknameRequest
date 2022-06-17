package ca.pn.commands;

import com.infermc.nicknamerequest.NicknameRequest;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand extends Command {

    public HelpCommand(CommandManager manager, Command parentCommand) {
        super(manager, parentCommand);
        this.commandName = "help";
        this.commandUsage = "[...command]";
        this.commandDescription = "Shows usage for NicknameRequest commands.";
    }

    @Override
    public boolean onCall(CommandSender sender, String[] args) {
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;

        sender.sendMessage("=@= Command Usage =@=");
        // Return top level usage.
        if (args.length == 0) {

            // '/nick help'
            for (Command command : parentCommand.subCommands) {

                String requiredPermission = command.requiredPermission;
                if (requiredPermission != null) {
                    if (!sender.hasPermission(requiredPermission)) {
                        // Skip commands we can't access.
                        continue;
                    }
                }
                this.sendCommandInfo(command, sender, false);
            }
        } else if (args.length == 1) {

            // Try and return info for a specific command.
            String commandName = args[0];
            if (this.manager.getSubCommand(this.parentCommand, commandName) != null) {
                Command command = this.manager.getSubCommand(this.parentCommand, commandName.toLowerCase());
                this.sendCommandInfo(command, sender, true);
            } else {
                sender.sendMessage(plugin.colourFormat("    &cNo such NicknameRequest command!1"));
                sender.sendMessage(plugin.colourFormat("    &cTry &l/" + String.join(" ", manager.getCommandPath(this.parentCommand)) + " help"));
            }
        } else {

            // Iterate through the args until we either hit a final command or have a list of commands.
            Command command = this.manager.getSubCommand(this.parentCommand, args[0]); // Top level command
            List<String> commandChain = new ArrayList<>();
            commandChain.add(args[1].toLowerCase());

            if (command == null) {
                // No such top level command
                sender.sendMessage(plugin.colourFormat("    &cNo such NicknameRequest command!2"));
                sender.sendMessage(plugin.colourFormat("    &cTry &l/" + String.join(" ", manager.getCommandPath(this.parentCommand)) + " help"));
                return true;
            }

            for (int i=1; i<args.length; i++) {
                // Try and get a sub command
                Command subCommand = this.getSubCommand(command, args[i]);
                if (subCommand != null) {
                    commandChain.add(subCommand.commandName);
                    command = subCommand;
                }
            }

            this.sendCommandInfo(command, sender, true, commandChain);
            return true;

        }
        return true;
    }

    private void sendCommandInfo(Command command, CommandSender target, boolean showSubCommands) {
        ArrayList<String> commandChain = new ArrayList<>();
        commandChain.add(command.commandName);
        this.sendCommandInfo(command, target, showSubCommands, commandChain);
    }
    private void sendCommandInfo(Command command, CommandSender target, boolean showSubCommands, List<String> commandChain) {
        NicknameRequest plugin = (NicknameRequest) this.manager.parentPlugin;
        String commandPrefix = "";

        for (String chainCommand : commandChain) {
            int index = commandChain.indexOf(chainCommand);
            if (index == 0) {
                commandPrefix = chainCommand;
                continue;
            }
            commandPrefix = commandPrefix + " " + chainCommand;
        }

        target.sendMessage(plugin.colourFormat("    &a&l/nick " + commandPrefix + " &r&b" + command.commandUsage));
        target.sendMessage(plugin.colourFormat("        &e&o" + command.commandDescription));

        if (showSubCommands) {
            target.sendMessage(" Sub commands:");
            for (Command subCommand : command.subCommands) {
                commandChain.add(subCommand.commandName);
                this.sendCommandInfo(subCommand, target, false, commandChain);
            }
        }
    }

    private Command getSubCommand(Command command, String commandName) {
        for (Command subCommand : command.subCommands) {

            // Check if this sub commands main name or alias' match.
            boolean callSubCommand = false;
            if (subCommand.commandName.equalsIgnoreCase(commandName)) {
                return subCommand;
            }
            for (int i = 0; i < subCommand.commandAliases.length; i++) {
                if (subCommand.commandAliases[i].equalsIgnoreCase(commandName)) {
                    return subCommand;
                }
            }
        }
        return null;
    }
}
