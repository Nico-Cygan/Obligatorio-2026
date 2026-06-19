package uy.edu.um.doors;

import uy.edu.um.doors.model.Event;
import uy.edu.um.doors.model.EventType;
import uy.edu.um.doors.model.Process;
import uy.edu.um.doors.model.ProcessState;
import uy.edu.um.doors.model.User;
import uy.edu.um.doors.model.UserType;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CsvLoader {

    public MyList<User> loadUsers(String usersCsvPath) {
        MyList<User> users = new MyLinkedListImpl<>();

        try (BufferedReader reader = Files.newBufferedReader(Path.of(usersCsvPath), StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                users.add(parseUserLine(line, lineNumber));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo de usuarios: " + usersCsvPath, e);
        }

        return users;
    }

    public MyList<Process> loadProcesses(String processCsvPath, MyHash<Integer, User> usersByUid) {
        MyList<Process> processes = new MyLinkedListImpl<>();

        try (BufferedReader reader = Files.newBufferedReader(Path.of(processCsvPath), StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                processes.add(parseProcessLine(line, lineNumber, usersByUid));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo de procesos: " + processCsvPath, e);
        }

        return processes;
    }

    private User parseUserLine(String line, int lineNumber) {
        String[] parts = line.split(";", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato inválido en usuarios.csv en la línea " + lineNumber);
        }

        int uid = parseInt(parts[0], "UID", lineNumber);
        String alias = parts[1].trim();
        if (alias.isEmpty()) {
            throw new IllegalArgumentException("Alias vacío en usuarios.csv en la línea " + lineNumber);
        }

        UserType type = parseUserType(parts[2], lineNumber);
        return new User(uid, alias, type);
    }

    private Process parseProcessLine(String line, int lineNumber, MyHash<Integer, User> usersByUid) {
        String[] parts = line.split(";", 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Formato inválido en process.csv en la línea " + lineNumber);
        }

        int pid = parseInt(parts[0], "PID", lineNumber);
        int uid = parseInt(parts[1], "UID", lineNumber);
        String name = parts[2].trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Nombre de proceso vacío en la línea " + lineNumber);
        }

        User owner = usersByUid.get(uid);
        if (owner == null) {
            throw new IllegalArgumentException("No existe un usuario con UID " + uid + " para el proceso de la línea " + lineNumber);
        }

        MyList<Event> events = parseEvents(parts[3], lineNumber);
        return new Process(pid, name, owner, 0, ProcessState.NEW, events, null, null);
    }

    private MyList<Event> parseEvents(String rawEvents, int lineNumber) {
        String normalized = rawEvents.trim();
        if (!normalized.startsWith("{") || !normalized.endsWith("}")) {
            throw new IllegalArgumentException("Formato inválido de eventos en la línea " + lineNumber);
        }

        normalized = normalized.substring(1, normalized.length() - 1).trim();
        MyList<Event> events = new MyLinkedListImpl<>();
        if (normalized.isEmpty()) {
            return events;
        }

        String[] eventChunks = normalized.split("#");
        for (String chunk : eventChunks) {
            String eventText = chunk.trim();
            if (eventText.isEmpty()) {
                continue;
            }

            int separator = eventText.indexOf(':');
            if (separator <= 0) {
                throw new IllegalArgumentException("Formato inválido de evento en la línea " + lineNumber);
            }

            EventType type = parseEventType(eventText.substring(0, separator), lineNumber);
            String instructionsBlock = eventText.substring(separator + 1).trim();
            if (!instructionsBlock.startsWith("[") || !instructionsBlock.endsWith("]")) {
                throw new IllegalArgumentException("Formato inválido de instrucciones en la línea " + lineNumber);
            }

            String instructionsText = instructionsBlock.substring(1, instructionsBlock.length() - 1).trim();
            MyList<String> instructions = new MyLinkedListImpl<>();
            if (!instructionsText.isEmpty()) {
                String[] instructionParts = instructionsText.split(",");
                for (String instruction : instructionParts) {
                    String cleanInstruction = instruction.trim();
                    if (!cleanInstruction.isEmpty()) {
                        instructions.add(cleanInstruction);
                    }
                }
            }

            if (instructions.isEmpty()) {
                throw new IllegalArgumentException("Un evento debe tener al menos una instrucción en la línea " + lineNumber);
            }

            events.add(new Event(type, instructions));
        }

        return events;
    }

    private EventType parseEventType(String value, int lineNumber) {
        try {
            return EventType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de evento inválido en la línea " + lineNumber + ": " + value.trim(), e);
        }
    }

    private UserType parseUserType(String value, int lineNumber) {
        try {
            return UserType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de usuario inválido en la línea " + lineNumber + ": " + value.trim(), e);
        }
    }

    private int parseInt(String value, String fieldName, int lineNumber) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " inválido en la línea " + lineNumber + ": " + value.trim(), e);
        }
    }
}
