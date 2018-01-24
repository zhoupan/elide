package example;

import com.yahoo.elide.annotation.Include;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@Audited
@Include(rootLevel = true)
public class Child {

    @Setter
    private long id;

    @Setter
    @Getter
    private String name;

    @Id
    public long getId() {
        return id;
    }

    private Person parent;
    @ManyToOne(fetch = FetchType.EAGER)
    public Person getParent() {
        return  parent;
    }
    public void setParent(Person parent) {
        this.parent = parent;
    }
}
