package org.asamk.signal.commands;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import org.asamk.signal.JsonWriter;
import org.asamk.signal.OutputType;
import org.asamk.signal.commands.exceptions.CommandException;
import org.asamk.signal.commands.exceptions.IOErrorException;
import org.asamk.signal.manager.Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class UpdateProfileCommand implements LocalCommand {

    @Override
    public void attachToSubparser(final Subparser subparser) {
        subparser.addArgument("--name").help("New profile name");
        subparser.addArgument("--about").help("New profile about text");
        subparser.addArgument("--about-emoji").help("New profile about emoji");

        final var avatarOptions = subparser.addMutuallyExclusiveGroup();
        avatarOptions.addArgument("--avatar").help("Path to new profile avatar");
        avatarOptions.addArgument("--remove-avatar").action(Arguments.storeTrue());

        subparser.help("Set a name, about and avatar image for the user profile");
    }
    private final static Logger logger = LoggerFactory.getLogger(UpdateProfileCommand.class);

    @Override
    public Set<OutputType> getSupportedOutputTypes() {
        return Set.of(OutputType.PLAIN_TEXT, OutputType.JSON);
    }

    @Override
    public void handleCommand(final Namespace ns, final Manager m) throws CommandException {
        var name = ns.getString("name");
        var about = ns.getString("about");
        var aboutEmoji = ns.getString("about_emoji");
        var avatarPath = ns.getString("avatar");
        boolean removeAvatar = ns.getBoolean("remove_avatar");

        Optional<File> avatarFile = removeAvatar
                ? Optional.absent()
                : avatarPath == null ? null : Optional.of(new File(avatarPath));


        var inJson = ns.get("output") == OutputType.JSON || ns.getBoolean("json");

        // TODO delete later when "json" variable is removed
        if (ns.getBoolean("json")) {
            logger.warn("\"--json\" option has been deprecated, please use the global \"--output=json\" instead.");
        }

        try {
            m.setProfile(name, about, aboutEmoji, avatarFile);
            if (inJson) {
                final var jsonWriter = new JsonWriter(System.out);
                jsonWriter.write(Map.of("status", "success"));
            }
        } catch (IOException e) {
            if (inJson) {
                final var jsonWriter = new JsonWriter(System.out);
                jsonWriter.write(Map.of("status", "failure"));
            }
            throw new IOErrorException("Update profile error: " + e.getMessage());
        }
    }
}
