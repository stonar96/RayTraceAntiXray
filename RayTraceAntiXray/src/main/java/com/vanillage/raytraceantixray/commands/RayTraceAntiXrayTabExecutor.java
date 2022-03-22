package com.vanillage.raytraceantixray.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import com.vanillage.raytraceantixray.RayTraceAntiXray;

public final class RayTraceAntiXrayTabExecutor implements TabExecutor {
    private final RayTraceAntiXray plugin;

    public RayTraceAntiXrayTabExecutor(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new LinkedList<>();

        if (args.length == 0) {
            if ("raytraceantixray".startsWith(label.toLowerCase(Locale.ROOT))) {
                completions.add("raytraceantixray");
            }
        } else if (command.getName().toLowerCase(Locale.ROOT).equals("raytraceantixray")) {
            if (args.length == 1) {
                if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings") && "timings".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add("timings");
                }
            } else if (args[0].toLowerCase(Locale.ROOT).equals("timings")) {
                if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings")) {
                    if (args.length == 2) {
                        if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings.on") && "on".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            completions.add("on");
                        }

                        if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings.off") && "off".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            completions.add("off");
                        }
                    } else if (args[1].toLowerCase(Locale.ROOT).equals("on")) {
                        if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings.on")) {
                            if (args.length == 3) {

                            }
                        }
                    } else if (args[1].toLowerCase(Locale.ROOT).equals("off")) {
                        if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings.off")) {
                            if (args.length == 3) {

                            }
                        }
                    }
                }
            }
        }

        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().toLowerCase(Locale.ROOT).equals("raytraceantixray")) {
            if (args.length == 0) {

            } else if (args[0].toLowerCase(Locale.ROOT).equals("timings")) {
                if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings")) {
                    if (args.length == 1) {

                    } else if (args[1].toLowerCase(Locale.ROOT).equals("on")) {
                        if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings.on")) {
                            if (args.length == 2) {
                                plugin.setTimings(true);
                                sender.sendMessage("Timings turned on.");
                                return true;
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
                            return true;
                        }
                    } else if (args[1].toLowerCase(Locale.ROOT).equals("off")) {
                        if (sender.hasPermission("raytraceantixray.command.raytraceantixray.timings.off")) {
                            if (args.length == 2) {
                                plugin.setTimings(false);
                                sender.sendMessage("Timings turned off.");
                                return true;
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permissions.");
                            return true;
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "You don't have permissions.");
                    return true;
                }
            }
        }

        return false;
    }
}
