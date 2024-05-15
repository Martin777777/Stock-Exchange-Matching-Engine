package command;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "transactions")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransactionsCommand {
    @XmlAttribute(name = "id")
    private String id;

    @XmlElements({
            @XmlElement(name = "order", type = OrderCommand.class),
            @XmlElement(name = "cancel", type = CancelCommand.class),
            @XmlElement(name = "query", type = QueryCommand.class)
    })
    private List<Object> commands;

    public String getAccountId() {
        return id;
    }

    public List<Object> getCommands() {
        return commands;
    }

    // Getters and Setters
}