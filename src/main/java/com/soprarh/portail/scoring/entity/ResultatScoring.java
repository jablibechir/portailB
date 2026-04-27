package com.soprarh.portail.scoring.entity;
import com.soprarh.portail.application.entity.Candidature;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Entity
@Table(name = "resultats_scoring", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ResultatScoring {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id")
    private Candidature candidature;
    @Column(name = "score_total", nullable = false)
    private Double scoreTotal;
    @Column(name = "date_calcul", updatable = false)
    @Builder.Default
    private LocalDateTime dateCalcul = LocalDateTime.now();
    @OneToMany(mappedBy = "resultat", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResultatCritere> resultatCriteres = new ArrayList<>();
}
