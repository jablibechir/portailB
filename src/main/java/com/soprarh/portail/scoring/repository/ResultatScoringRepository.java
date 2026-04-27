package com.soprarh.portail.scoring.repository;

import com.soprarh.portail.scoring.entity.ResultatScoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultatScoringRepository extends JpaRepository<ResultatScoring, UUID> {

    List<ResultatScoring> findByCandidatureId(UUID candidatureId);

    Optional<ResultatScoring> findTopByCandidatureIdOrderByDateCalculDesc(UUID candidatureId);
}

