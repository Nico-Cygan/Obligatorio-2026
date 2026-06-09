package uy.edu.um.doors.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uy.edu.um.tad.list.MyList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Process {
    @EqualsAndHashCode.Include
    private int pid;
    private String name;
    private User user;
    private int priority;
    private ProcessState state;
    private MyList<Event> events;
}
