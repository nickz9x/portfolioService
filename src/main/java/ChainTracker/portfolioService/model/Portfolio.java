package ChainTracker.portfolioService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String owner;
    @OneToMany(mappedBy = "portfolio",cascade = CascadeType.ALL)
    private List<Position> positions = new ArrayList<>();
}
