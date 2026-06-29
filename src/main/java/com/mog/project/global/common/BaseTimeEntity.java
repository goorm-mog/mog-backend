package com.mog.project.global.common;                         
                                                               
import jakarta.persistence.Column;          
import jakarta.persistence.EntityListeners;                    
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;                                
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;   
import org.springframework.data.jpa.domain.support.AuditingEntityListener;     

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    // 최초 insert 될 때 자동으로 현재 시간 세팅
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // update 될 때마다 자동으로 현재 시간 갱신
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
