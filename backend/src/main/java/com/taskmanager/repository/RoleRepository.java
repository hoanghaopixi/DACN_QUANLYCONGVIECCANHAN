package com.taskmanager.repository;

import com.taskmanager.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<VaiTro, Integer> {
    Optional<VaiTro> findByName(String name);
}
