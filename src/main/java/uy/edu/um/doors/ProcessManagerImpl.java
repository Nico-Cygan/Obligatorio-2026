package uy.edu.um.doors;

import uy.edu.um.doors.model.Event;
import uy.edu.um.doors.model.FinishType;
import uy.edu.um.doors.model.Process;
import uy.edu.um.doors.model.ProcessState;
import uy.edu.um.doors.model.User;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.heap.MyHeap;
import uy.edu.um.tad.heap.MyHeapImpl;
import uy.edu.um.tad.list.MyLinkedListImpl;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.Node;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;
import uy.edu.um.tad.stack.EmptyStackException;
import uy.edu.um.tad.stack.MyStack;
import uy.edu.um.tad.stack.MyStackImpl;

public class ProcessManagerImpl implements ProcessManager {

    private MyHash<Integer, User> usersByUid;
    private MyHash<Integer, Process> processesByPid;
    private MyQueue<Process> newProcesses;
    private MyHeap<Process> pendingProcesses;
    private MyStack<Process> finishedProcesses;
    private Process currentProcess;
    private final ProcessLogger logger;
    public ProcessManagerImpl() {
        this.usersByUid = new MyHashImpl<>();
        this.processesByPid = new MyHashImpl<>();
        this.newProcesses = new MyQueueImpl<>();
        this.finishedProcesses = new MyStackImpl<>();
        this.pendingProcesses = new MyHeapImpl<>(false);
        this.logger = new ProcessLogger();
    }

    @Override
    public void loadProcessAndUserData(String processCsvPath, String usersCsvPath) {
        CsvLoader loader = new CsvLoader();

        this.usersByUid = new MyHashImpl<>();
        this.processesByPid = new MyHashImpl<>();
        this.newProcesses = new MyQueueImpl<>();

        MyList<User> users = loader.loadUsers(usersCsvPath);
        Node<User> currentUser = users.getFirst();
        while (currentUser != null) {
            User user = currentUser.getValue();
            if (user != null) {
                if (usersByUid.contains(user.getUid())) {
                    throw new IllegalArgumentException("Usuario duplicado con UID " + user.getUid());
                }
                usersByUid.put(user.getUid(), user);
            }
            currentUser = currentUser.getNext();
        }

        MyList<Process> processes = loader.loadProcesses(processCsvPath, usersByUid);
        Node<Process> currentProcess = processes.getFirst();
        while (currentProcess != null) {
            Process process = currentProcess.getValue();
            if (process != null) {
                if (processesByPid.contains(process.getPid())) {
                    throw new IllegalArgumentException("Proceso duplicado con PID " + process.getPid());
                }
                processesByPid.put(process.getPid(), process);
                newProcesses.enqueue(process);
            }
            currentProcess = currentProcess.getNext();
        }

        System.out.println("Datos de usuarios y procesos cargados correctamente.");
    }

    @Override
    public void prepareProcesses() {
        if (newProcesses == null || newProcesses.isEmpty()) {
            System.out.println("No hay procesos nuevos para preparar.");
            return;
        }


        while (!newProcesses.isEmpty()) {
            try {
                Process p = newProcesses.dequeue();
                p.calculatePriority();
                p.setState(ProcessState.PENDING);
                pendingProcesses.insert(p);
                logger.logNewPending(p);

            } catch (Exception e){
                System.out.println("Error al procesar el proceso: " + e.getMessage());
            }
        }
    }

    @Override
    public void executeNextProcess() {
        if (currentProcess != null) {
            System.out.println("Error: Ya hay un proceso en ejecucion. PID=" + currentProcess.getPid());
            return;
        }

        if (pendingProcesses == null || pendingProcesses.isEmpty()) {
            System.out.println("Error: No hay procesos pendientes para ejecutar.");
            return;
        }

        try {
            currentProcess = pendingProcesses.remove();
            currentProcess.setState(ProcessState.RUNNING);
            logger.logExecuting(currentProcess);

        } catch (EmptyHeapException e) {
            System.out.println("Error: No se pudo obtener el siguiente proceso.");
        }
    }

    @Override
    public void finishProcessOk() {
        if (currentProcess == null){
            System.out.println("Error: no hay proceso en ejecuccion.");
            return;
        }

        currentProcess.setState(ProcessState.FINISHED);
        currentProcess.setFinishType(FinishType.OK);

        System.out.println("Proceso finalizado correctamente PID=" + currentProcess.getPid()
                + " | " + currentProcess.getName());

        logger.logFinishedOk(currentProcess);

        pushToFinished(currentProcess);
        currentProcess = null;
    }
    @Override
    public void finishProcessError() {
        if (currentProcess == null){
            System.out.println("Error: no hay proceso en ejecuccion.");
            return;
        }

        currentProcess.setState(ProcessState.FINISHED);
        currentProcess.setFinishType(FinishType.ERROR);
        System.out.println("Proceso finalizado con ERROR PID=" + currentProcess.getPid()
                + " | " + currentProcess.getName());

        logger.logFinishedError(currentProcess);

        pushToFinished(currentProcess);
        currentProcess = null;
    }

    @Override
    public void terminateProcess(int uid) {
        if (currentProcess == null){
            System.out.println("Error: no hay proceso en ejecuccion.");
            return;
        }

        User requestingUser = usersByUid.get(uid);
        if (requestingUser == null) {
            System.out.println("Error: No existe usuario con UID=" + uid);
            return;
        }

        currentProcess.setState(ProcessState.FINISHED);
        currentProcess.setFinishType(FinishType.TERMINATED);
        currentProcess.setTerminatedBy(requestingUser);

        System.out.println("Proceso terminado forzosamente PID=" + currentProcess.getPid()
                + " | " + currentProcess.getName()
                + " | por USER:" + requestingUser.getAlias() + " UID=" + uid);

        logger.logTerminated(currentProcess, requestingUser);

        pushToFinished(currentProcess);
        currentProcess = null;
    }

    private void pushToFinished(Process process){
        if (finishedProcesses == null){
            finishedProcesses = new MyStackImpl<>();
        }

        if (finishedProcesses.size() >= MAX_FINISHED_PROCESS_ON_RAM) {
            System.out.println("Vaciando la pila de finalizados (stack overflow):");
            while (!finishedProcesses.isEmpty()) {
                try {
                    Process finished = finishedProcesses.pop();
                    logger.logStackOverflow(finished);
                    processesByPid.remove(finished.getPid());
                    System.out.println("  PID archivado=" + finished.getPid()
                            + " | " + finished.getName()
                            + " | STATE=" + (finished.getFinishType() != null
                            ? finished.getFinishType().name()
                            : finished.getState().name()));
                } catch (EmptyStackException e) {
                    break;
                }
            }
        }
        finishedProcesses.push(process);
    }

    @Override
    public void printStatus() {
        System.out.println("PROCESS STATUS");
        System.out.printf("EXECUTING:");
        if (currentProcess != null) {
            System.out.println("\t" + printOneProcess(currentProcess));
        }else {
            System.out.println("\t(sin proceso en ejecucion)");

        }
        System.out.println("PENDING");
        MyList<Process> aux = new MyLinkedListImpl<>();
        while(!pendingProcesses.isEmpty()) {
            try {
                Process p = pendingProcesses.remove();
                aux.add(p);
                System.out.println("\t" + printOneProcess(p));
            } catch (EmptyHeapException exc) {
                break;
            }
        }

        Node<Process> actual = aux.getFirst();
        while (actual != null){
            pendingProcesses.insert(actual.getValue());
            actual = actual.getNext();
        }


        System.out.println("FINISHED:");
        if (finishedProcesses == null || finishedProcesses.isEmpty()) {
            System.out.println("\t(sin procesos finalizados)");
        } else {
            MyList<Process> auxTerminados = new MyLinkedListImpl<>();
            while (!finishedProcesses.isEmpty()) {
                try {
                    Process p = finishedProcesses.pop();
                    auxTerminados.add(p);
                    System.out.println("\t" + printOneFinishedProcess(p));
                } catch (EmptyStackException exc2) {
                    break;
                }
            }
            for (int i = auxTerminados.size() - 1; i >= 0; i--) {
                finishedProcesses.push(auxTerminados.get(i));
            }
        }
    }
    private String printOneProcess(Process p) {
        return "PID=" + p.getPid() + " | " + p.getName() + " | USER:"
                + p.getUser().getAlias() + " UID:" + p.getUser().getUid()
                + " | P=" + p.getPriority();
    }
    private String printOneFinishedProcess(Process p) {
        String stateStr;
        if (p.getFinishType() != null) {
            stateStr = p.getFinishType().name();
            if (p.getFinishType() == FinishType.TERMINATED && p.getTerminatedBy() != null) {
                stateStr += " by USER:" + p.getTerminatedBy().getAlias()
                        + " UID:" + p.getTerminatedBy().getUid();
            }
        } else {
            stateStr = p.getState().name();
        }
        return "PID=" + p.getPid() + " | " + p.getName()
                + " | STATE: " + stateStr
                + " | USER:" + p.getUser().getAlias() + " UID:" + p.getUser().getUid();
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("=== PROCESS STATUS (VERBOSE) ===");
        System.out.println("EXECUTING:");
        if (currentProcess != null) {
            System.out.println("\t" + printOneProcess(currentProcess));
            printEvents(currentProcess);
        } else {
            System.out.println("\t(sin proceso en ejecucion)");
        }

        System.out.println("PENDING:");
        if (pendingProcesses == null || pendingProcesses.isEmpty()) {
            System.out.println("\t(sin procesos pendientes)");
        } else {
            MyList<Process> aux = new MyLinkedListImpl<>();
            while (!pendingProcesses.isEmpty()) {
                try {
                    Process p = pendingProcesses.remove();
                    aux.add(p);
                    System.out.println("\t" + printOneProcess(p));
                    printEvents(p);
                } catch (EmptyHeapException exc) {
                    break;
                }
            }
            Node<Process> actual = aux.getFirst();
            while (actual != null) {
                pendingProcesses.insert(actual.getValue());
                actual = actual.getNext();
            }
        }

        System.out.println("FINISHED:");
        if (finishedProcesses == null || finishedProcesses.isEmpty()) {
            System.out.println("\t(sin procesos finalizados)");
        } else {
            MyList<Process> auxTerminados = new MyLinkedListImpl<>();
            while (!finishedProcesses.isEmpty()) {
                try {
                    Process p = finishedProcesses.pop();
                    auxTerminados.add(p);
                    System.out.println("\t" + printOneFinishedProcess(p));
                    printEvents(p);
                } catch (EmptyStackException exc2) {
                    break;
                }
            }
            for (int i = auxTerminados.size() - 1; i >= 0; i--) {
                finishedProcesses.push(auxTerminados.get(i));
            }
        }
    }
    private void printEvents(Process p) {
        Node<Event> actual = p.getEvents().getFirst();
        while (actual != null){
            Event evento = actual.getValue();
            String instrucciones = "";
            Node<String> instrNodo = evento.getInstructions().getFirst();
            while (instrNodo !=null){
                instrucciones += instrNodo.getValue();
                if(instrNodo.getNext() != null){
                    instrucciones += ", ";
                }
                instrNodo = instrNodo.getNext();
            }
            System.out.println("\t EVENT:" + evento.getType() + " | Instructions [" + instrucciones + "]");
            actual = actual.getNext();
        }

    }

    @Override
    public void printStatusByUser(int uid) {
        User u = usersByUid.get(uid);
        if (u == null){
            System.out.println("No existe usuario con UID: " + uid);
            return;
        }
        System.out.println("PROCESS STATUS - USER: " + u.getAlias() + " UID:" + uid);
        System.out.println("EXECUTING:");
        if (currentProcess != null && currentProcess.getUser().getUid() == uid){
            System.out.println("\t" + printOneProcess(currentProcess));
        }
        System.out.println("PENDING:");
        if (pendingProcesses == null || pendingProcesses.isEmpty()) {
            System.out.println("\t(sin procesos pendientes)");
        } else {
            boolean found = false;
            MyList<Process> aux = new MyLinkedListImpl<>();
            while (!pendingProcesses.isEmpty()) {
                try {
                    Process p = pendingProcesses.remove();
                    aux.add(p);
                    if (p.getUser().getUid() == uid) {
                        System.out.println("\t" + printOneProcess(p));
                        found = true;
                    }
                } catch (EmptyHeapException exc1) {
                    break;
                }
            }
            if (!found) System.out.println("\t(ninguno)");
            Node<Process> actual = aux.getFirst();
            while (actual != null) {
                pendingProcesses.insert(actual.getValue());
                actual = actual.getNext();
            }
        }

        System.out.println("FINISHED");
        if (finishedProcesses == null || finishedProcesses.isEmpty()) {
            System.out.println("\t(sin procesos finalizados)");
        } else {
            boolean found = false;
            MyList<Process> auxTerminados = new MyLinkedListImpl<>();
            while (!finishedProcesses.isEmpty()) {
                try {
                    Process p = finishedProcesses.pop();
                    auxTerminados.add(p);
                    if (p.getUser().getUid() == uid) {
                        System.out.println("\t" + printOneFinishedProcess(p));
                        found = true;
                    }
                } catch (EmptyStackException exc2) {
                    break;
                }
            }
            if (!found) System.out.println("\t(ninguno)");
            for (int i = auxTerminados.size() - 1; i >= 0; i--) {
                finishedProcesses.push(auxTerminados.get(i));
            }
        }
    }

    @Override
    public void printStatusByProcess(int pid) {
        Process p = processesByPid.get(pid);
        if (p== null){
            System.out.println("No existe el proceso con PID: " + pid);
            return;
        }
        System.out.println(printOneProcess(p));
        printEvents(p);
    }
}
