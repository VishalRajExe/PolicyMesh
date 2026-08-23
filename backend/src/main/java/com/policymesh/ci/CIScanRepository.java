package com.policymesh.ci;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CIScanRepository extends JpaRepository<CIScan, Long> {
  List<CIScan> findTop20ByOrderByStartedAtDesc();
  Page<CIScan> findAllByOrderByStartedAtDesc(Pageable pageable);
}
