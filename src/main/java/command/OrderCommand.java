package command;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class OrderCommand {
    @XmlAttribute(name = "sym")
    private String sym;

    @XmlAttribute(name = "amount")
    private int amount;

    @XmlAttribute(name = "limit")
    private double limit;

    public String getSym() {
        return sym;
    }

    public int getAmount() {
        return amount;
    }

    public double getLimit() {
        return limit;
    }
}
