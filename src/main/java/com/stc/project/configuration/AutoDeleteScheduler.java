package com.stc.project.configuration;


import com.stc.project.core.CrudService;
import com.stc.project.model.Apartment;
import com.stc.project.model.Building;
import com.stc.project.model.Project;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

// một scheduler (bộ lập lịch) trong Spring Boot có chức năng tự động dọn "thùng rác" theo lịch định kỳ.
@EnableScheduling //Bật tính năng lập lịch (scheduling) trong Spring Boot
@Component
@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoDeleteScheduler {

    CrudService<Building, Long> buildingService;
    CrudService<Project, Long> projectService;
    CrudService<Apartment, Long> apartmentService;

    //Mỗi phút một lần, hàm runAutoCleanup() sẽ Chạy ngầm tự động trong ứng dụng,
    //Hàm này (Xóa vĩnh viễn các bản ghi quá hạn trong thùng rác có status = 0 và deletedAt < LocalDateTime.now().minusDays(30).)
    @Scheduled(cron = "0 * * * * ?") //được lập lịch chạy mỗi phút một lần.
    public void runAutoCleanup() {
        buildingService.deleteExpiredTrash();
        projectService.deleteExpiredTrash();
        apartmentService.deleteExpiredTrash();
        System.out.println("✅ Auto cleanup completed at " + java.time.LocalDateTime.now());
    }
}

