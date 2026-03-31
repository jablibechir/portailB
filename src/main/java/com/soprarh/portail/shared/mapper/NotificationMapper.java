package com.soprarh.portail.shared.mapper;

import com.soprarh.portail.shared.dto.NotificationResponse;
import com.soprarh.portail.shared.entity.Notification;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper pour convertir Notification <-> NotificationResponse.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);
}

