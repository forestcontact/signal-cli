package org.asamk.signal.commands;

import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import org.asamk.Signal;
import org.asamk.signal.JsonWriter;
import org.asamk.signal.OutputType;
import org.asamk.signal.PlainTextWriterImpl;
import org.asamk.signal.commands.exceptions.CommandException;
import org.asamk.signal.commands.exceptions.UnexpectedErrorException;
import org.asamk.signal.commands.exceptions.UserErrorException;
import org.asamk.signal.manager.groups.GroupIdFormatException;
import org.asamk.signal.util.Util;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.asamk.signal.util.ErrorUtils.handleAssertionError;

public class UpdateGroupCommand implements DbusCommand {

    private final static Logger logger = LoggerFactory.getLogger(UpdateGroupCommand.class);

    @Override
    public void attachToSubparser(final Subparser subparser) {
        subparser.addArgument("-g", "--group").help("Specify the recipient group ID.");
        subparser.addArgument("-n", "--name").help("Specify the new group name.");
        subparser.addArgument("-a", "--avatar").help("Specify a new group avatar image file");
        subparser.addArgument("-m", "--member").nargs("*").help("Specify one or more members to add to the group");
    }

    @Override
    public Set<OutputType> getSupportedOutputTypes() {
        return Set.of(OutputType.PLAIN_TEXT, OutputType.JSON);
    }

    @Override
    public void handleCommand(final Namespace ns, final Signal signal) throws CommandException {
        final var writer = new PlainTextWriterImpl(System.out);
        byte[] groupId = null;
        if (ns.getString("group") != null) {
            try {
                groupId = Util.decodeGroupId(ns.getString("group")).serialize();
            } catch (GroupIdFormatException e) {
                throw new UserErrorException("Invalid group id:" + e.getMessage());
            }
        }
        if (groupId == null) {
            groupId = new byte[0];
        }

        var groupName = ns.getString("name");
        if (groupName == null) {
            groupName = "";
        }

        List<String> groupMembers = ns.getList("member");
        if (groupMembers == null) {
            groupMembers = new ArrayList<>();
        }

        var groupAvatar = ns.getString("avatar");
        if (groupAvatar == null) {
            groupAvatar = "";
        }

        var inJson = ns.get("output") == OutputType.JSON || ns.getBoolean("json");

        // TODO delete later when "json" variable is removed
        if (ns.getBoolean("json")) {
            logger.warn("\"--json\" option has been deprecated, please use the global \"--output=json\" instead.");
        }

        try {
            var newGroupId = signal.updateGroup(groupId, groupName, groupMembers, groupAvatar);
            if (groupId.length != newGroupId.length) {
                String encodedGroup = Base64.getEncoder().encodeToString(newGroupId);
                if (inJson) {
                    final var jsonWriter = new JsonWriter(System.out);
                    jsonWriter.write(Map.of("group", encodedGroup, "members", groupMembers, "name", groupName));
                } else {
                    writer.println("Created new group: \"{}\"", encodedGroup);
                }
            }
        } catch (AssertionError e) {
            handleAssertionError(e);
            throw e;
        } catch (Signal.Error.AttachmentInvalid e) {
            throw new UserErrorException("Failed to add avatar attachment for group\": " + e.getMessage());
        } catch (DBusExecutionException e) {
            throw new UnexpectedErrorException("Failed to send message: " + e.getMessage());
        }
    }
}
