package org.asamk.signal.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import org.asamk.signal.JsonReceiveMessageHandler;
import org.asamk.signal.JsonWriter;
import org.asamk.signal.OutputType;
import org.asamk.signal.ReceiveMessageHandler;
import org.asamk.signal.manager.Manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class NamespaceDefaultingToFalse extends Namespace {
    public NamespaceDefaultingToFalse(final Map<String, Object> attrs) {
        super(attrs);
    }

    @Override
    public Boolean getBoolean(String dest) {
        Boolean maybeGotten = this.get(dest);
        if (maybeGotten == null) {
            maybeGotten = false;
        }
        return maybeGotten;
    }
}

class InputReader implements Runnable {
    private volatile boolean alive = true;
    private final Manager manager;
    private final Map<String, Object> ourNamespace;
    private final Boolean inJson;
    InputReader(Namespace ns, final Manager manager, Boolean inJson) {
        this.ourNamespace = ns.getAttrs();
        this.manager = manager;
        this.inJson = inJson;
    }

    public void terminate() {
        this.alive = false;
    }

    @Override
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        ObjectMapper jsonProcessor = new ObjectMapper();
        JsonWriter jsonWriter = new JsonWriter(System.out);
        TypeReference<Map<String, Object>> inputType = new TypeReference<>() {};
        while (alive) {
            try {
                String input = br.readLine();
                if (input != null) {
                    Map<String, Object> commandMap = jsonProcessor.readValue(input, inputType);
                    HashMap<String, Object> mergedMap = new HashMap<>(ourNamespace);
                    mergedMap.putAll(commandMap);
                    Namespace commandNamespace = new NamespaceDefaultingToFalse(mergedMap);
                    String commandKey = commandNamespace.getString("command");
                    LocalCommand commandObject = (LocalCommand) Commands.getCommand(commandKey);
                    assert commandObject != null;
                    commandObject.handleCommand(commandNamespace, manager);
                }
            } catch (Exception e) {
                if (this.inJson) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    jsonWriter.write(Map.of("error", e.toString(), "traceback", errors.toString()));
                } else {
                    e.printStackTrace(System.err);
                }
            }
        }
    }
}

public class StdioCommand implements LocalCommand {
    public void attachToSubparser(final Subparser subparser) {
        subparser.addArgument("--ignore-attachments")
                .help("Don???t download attachments of received messages.")
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
        InputReader reader = new InputReader(ns, m, inJson);
        Thread readerThread = new Thread(reader);
        readerThread.start();
        try {
            m.receiveMessages(1,
                    TimeUnit.HOURS,
                    false,
                    ignoreAttachments,
                    inJson ? new JsonReceiveMessageHandler(m) : new ReceiveMessageHandler(m)
            );
        } catch (IOException e) {
            System.err.println("Error while receiving messages: " + e.getMessage());
        } finally {
            reader.terminate();
        }
    }
}
