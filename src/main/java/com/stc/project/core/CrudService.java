package com.stc.project.core;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import com.stc.project.constants.Constants;
import com.stc.project.model.AbstractEntity;
import com.stc.project.rsql.CustomRsqlVisitor;
import com.stc.project.utils.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Transactional
public class CrudService<T extends AbstractEntity, ID extends Serializable> {

    private static Logger logger = LoggerFactory.getLogger(CrudService.class);

    protected CustomJpaRepository<T, ID> repository;

    public T get(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity with ID = " + id + " does not exist."));

    }

    public List<T> findAll() {
        return repository.findAll();
    }


    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }


    public List<T> search(String query) {
        // nếu ko truyền gì cả thì là findAll
        if (StringUtils.isEmpty(query)) {
            return repository.findAll();
        }

        //nếu có thì là search bằng rsql
        Node rootNode = new RSQLParser().parse(query);
        Specification<T> spec = rootNode.accept(new CustomRsqlVisitor<T>());
        return repository.findAll(spec);
    }


    // NEED  TO CHECK
    // Nếu người dùng có role ROLE_MANAGER, thì TỰ ĐỘNG FILL điều kiện createdBy==<username> vào query.
    // Nếu là ROLE_ADMIN, thì giữ nguyên query được truyền
    public Page<T> search(String query, Pageable pageable) {
        try {
            String username = SecurityUtil.getCurrentUserLogin();

            logger.info("=== SEARCH DEBUG START ===");
            logger.info("Username: {}", username);
            logger.info("Is Admin: {}", SecurityUtil.isAdmin());
            logger.info("Is Manager: {}", SecurityUtil.isManager());
            logger.info("Original Query: {}", query);

            // Logic rõ ràng theo role
            if (SecurityUtil.isAdmin()) {
                // ADMIN: Xem tất cả records, giữ nguyên query gốc
                logger.info("ADMIN - Processing original query: {}", query);

                if (!StringUtils.hasText(query)) {
                    // Không có filter gì, trả về tất cả
                    logger.info("ADMIN - No query, returning all records");
                    Page<T> result = repository.findAll(pageable);
                    logger.info("ADMIN - Result count: {}", result.getTotalElements());
                    logger.info("=== SEARCH DEBUG END ===");
                    return result;
                } else {
                    // Có query, thực hiện RSQL với query gốc
                    logger.info("ADMIN - Executing RSQL with original query: {}", query);
                    Node rootNode = new RSQLParser().parse(query);
                    Specification<T> spec = rootNode.accept(new CustomRsqlVisitor<T>());
                    Page<T> result = repository.findAll(spec, pageable);
                    logger.info("ADMIN - RSQL Result count: {}", result.getTotalElements());
                    logger.info("=== SEARCH DEBUG END ===");
                    return result;
                }

            } else if (SecurityUtil.isManager()) {
                // MANAGER: Chỉ xem records do chính họ tạo
                String managerQuery;
                if (!StringUtils.hasText(query)) {
                    managerQuery = "createdBy==" + username;
                } else {
                    managerQuery = "(" + query + ");createdBy==" + username;
                }

                logger.info("MANAGER - Modified query: {}", managerQuery);
                Node rootNode = new RSQLParser().parse(managerQuery);
                Specification<T> spec = rootNode.accept(new CustomRsqlVisitor<T>());
                Page<T> result = repository.findAll(spec, pageable);
                logger.info("MANAGER - Result count: {}", result.getTotalElements());
                logger.info("=== SEARCH DEBUG END ===");
                logger.info(spec.toString());
                return result;

            } else {
                // Không có role phù hợp, trả về empty
                logger.warn("User has no valid role (not ADMIN or MANAGER)");
                logger.info("=== SEARCH DEBUG END ===");
                return emptyPage();
            }

        } catch (RSQLParserException pe) {
            logger.error("{} SEARCH RSQLParserException FAIL: {}", this.getClass().getSimpleName(), query);
            return emptyPage();
        } catch (Exception e) {
            logger.error("{} SEARCH Exception FAIL: {}", this.getClass().getSimpleName(), query, e);
            return emptyPage();
        }
    }


    public T create(T entity) {
        beforeCreate(entity);
        repository.save(entity);
        return entity;
    }


    public T update(ID id, T entity) {
        beforeUpdate(entity);
        T old = get(id);
        if (entity.getCreatedBy() == null) entity.setCreatedBy(old.getCreatedBy());
        repository.save(entity);
        return entity;
    }


    // vô hiệu hóa khi còn hoạt động ( active == 1 )
    public void deactivate(ID id) {
        T t = get(id);
        if (t.getActive() == Constants.EntityStatus.ACTIVE) {
            t.setActive(Constants.EntityStatus.DEACTIVATED);
            t.setDeactivatedAt(LocalDateTime.now());
            t.setUpdatedBy(SecurityUtil.getCurrentUserLogin());
            repository.save(t);
        } else {
            throw new IllegalStateException("Cannot deactivate because the object has already been soft-deleted or deactivated."
            );
        }
    }


    // xóa mềm ( chuyển vào thùng rác tạm ) - áp dụng cho các bản ghi đang active và đang bị deactive
    public void moveToTrash(ID id) {
        T t = get(id);
        if(t.getActive() != Constants.EntityStatus.IN_ACTIVE) {
            t.setActive(Constants.EntityStatus.IN_ACTIVE);
            t.setDeletedAt(LocalDateTime.now());
            t.setDeactivatedAt(null);
            t.setUpdatedBy(SecurityUtil.getCurrentUserLogin());
            repository.save(t);
        } else {
            throw new IllegalStateException("Unable to move the object to trash.");
        }
    }


    // xóa mêm toàn bộ đối tượng ĐANG bị vô hiệu hóa
    public void moveDeactivateToTrashAll() {
        try {
            List<T> trashItems = repository.findAll().stream()
                    .filter(p -> p.getActive() == Constants.EntityStatus.DEACTIVATED)
                    .collect(Collectors.toList());

            trashItems.forEach(item -> {
                item.setActive(Constants.EntityStatus.IN_ACTIVE);
                item.setDeletedAt(LocalDateTime.now());
                item.setDeactivatedAt(null);
                item.setUpdatedBy(SecurityUtil.getCurrentUserLogin());
            });

            repository.saveAll(trashItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to soft-delete all deactivated objects: " + e.getMessage(), e);
        }
    }


    // khôi phục lại từng trường hợp bị vô hiệu hoá + xóa mềm
    public void restore(ID id) {
        T t = get(id);
        beforeRestore(t);
        if(t.getActive() != Constants.EntityStatus.ACTIVE) {
            t.setActive(Constants.EntityStatus.ACTIVE);
            t.setDeactivatedAt(null);
            t.setDeletedAt(null);
            t.setUpdatedBy(SecurityUtil.getCurrentUserLogin());
            repository.save(t);
        }
        else {
            throw new IllegalStateException("Unable to restore active object");
        }
    }


    // khôi phục lại toàn bộ trường hợp bị vô hiệu hóa
    public void restoreAllDeactivated() {
        try {
            List<T> deactivatedItems = repository.findAll().stream()
                    .filter(e -> e.getActive() == Constants.EntityStatus.DEACTIVATED)
                    .collect(Collectors.toList());

            for (T item : deactivatedItems) {
                //kiểm tra điều kiện trước khi khôi phục
                beforeRestore(item);
                item.setActive(Constants.EntityStatus.ACTIVE);
                item.setDeactivatedAt(null);
                item.setUpdatedBy(SecurityUtil.getCurrentUserLogin());
            }

            repository.saveAll(deactivatedItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore all deactivated objects: "	 + e.getMessage(), e);
        }
    }

    // khôi phục lại toàn bộ trường hợp bị xóa mềm
    public void restoreAllFromTrash() {
        try {
            List<T> trashItems = repository.findAll().stream()
                    .filter(e -> e.getActive() == Constants.EntityStatus.IN_ACTIVE)
                    .collect(Collectors.toList());

            for (T item : trashItems) {
                //kiểm tra điều kiện trước khi khôi phục
                beforeRestore(item);
                item.setActive(Constants.EntityStatus.ACTIVE);
                item.setDeletedAt(null);
                item.setUpdatedBy(SecurityUtil.getCurrentUserLogin());
            }

            repository.saveAll(trashItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore all soft-deleted objects:"	 + e.getMessage(), e);
        }
    }


    // xóa vĩnh viễn 1 đối tượng bằng id trong thùng rác
    public void deleteById(ID id) {
        T entity = get(id);
        delete(entity);
    }


    // xóa vĩnh viễn toàn bộ trong thùng rác
    public void clearTrash() {
        try {
            List<T> trashItems = repository.findAll().stream()
                    .filter(e -> e.getActive() == Constants.EntityStatus.IN_ACTIVE)
                    .collect(Collectors.toList());
            repository.deleteAll(trashItems);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete all objects in the trash: "	 + e.getMessage(), e);
        }
    }


    //  tự động xóa theo thời gian quá hạn (quá 30 ngày trong thùng rác)
    public void deleteExpiredTrash() {
        try {
            //tính Mốc thời gian 30 ngày trước kể từ hiện tại
            //ví dụ hnay là 17/5 thì expiredTime = 17/4
            LocalDateTime expiredTime = LocalDateTime.now().minusDays(30);
            List<T> expired = repository.findAll().stream()
                    .filter(e -> e.getActive() == Constants.EntityStatus.IN_ACTIVE && e.getDeletedAt().isBefore(expiredTime))
                    .collect(Collectors.toList());
            repository.deleteAll(expired);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete expired items in the trash: " + e.getMessage(), e);
        }
    }


    protected void beforeCreate(T entity) {
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(SecurityUtil.getCurrentUserLogin());
        }
        if (entity.getActive() == null) {
            entity.setActive(Constants.EntityStatus.ACTIVE);
        }
    }


    protected void beforeUpdate(T entity) {
        entity.setUpdatedBy(SecurityUtil.getCurrentUserLogin());

        if (entity.getActive() == null) {
            entity.setActive(Constants.EntityStatus.ACTIVE);
        }
    }


    // Mặc định không kiểm tra gì, sẽ override ở lớp con nếu cần
    protected void beforeRestore(T entity) {
    }


    public void delete(T entity) {
        beforeDelete(entity);
        repository.delete(entity);
    }


    protected void beforeDelete(T entity) {
        if (entity.getActive() == Constants.EntityStatus.ACTIVE) {
            throw new IllegalStateException("You can only permanently delete an object that has already been soft-deleted."	);
        }
    }


    public Page<T> emptyPage() {
        return new Page<T>() {
            @Override
            public int getTotalPages() {
                return 0;
            }

            @Override
            public long getTotalElements() {
                return 0;
            }

            @Override
            public <U> Page<U> map(Function<? super T, ? extends U> converter) {
                return null;
            }

            @Override
            public int getNumber() {
                return 0;
            }

            @Override
            public int getSize() {
                return 0;
            }

            @Override
            public int getNumberOfElements() {
                return 0;
            }

            @Override
            public List<T> getContent() {
                return List.of();
            }

            @Override
            public boolean hasContent() {
                return false;
            }

            @Override
            public Sort getSort() {
                return null;
            }

            @Override
            public boolean isFirst() {
                return false;
            }

            @Override
            public boolean isLast() {
                return false;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable nextPageable() {
                return null;
            }

            @Override
            public Pageable previousPageable() {
                return null;
            }

            @Override
            public Iterator<T> iterator() {
                return null;
            }
        };
    }
}
