package command;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlRootElement(name = "create")
@XmlAccessorType(XmlAccessType.FIELD)
public class CreateCommand {
    @XmlElements({
            @XmlElement(name = "account", type = CreateAccountCommand.class),
            @XmlElement(name = "symbol", type = SymbolCommand.class)
    })
    private List<Object> commands;

    public List<Object> getCommands() {
        return commands;
    }
}
