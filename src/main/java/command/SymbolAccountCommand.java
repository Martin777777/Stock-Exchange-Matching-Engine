package command;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolAccountCommand {
    @XmlAttribute
    private String id;

    @XmlValue
    private int shares;

    // Getter for id
    public String getId() {
        return id;
    }

    // Setter for id
    public void setId(String id) {
        this.id = id;
    }

    // Getter for shares
    public int getShares() {
        return shares;
    }

    // Setter for shares
    public void setShares(int shares) {
        this.shares = shares;
    }
}
