package command;

import javax.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class SymbolCommand {
    @XmlAttribute(name = "sym")
    private String sym;

    @XmlElement(name = "account")
    private List<SymbolAccountCommand> accounts;

    // Getter method for the 'sym' field
    public String getSym() {
        return sym;
    }

    // Getter method for the 'accounts' list
    public List<SymbolAccountCommand> getAccounts() {
        return accounts;
    }
}
