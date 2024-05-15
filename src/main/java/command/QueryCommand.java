package command;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class QueryCommand {
    @XmlAttribute(name = "id")
    private String id;

    public String getId() {
        return id;
    }
}
