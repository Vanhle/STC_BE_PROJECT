package com.stc.project.core;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

// Đánh dấu một interface repository cơ sở (base interface) không được Spring tạo implementation tự động (sinh ra code thực thi cho các repository)
@NoRepositoryBean
public interface CustomJpaRepository<T, ID extends Serializable> extends JpaRepository<T,ID>, JpaSpecificationExecutor<T> {
}
