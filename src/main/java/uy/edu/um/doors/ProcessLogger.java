package uy.edu.um.doors;

import lombok.Getter;
import uy.edu.um.doors.model.Event;
import uy.edu.um.doors.model.Process;
import uy.edu.um.doors.model.User;
import uy.edu.um.tad.list.Node;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProcessLogger {

    private static final DateTimeFormatter FILE_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Getter
    private final String logFilePath;
    private PrintWriter writer;

    public ProcessLogger() {
        String date = LocalDateTime.now().format(FILE_DATE_FMT);
        this.logFilePath = "DOORS_PROCESS_LOG_" + date;
        try {
            this.writer = new PrintWriter(new FileWriter(logFilePath, true), true);
        } catch (IOException e) {
            System.err.println("[Logger] No se pudo abrir el archivo de log: " + e.getMessage());
        }
    }

    public void logNewPending(Process p) {
        String ts = timestamp();
        String line = "[" + ts + "]: NEW PENDING PROCESS: PID=" + p.getPid()
                + " | " + p.getName()
                + " | USER:" + p.getUser().getAlias()
                + " UID:" + p.getUser().getUid()
                + " | P=" + p.getPriority();
        writeLine(line);
    }

    public void logExecuting(Process p) {
        String ts = timestamp();
        writeLine("[" + ts + "]: EXECUTING PROCESS: PID=" + p.getPid()
                + " | " + p.getName()
                + " | USER:" + p.getUser().getAlias()
                + " UID:" + p.getUser().getUid());

        if (p.getEvents() != null) {
            Node<Event> eventNode = p.getEvents().getFirst();
            while (eventNode != null) {
                Event ev = eventNode.getValue();
                if (ev != null) {
                    StringBuilder instructions = new StringBuilder();
                    Node<String> instrNode = ev.getInstructions().getFirst();
                    while (instrNode != null) {
                        instructions.append(instrNode.getValue());
                        if (instrNode.getNext() != null) instructions.append(", ");
                        instrNode = instrNode.getNext();
                    }
                    writeLine("    EVENT: " + ev.getType()
                            + " | Instructions [" + instructions + "]");
                }
                eventNode = eventNode.getNext();
            }
        }
    }

    public void logFinishedOk(Process p) {
        String ts = timestamp();
        writeLine("[" + ts + "]: ENDING PROCESS: PID=" + p.getPid()
                + " | " + p.getName()
                + " | STATE: OK"
                + " | USER:" + p.getUser().getAlias()
                + " UID:" + p.getUser().getUid());
    }

    public void logFinishedError(Process p) {
        String ts = timestamp();
        writeLine("[" + ts + "]: ENDING PROCESS: PID=" + p.getPid()
                + " | " + p.getName()
                + " | STATE: ERROR"
                + " | USER:" + p.getUser().getAlias()
                + " UID:" + p.getUser().getUid());
    }

    public void logTerminated(Process p, User terminatedBy) {
        String ts = timestamp();
        writeLine("[" + ts + "]: ENDING PROCESS: PID=" + p.getPid()
                + " | " + p.getName()
                + " | STATE: TERMINATED"
                + " by USER:" + terminatedBy.getAlias()
                + " UID:" + terminatedBy.getUid());
    }

    public void logStackOverflow(Process archived) {
        String ts = timestamp();
        writeLine("[" + ts + "]: STACK OVERFLOW — ARCHIVING: PID=" + archived.getPid()
                + " | " + archived.getName()
                + " | STATE: " + (archived.getFinishType() != null
                ? archived.getFinishType().name() : archived.getState().name())
                + " | USER:" + archived.getUser().getAlias()
                + " UID:" + archived.getUser().getUid());
    }

    private String timestamp() {
        return LocalDateTime.now().format(TS_FMT);
    }

    private void writeLine(String line) {
        if (writer != null) {
            writer.println(line);
        }
        System.out.println(line);
    }

    public void close() {
        if (writer != null) {
            writer.close();
        }
    }
}
