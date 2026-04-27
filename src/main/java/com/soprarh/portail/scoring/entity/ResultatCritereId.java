package com.soprarh.portail.scoring.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ResultatCritereId implements Serializable {
    private UUID resultat;
    private UUID critere;
}
