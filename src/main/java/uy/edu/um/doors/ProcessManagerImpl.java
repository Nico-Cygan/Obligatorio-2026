package uy.edu.um.doors;

import uy.edu.um.doors.model.Process;
import uy.edu.um.doors.model.User;
import uy.edu.um.tad.hash.MyHash;
import uy.edu.um.tad.hash.MyHashImpl;
import uy.edu.um.tad.queue.MyQueue;
import uy.edu.um.tad.queue.MyQueueImpl;

public class ProcessManagerImpl implements ProcessManager {

    private MyHash<Integer, User> usersByUid;
    private MyHash<Integer, Process> processesByPid;
    private MyQueue<Process> newProcesses;

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
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusVerbose() {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByUser(int uid) {
        System.out.println("IMPLEMENTAR");
    }

    @Override
    public void printStatusByProcess(int pid) {
        System.out.println("IMPLEMENTAR");
    }
}
