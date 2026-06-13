package uy.edu.um.doors;

import uy.edu.um.doors.model.Event;
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

import javax.swing.*;

public class ProcessManagerImpl implements ProcessManager {

    private MyHash<Integer, User> usersByUid;
    private MyHash<Integer, Process> processesByPid;
    private MyQueue<Process> newProcesses;
    private MyHeap<Process> pendingProcesses;
    private MyStack<Process> finishedProcesses;
    private Process currentProcess;

    public ProcessManagerImpl() {
        this.usersByUid = new MyHashImpl<>();
        this.processesByPid = new MyHashImpl<>();
        this.newProcesses = new MyQueueImpl<>();
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
            System.out.println("No hay proceso nuevos para prepar.");
            return;
        }

        this.pendingProcesses = new MyHeapImpl<>(false);

        while (!newProcesses.isEmpty()) {
            try {
                Process p = newProcesses.dequeue();
                p.calculatePriority();
                p.setState(ProcessState.PENDING);
                pendingProcesses.insert(p);
                System.out.println("New Pending Proces: PID=" + p.getPid()
                        + " | " + p.getName()
                        + " | User: " + p.getUser().getAlias()
                        + " Uid: " + p.getUser().getUid()
                        + " | p= " + p.getPriority());

            } catch (Exception e){
                System.out.println("Error al procesar el proceso: " + e.getMessage());
            }
        }

    }

    @Override
    public void executeNextProcess() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessOk() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void finishProcessError() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void terminateProcess(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatus() {
        System.out.println("PROCESS STATUS");
        System.out.printf("EXECUTING:");
        if (currentProcess != null){
            System.out.println("\t" + printOneProcess(currentProcess));
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
        MyList<Process> auxTerminados = new MyLinkedListImpl<>();
        while (!finishedProcesses.isEmpty()){
            try{
                    Process p = finishedProcesses.pop();
                    auxTerminados.add(p);
                    System.out.println("\t" + printOneFinishedProcess(p));
            } catch (EmptyStackException exc2){
                    break;
            }
        }
        for (int i = auxTerminados.size() - 1; i>=0; i--){
            finishedProcesses.push(auxTerminados.get(i));
        }
    }
    private String printOneProcess(Process p){
        return "PID=" + p.getPid() + " | " + p.getName() + " | USER:"
                + p.getUser().getAlias() + " UID:" + p.getUser().getUid() + " | P=" + p.getPriority();

    }
    private String printOneFinishedProcess(Process p){
        return "PID=" + p.getPid() + " | " + p.getName() + " | STATE: " + p.getState() +
                " | USER:" + p.getUser().getAlias() + " UID:" + p.getUser().getUid();

    }

    @Override
    public void printStatusVerbose() {
        System.out.println("PROCESS STATUS");
        System.out.printf("EXECUTING:");
        if (currentProcess != null){
            System.out.println("\t" + printOneProcess(currentProcess));
            printEvents(currentProcess);
        }
        System.out.println("PENDING");
        MyList<Process> aux = new MyLinkedListImpl<>();
        while(!pendingProcesses.isEmpty()) {
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
        while (actual != null){
            pendingProcesses.insert(actual.getValue());
            actual = actual.getNext();
        }
        System.out.println("FINISHED:");
        MyList<Process> auxTerminados = new MyLinkedListImpl<>();
        while (!finishedProcesses.isEmpty()){
            try{
                Process p = finishedProcesses.pop();
                auxTerminados.add(p);
                System.out.println("\t" + printOneFinishedProcess(p));
                printEvents(p);
            } catch (EmptyStackException exc2){
                break;
            }
        }
        for (int i = auxTerminados.size() - 1; i>=0; i--){
            finishedProcesses.push(auxTerminados.get(i));
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
        MyList<Process> aux = new MyLinkedListImpl<>();
        while (!pendingProcesses.isEmpty()){
            try{
                Process p = pendingProcesses.remove();
                aux.add(p);
                if (p.getUser().getUid() == uid){
                    System.out.println("\t" + printOneProcess(p));
                }
            } catch (EmptyHeapException exc1){
                break;
            }
        }

        Node<Process> actual = aux.getFirst();
        while(actual != null){
            pendingProcesses.insert(actual.getValue());
            actual = actual.getNext();
        }

        System.out.println("FINISHED");
        MyList<Process> auxTerminados = new MyLinkedListImpl<>();
        while(!finishedProcesses.isEmpty()){
            try{
                Process p = finishedProcesses.pop();
                auxTerminados.add(p);
                if(p.getUser().getUid() == uid){
                    System.out.println("\t" + printOneFinishedProcess(p));
                }
            }catch (EmptyStackException exc2){
                break;
            }
        }
        for (int i =auxTerminados.size() - 1; i >= 0; i--){
            finishedProcesses.push(auxTerminados.get(i));
        }
    }

    @Override
    public void printStatusByProcess(int pid) {
        Process p = processesByPid.get(pid);
        if (p== null){
            System.out.println("No existe el proceso con PID: " + pid);
        }
        System.out.println(printOneProcess(p));
        printEvents(p);
    }
}
