package command;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class CreateAccountCommand {
    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "balance")
    private double balance;

    public String getId() {
        return id;
    }

    public double getBalance() {
        return balance;
    }
}
