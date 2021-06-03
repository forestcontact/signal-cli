package org.asamk.signal.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import org.asamk.signal.JsonReceiveMessageHandler;
import org.asamk.signal.OutputType;
import org.asamk.signal.ReceiveMessageHandler;
import org.asamk.signal.manager.Manager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.asamk.signal.util.ErrorUtils.handleAssertionError;


class InputReader implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(InputReader.class);

    private volatile boolean alive = true;
    private final Manager manager;

    InputReader(final Manager manager) {
        this.manager = manager;
    }

    public void terminate() {
        this.alive = false;
    }

    @Override
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        ObjectMapper jsonProcessor = new ObjectMapper();
        TypeReference<Map<String, Object>> inputType = new TypeReference<>() {};
        while (alive) {
            try {
                String input = br.readLine();
                if (input != null) {
                    new Namespace(Map.of("a", "b"));
                    Map<String, Object> commandMap = jsonProcessor.readValue(input, inputType);
                    Namespace commandNamespace = new Namespace(commandMap);
                    // ideally, union with our namespace, or just add output=json
                    String commandKey = commandNamespace.getString("command");
                    LocalCommand commandObject = (LocalCommand) Commands.getCommand(commandKey);
                    assert commandObject != null;
                    commandObject.handleCommand(commandNamespace, manager); // updateGroup needs to have a json output
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
                //logger.error("{\"error\":\"{}\"}", error); // wrong...
                // alive = false; // there are some exceptions where we wouldn't want to keep going but idk what they are
            }
        }
    }
}

public class StdioCommand implements LocalCommand {
    //private final static Logger logger = LoggerFactory.getLogger(StdioCommand.class);

    @Override
    public void attachToSubparser(final Subparser subparser) {
        subparser.addArgument("--ignore-attachments")
                .help("Don’t download attachments of received messages.")
                .action(Arguments.storeTrue());
        subparser.addArgument("--json")
                .help("WARNING: This parameter is now deprecated! Please use the global \"--output=json\" option instead.\n\nOutput received messages in json format, one json object per line.")
                .action(Arguments.storeTrue());
    }

    @Override
    public Set<OutputType> getSupportedOutputTypes() {
        return Set.of(OutputType.PLAIN_TEXT, OutputType.JSON);
    }

    @Override
    public void handleCommand(final Namespace ns, final Manager m) {
        var inJson = ns.get("output") == OutputType.JSON || ns.getBoolean("json");
        boolean ignoreAttachments = ns.getBoolean("ignore_attachments");
        InputReader reader = new InputReader(m);
        Thread readerThread = new Thread(reader);
        readerThread.start();
        try {
            m.receiveMessages(1,
                    TimeUnit.HOURS,
                    false,
                    ignoreAttachments,
                    inJson ? new JsonReceiveMessageHandler(m) : new ReceiveMessageHandler(m)
                    /*true*/);
        } catch (IOException e) {
            System.err.println("Error while receiving messages: " + e.getMessage());
        } catch (AssertionError e) {
            handleAssertionError(e);
        } finally {
            reader.terminate();
        }
    }
}
