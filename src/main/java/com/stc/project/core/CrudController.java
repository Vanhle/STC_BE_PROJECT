package com.stc.project.core;

import com.stc.project.model.IdEntity;
import com.stc.project.utils.PaginationUtil;
import com.stc.project.utils.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class CrudController<T extends IdEntity, ID extends Serializable> {
    private static Logger logger = LoggerFactory.getLogger(CrudController.class);

    // Đường dẫn gốc của API (ví dụ /api/products) → để sinh các link phân trang full URL
    protected String baseUrl;

    protected CrudService<T, ID> service;

    public CrudController(CrudService<T, ID> service) {
        this.service = service;
    }


    //findByID
    @GetMapping(value = "{id}")
    public T get(@PathVariable(value = "id") ID id) {
        return service.get(id);
    }

    //findAll
    @GetMapping()
    public List<T> listAll() {
        return service.findAll();
    }

    //findAll có hỗ trợ phân trang
//    @GetMapping()
//    public ResponseEntity<List<T>> list(Pageable pageable) {
//        Page<T> page = service.findAll(pageable);
//        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, baseUrl);
//        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
//    }


    @GetMapping(path = "/search")
//    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")
    public ResponseEntity<Page<T>> get( @RequestParam(value = "query", required = false) String query, Pageable pageable) {
        try {
            Page<T> page = service.search(query, pageable);
            HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, baseUrl);
            return new ResponseEntity<>(page, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error during search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping
    public T create(@RequestBody T entity) {
        // Lấy username người đang đăng nhập để ghi log
        logger.info("Call Create API by {}", SecurityUtil.getCurrentUserLogin());
        return service.create(entity);
    }


    @PutMapping("/{id}")
    public T update(@PathVariable(value = "id") ID id, @RequestBody T entity) {
        logger.info("Call Update API by {}", SecurityUtil.getCurrentUserLogin());
        entity.setId((Long) id);
        return service.update(id, entity);
    }


    // Vô hiệu hóa 1 đối tượng
    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<String> deactivate(@PathVariable("id") ID id) {
        logger.info("Deactivate API called by {}", SecurityUtil.getCurrentUserLogin());
        service.deactivate(id);
        return ResponseEntity.ok("Deactivated successfully!");
    }


    // Xóa mềm 1 đối tượng (chuyển vào thùng rác)
    @DeleteMapping("/moveToTrash/{id}")
    public ResponseEntity<String> moveToTrash(@PathVariable("id") ID id) {
        logger.info("Soft delete API called by {}", SecurityUtil.getCurrentUserLogin());
        service.moveToTrash(id);
        return ResponseEntity.ok("Moved to trash successfully!");
    }


    // Xóa mềm 1 danh sách ĐANG BỊ VÔ HIỆU HÓA
    @DeleteMapping("/moveDeactivateToTrashAll")
    public ResponseEntity<String> moveDeactivateToTrashAll() {
        logger.info("Move To Trash All API called by {}", SecurityUtil.getCurrentUserLogin());
        service.moveDeactivateToTrashAll();
        return ResponseEntity.ok("Moved all deactivated records to trash successfully!");
    }


    // khôi phục lại từng trường hợp bị vô hiệu hóa + xóa mềm
    @PutMapping("/restore/{id}")
    public ResponseEntity<String> restore(@PathVariable("id") ID id) {
        logger.info("Restore API called by {}", SecurityUtil.getCurrentUserLogin());
        service.restore(id);
        return ResponseEntity.ok("Restored successfully!");
    }


    // khôi phục lại LIST trường hợp bị vô hiệu hóa
    @PutMapping("/restoreAllDeactivated")
    public ResponseEntity<String> restoreAllDeactivated() {
        logger.info("Restore All Deactivated API called by {}", SecurityUtil.getCurrentUserLogin());
        service.restoreAllDeactivated();
        return ResponseEntity.ok("Restored all deactivated records successfully!");
    }

    // khôi phục lại LIST trường hợp bị xóa mềm
    @PutMapping("/restoreAllFromTrash")
    public ResponseEntity<String> restoreAllFromTrash() {
        logger.info("Restore All From Trash API called by {}", SecurityUtil.getCurrentUserLogin());
        service.restoreAllFromTrash();
        return ResponseEntity.ok("Restored all records from trash successfully!");
    }


    // Xóa vĩnh viễn 1 bản ghi trong thùng rác
    @DeleteMapping("/trash/permanentDelete/{id}")
    public ResponseEntity<String> permanentDelete(@PathVariable(value = "id") ID id) {
        logger.info("Permanent delete API called by {}", SecurityUtil.getCurrentUserLogin());
        service.deleteById(id);
        return ResponseEntity.ok("Permanently deleted successfully!");
    }


    // Xóa vĩnh viễn toàn bộ bản ghi trong thùng rác
    @DeleteMapping("/trash/clear")
    public ResponseEntity<String> clearTrash() {
        logger.info("Clear trash API called by {}", SecurityUtil.getCurrentUserLogin());
        service.clearTrash();
        return ResponseEntity.ok("Delete all records in trash successfully!");
    }


    //  Tự động xóa theo thời gian quá hạn (quá 30 ngày trong thùng rác)
    @DeleteMapping("/trash/expired")
    public ResponseEntity<String> deleteExpiredTrash() {
        service.deleteExpiredTrash();
        return ResponseEntity.ok("Deleted expired trash (older than 30 days)!");
    }


}














