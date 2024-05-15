package entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Account {
    @Id
    private Long id;

    @Column(nullable = false)
    private Double balance;

//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public Double getBalance() {
//        return balance;
//    }
//
//    public void setBalance(Double balance) {
//        this.balance = balance;
//    }
//
//    public Set<Position> getPositions() {
//        return positions;
//    }
//
//    public void setPositions(Set<Position> positions) {
//        this.positions = positions;
//    }

    @ManyToMany
    @JoinTable(
            name = "account_position",
            joinColumns = @JoinColumn(name = "account_id"),
            inverseJoinColumns = @JoinColumn(name = "position_id"))
    private Set<Position> positions = new HashSet<>();

    // Getter for id
    public Long getId() {
        return id;
    }

    // Setter for id
    public void setId(Long id) {
        this.id = id;
    }

    // Getter for balance
    public Double getBalance() {
        return balance;
    }

    // Setter for balance
    public void setBalance(Double balance) {
        this.balance = balance;
    }

    // Getter for positions
    public Set<Position> getPositions() {
        return positions;
    }

    // Setter for positions
    public void setPositions(Set<Position> positions) {
        this.positions = positions;
    }
//
//    // Helper method to add a position to the Account
//    public void addPosition(Position position) {
//        this.positions.add(position);
//        position.getAccounts().add(this); // Assuming Position class has a getAccounts method
//    }
//
//    // Helper method to remove a position from the Account
//    public void removePosition(Position position) {
//        this.positions.remove(position);
//        position.getAccounts().remove(this); // Assuming Position class has a getAccounts method
//    }
}
