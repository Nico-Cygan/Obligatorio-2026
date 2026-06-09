package uy.edu.um.doors.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uy.edu.um.tad.list.MyList;
import uy.edu.um.tad.list.Node;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Process implements Comparable<Process> {
    @EqualsAndHashCode.Include
    private int pid;
    private String name;
    private User user;
    private int priority;
    private ProcessState state;
    private MyList<Event> events;

    public int calculatePriority() {
        if (user == null || events == null || events.isEmpty()) {
            this.priority = 0;
            return this.priority;
        }

        int cpuEvents = 0;
        int ramEvents = 0;
        int diskEvents = 0;
        int totalEvents = 0;

        Node<Event> current = events.getFirst();
        while (current != null) {
            Event event = current.getValue();
            if (event != null && event.getType() != null) {
                switch (event.getType()) {
                    case CPU -> cpuEvents++;
                    case RAM -> ramEvents++;
                    case DISK -> diskEvents++;
                }
            }
            totalEvents++;
            current = current.getNext();
        }

        if (totalEvents == 0) {
            this.priority = 0;
            return this.priority;
        }

        int weightedEvents = 8 * cpuEvents + 2 * ramEvents + 2 * diskEvents;
        int userWeight = user.getType() != null ? user.getType().getWeight() : 0;
        this.priority = (weightedEvents / totalEvents) + (userWeight * totalEvents);
        return this.priority;
    }

    @Override
    public int compareTo(Process other) {
        if (other == null) {
            return 1;
        }
        return Integer.compare(this.priority, other.priority);
    }
}
