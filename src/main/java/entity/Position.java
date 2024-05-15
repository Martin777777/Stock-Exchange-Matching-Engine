package entity;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Position {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sym;

    private int amount;

    @ManyToMany(mappedBy = "positions")
    private Set<Account> accounts = new HashSet<>();

    // Getter and setter for id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Getter and setter for sym
    public String getSym() {
        return sym;
    }

    public void setSym(String sym) {
        this.sym = sym;
    }

    // Getter and setter for amount
    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    // Getter and setter for accounts
    public Set<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(Set<Account> accounts) {
        this.accounts = accounts;
    }

    // Helper methods for account management
    public void addAccount(Account account) {
        this.accounts.add(account);
        account.getPositions().add(this);
    }

    public void removeAccount(Account account) {
        this.accounts.remove(account);
        account.getPositions().remove(this);
    }
}
