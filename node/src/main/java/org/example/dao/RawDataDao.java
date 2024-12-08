package org.example.dao;

import org.example.entity.RawData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RawDataDao extends JpaRepository<RawData, Long> {
}
