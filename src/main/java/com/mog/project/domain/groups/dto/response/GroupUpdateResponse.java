package com.mog.project.domain.groups.dto.response;
                                              
import java.time.LocalDateTime;         

public record GroupUpdateResponse(                          
      Long groupId,
      String groupName,                                       
      LocalDateTime updatedAt
) {}