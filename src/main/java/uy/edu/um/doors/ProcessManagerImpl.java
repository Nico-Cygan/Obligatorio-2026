package uy.edu.um.doors;

import uy.edu.um.doors.model.Event;
import uy.edu.um.doors.model.Process;
import uy.edu.um.doors.model.User;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.heap.EmptyHeapException;
import uy.edu.um.tad.heap.MyHeap;
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
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void prepareProcesses() {
        System.out.println("IMPLEMENTAR");
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
        System.out.println("IMPLEMENTAR");
    }
}
