package com.tfg.dashboard.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tfg.dashboard.model.ArubaSwitchInterfaceUsageHistory;

public interface ArubaSwitchInterfaceUsageHistoryRepository extends JpaRepository<ArubaSwitchInterfaceUsageHistory, Long> {

    Optional<ArubaSwitchInterfaceUsageHistory> findTopByObservedAtIsNotNullOrderByObservedAtDesc();

    @Query("""
            select h.associatedDevice
            from ArubaSwitchInterfaceUsageHistory h
            where h.observedAt >= :since
              and h.associatedDevice in (
                  select older.associatedDevice
                  from ArubaSwitchInterfaceUsageHistory older
                  group by older.associatedDevice
                  having min(older.observedAt) <= :since
              )
            group by h.associatedDevice
            having min(h.downInterfaces) > :downInterfaceLimit
               and sum(
                   case
                       when lower(h.deviceStatus) = lower(:deviceStatus)
                       then 0
                       else 1
                   end
               ) = 0
            """)
    List<String> findDevicesAlwaysOverDownInterfaceLimitSince(
            @Param("deviceStatus") String deviceStatus,
            @Param("downInterfaceLimit") int downInterfaceLimit,
            @Param("since") LocalDateTime since
    );
}
