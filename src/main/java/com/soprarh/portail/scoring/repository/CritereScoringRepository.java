package com.soprarh.portail.scoring.repository;

import com.soprarh.portail.scoring.entity.CritereScoring;
import com.soprarh.portail.scoring.entity.TypeCritere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CritereScoringRepository extends JpaRepository<CritereScoring, UUID> {

    List<CritereScoring> findByOffreId(UUID offreId);

    long countByOffreId(UUID offreId);

    Optional<CritereScoring> findByOffreIdAndType(UUID offreId, TypeCritere type);

    void deleteAllByOffreId(UUID offreId);
}

